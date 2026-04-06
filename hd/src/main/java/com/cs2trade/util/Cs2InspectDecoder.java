package com.cs2trade.util;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * Decodes the self-encoded CS2 inspect payload from inspect links.
 */
@Slf4j
@Component
public class Cs2InspectDecoder {

    private static final Pattern INSPECT_LINK_PATTERN = Pattern.compile(
            "^(?:steam://(?:run|rungame)/730/(?:76561202255233023/)?/?)?\\+?csgo_econ_action_preview\\s+([0-9A-F]+)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9A-F]+$", Pattern.CASE_INSENSITIVE);

    public Optional<DecodedInspectData> decodeSafely(String inspectUrl) {
        if (inspectUrl == null || inspectUrl.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(decodeLink(inspectUrl));
        } catch (RuntimeException e) {
            log.debug("Failed to decode inspect url: {}", inspectUrl, e);
            return Optional.empty();
        }
    }

    public DecodedInspectData decodeLink(String inspectUrl) {
        String hexPayload = extractHexFromLink(inspectUrl);
        return decodeHex(hexPayload);
    }

    public DecodedInspectData decodeHex(String inspectHex) {
        String normalizedHex = inspectHex == null ? "" : inspectHex.trim();
        assertValidHex(normalizedHex);

        byte[] buffer = HexFormat.of().parseHex(normalizedHex);
        if (buffer.length < 5) {
            throw new IllegalArgumentException("Invalid inspect hex payload");
        }

        return (buffer[0] & 0xFF) == 0 ? decodeWrappedBuffer(buffer) : decodeMaskedBuffer(buffer);
    }

    private String extractHexFromLink(String inspectUrl) {
        String decodedLink = decodePercentEncodingSafely(inspectUrl == null ? "" : inspectUrl.trim());
        Matcher matcher = INSPECT_LINK_PATTERN.matcher(decodedLink);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid inspect link");
        }

        String inspectHex = matcher.group(1);
        assertValidHex(inspectHex);
        return inspectHex;
    }

    private String decodePercentEncodingSafely(String value) {
        try {
            return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    private void assertValidHex(String inspectHex) {
        if (!HEX_PATTERN.matcher(inspectHex).matches() || inspectHex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid inspect hex payload");
        }
    }

    private DecodedInspectData decodeWrappedBuffer(byte[] buffer) {
        if (buffer.length < 5 || (buffer[0] & 0xFF) != 0) {
            throw new IllegalArgumentException("Invalid inspect hex payload");
        }

        byte[] payload = new byte[buffer.length - 5];
        System.arraycopy(buffer, 1, payload, 0, payload.length);

        long expectedChecksum = readUnsignedInt32BigEndian(buffer, buffer.length - 4);
        long actualChecksum = getChecksum(payload);
        if (expectedChecksum != actualChecksum) {
            throw new IllegalArgumentException("Inspect hex checksum mismatch");
        }

        DecodedInspectData decoded = decodePayload(payload);
        if (!decoded.hasWrappedPayloadFields()) {
            throw new IllegalArgumentException("Invalid inspect hex payload");
        }

        return decoded;
    }

    private DecodedInspectData decodeMaskedBuffer(byte[] buffer) {
        if (buffer.length < 5) {
            throw new IllegalArgumentException("Invalid inspect hex payload");
        }

        int xorKey = buffer[0] & 0xFF;
        byte[] unmasked = new byte[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            unmasked[i] = (byte) ((buffer[i] & 0xFF) ^ xorKey);
        }

        if ((unmasked[0] & 0xFF) != 0) {
            throw new IllegalArgumentException("Invalid inspect hex payload");
        }

        byte[] payload = new byte[unmasked.length - 5];
        System.arraycopy(unmasked, 1, payload, 0, payload.length);

        DecodedInspectData decoded = decodePayload(payload);
        if (!decoded.hasMaskedRequiredFields()) {
            throw new IllegalArgumentException("Invalid inspect hex payload");
        }

        return decoded;
    }

    private DecodedInspectData decodePayload(byte[] payload) {
        try {
            CodedInputStream input = CodedInputStream.newInstance(payload);

            String itemId = null;
            Integer defindex = null;
            Integer paintIndex = null;
            Integer rarity = null;
            Integer quality = null;
            BigDecimal paintWear = null;
            Integer paintSeed = null;
            Integer inventory = null;
            Integer origin = null;
            int stickerCount = 0;
            int keychainCount = 0;
            int variationCount = 0;

            while (!input.isAtEnd()) {
                int tag = input.readTag();
                if (tag == 0) {
                    break;
                }

                int fieldNumber = WireFormat.getTagFieldNumber(tag);
                switch (fieldNumber) {
                    case 2 -> itemId = Long.toUnsignedString(input.readUInt64());
                    case 3 -> defindex = input.readUInt32();
                    case 4 -> paintIndex = input.readUInt32();
                    case 5 -> rarity = input.readUInt32();
                    case 6 -> quality = input.readUInt32();
                    case 7 -> paintWear = uint32BitsToWear(input.readUInt32());
                    case 8 -> paintSeed = input.readUInt32();
                    case 12 -> {
                        stickerCount++;
                        input.skipField(tag);
                    }
                    case 13 -> inventory = input.readUInt32();
                    case 14 -> origin = input.readUInt32();
                    case 20 -> {
                        keychainCount++;
                        input.skipField(tag);
                    }
                    case 22 -> {
                        variationCount++;
                        input.skipField(tag);
                    }
                    default -> input.skipField(tag);
                }
            }

            return new DecodedInspectData(
                    itemId,
                    defindex,
                    paintIndex,
                    rarity,
                    quality,
                    paintWear,
                    paintSeed,
                    inventory,
                    origin,
                    stickerCount,
                    keychainCount,
                    variationCount
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid inspect hex payload", e);
        }
    }

    private BigDecimal uint32BitsToWear(int bits) {
        float wear = Float.intBitsToFloat(bits);
        if (!Float.isFinite(wear)) {
            return null;
        }
        return new BigDecimal(Float.toString(wear));
    }

    private long getChecksum(byte[] payload) {
        CRC32 crc32 = new CRC32();
        crc32.update(new byte[]{0}, 0, 1);
        crc32.update(payload, 0, payload.length);

        long crc = crc32.getValue();
        long mixed = (crc & 0xFFFFL) ^ ((long) payload.length * crc);
        return mixed & 0xFFFFFFFFL;
    }

    private long readUnsignedInt32BigEndian(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFFL) << 24)
                | ((bytes[offset + 1] & 0xFFL) << 16)
                | ((bytes[offset + 2] & 0xFFL) << 8)
                | (bytes[offset + 3] & 0xFFL);
    }

    public record DecodedInspectData(
            String itemId,
            Integer defindex,
            Integer paintIndex,
            Integer rarity,
            Integer quality,
            BigDecimal paintWear,
            Integer paintSeed,
            Integer inventory,
            Integer origin,
            int stickerCount,
            int keychainCount,
            int variationCount
    ) {
        private boolean hasWrappedPayloadFields() {
            return itemId != null
                    || defindex != null
                    || paintIndex != null
                    || paintSeed != null
                    || stickerCount > 0
                    || keychainCount > 0
                    || variationCount > 0;
        }

        private boolean hasMaskedRequiredFields() {
            return itemId != null
                    && defindex != null
                    && paintIndex != null
                    && inventory != null
                    && origin != null;
        }
    }
}
