package com.cs2trade.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Cs2InspectDecoderTest {

    private static final String WRAPPED_HEX = "00180720DA03280638FBEE88F90340B2026BC03C96";
    private static final String WRAPPED_LINK =
            "steam://rungame/730/76561202255233023/+csgo_econ_action_preview 00180720DA03280638FBEE88F90340B2026BC03C96";
    private static final String MASKED_LINK =
            "steam://rungame/730/76561202255233023/+csgo_econ_action_preview%20CBDB3F0B6C6170CAD3F4EB60C2E3C9FBC7F3607D7A3DC88B29C8A9CEC3CADB5F80A9CEC3CBDB2289A9CEC3C8DB7381A9CEC3CFDB5F81A3484B4B4BC7BBC369CADDC3CBDB98F67323398B8E554C76F5869D456F8B9352CE341D4900";

    private final Cs2InspectDecoder decoder = new Cs2InspectDecoder();

    @Test
    void decodesWrappedInspectLinks() {
        Cs2InspectDecoder.DecodedInspectData decoded = decoder.decodeLink(WRAPPED_LINK);

        assertEquals(7, decoded.defindex());
        assertEquals(474, decoded.paintIndex());
        assertEquals(306, decoded.paintSeed());
        assertEquals(0.6336590648D, decoded.paintWear().doubleValue(), 0.0000001D);
    }

    @Test
    void decodesPercentEncodedInspectLinks() {
        Cs2InspectDecoder.DecodedInspectData decoded = decoder.decodeLink(
                "steam://rungame/730/76561202255233023/+csgo_econ_action_preview%2000180720DA03280638FBEE88F90340B2026BC03C96"
        );

        assertEquals(474, decoded.paintIndex());
        assertEquals(306, decoded.paintSeed());
        assertEquals(0.6336590648D, decoded.paintWear().doubleValue(), 0.0000001D);
    }

    @Test
    void decodesMaskedInspectLinks() {
        Cs2InspectDecoder.DecodedInspectData decoded = decoder.decodeLink(MASKED_LINK);

        assertEquals("50286157940", decoded.itemId());
        assertEquals(63, decoded.defindex());
        assertEquals(1195, decoded.paintIndex());
        assertEquals(482, decoded.paintSeed());
        assertEquals(0.3991330564D, decoded.paintWear().doubleValue(), 0.0000001D);
    }

    @Test
    void rejectsInvalidChecksum() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> decoder.decodeHex("00180720DA03280638FBEE88F90340B2026BC03C97")
        );

        assertEquals("Inspect hex checksum mismatch", error.getMessage());
    }

    @Test
    void rejectsInvalidLinks() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> decoder.decodeLink("steam://rungame/730/+csgo_econ_action_preview S76561198084749846A698323590D7935523998312483177")
        );

        assertEquals("Invalid inspect link", error.getMessage());
    }
}
