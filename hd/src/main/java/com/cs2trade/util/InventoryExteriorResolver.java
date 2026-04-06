package com.cs2trade.util;

import java.math.BigDecimal;

public final class InventoryExteriorResolver {

    private static final BigDecimal FACTORY_NEW_MAX = new BigDecimal("0.07");
    private static final BigDecimal MINIMAL_WEAR_MAX = new BigDecimal("0.15");
    private static final BigDecimal FIELD_TESTED_MAX = new BigDecimal("0.38");
    private static final BigDecimal WELL_WORN_MAX = new BigDecimal("0.45");
    private static final BigDecimal MAX_WEAR = BigDecimal.ONE;

    private InventoryExteriorResolver() {
    }

    public static String resolve(BigDecimal paintWear, String raw, String... nameCandidates) {
        String byWear = fromPaintWear(paintWear);
        if (byWear != null) {
            return byWear;
        }

        String inferred = inferFromNames(nameCandidates);
        if (inferred != null) {
            return inferred;
        }

        return normalize(raw);
    }

    public static String fromPaintWear(BigDecimal paintWear) {
        if (paintWear == null || paintWear.compareTo(BigDecimal.ZERO) < 0) {
            return null;
        }
        if (paintWear.compareTo(FACTORY_NEW_MAX) <= 0) {
            return "FN";
        }
        if (paintWear.compareTo(MINIMAL_WEAR_MAX) <= 0) {
            return "MW";
        }
        if (paintWear.compareTo(FIELD_TESTED_MAX) <= 0) {
            return "FT";
        }
        if (paintWear.compareTo(WELL_WORN_MAX) <= 0) {
            return "WW";
        }
        if (paintWear.compareTo(MAX_WEAR) <= 0) {
            return "BS";
        }
        return null;
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }

        String normalized = raw.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        return switch (normalized.toLowerCase()) {
            case "fn", "factory_new", "factory new", "崭新出厂" -> "FN";
            case "mw", "minimal_wear", "minimal wear", "略有磨损" -> "MW";
            case "ft", "field-tested", "field tested", "久经沙场" -> "FT";
            case "ww", "well-worn", "well worn", "破损不堪" -> "WW";
            case "bs", "battle-scarred", "battle scarred", "战痕累累" -> "BS";
            case "nopaint", "no paint", "无涂装" -> "NoPaint";
            default -> normalized;
        };
    }

    public static String inferFromNames(String... names) {
        if (names == null) {
            return null;
        }

        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }

            String normalized = name.trim();
            String lower = normalized.toLowerCase();
            String direct = normalize(normalized);
            if (isRecognizedExterior(direct)) {
                return direct;
            }
            if (normalized.contains("崭新出厂") || lower.contains("factory new")) {
                return "FN";
            }
            if (normalized.contains("略有磨损") || lower.contains("minimal wear")) {
                return "MW";
            }
            if (normalized.contains("久经沙场") || lower.contains("field-tested") || lower.contains("field tested")) {
                return "FT";
            }
            if (normalized.contains("破损不堪") || lower.contains("well-worn") || lower.contains("well worn")) {
                return "WW";
            }
            if (normalized.contains("战痕累累") || lower.contains("battle-scarred") || lower.contains("battle scarred")) {
                return "BS";
            }
            if (normalized.contains("无涂装") || lower.contains("no paint")) {
                return "NoPaint";
            }
        }
        return null;
    }

    private static boolean isRecognizedExterior(String exterior) {
        return "FN".equals(exterior)
                || "MW".equals(exterior)
                || "FT".equals(exterior)
                || "WW".equals(exterior)
                || "BS".equals(exterior)
                || "NoPaint".equals(exterior);
    }
}
