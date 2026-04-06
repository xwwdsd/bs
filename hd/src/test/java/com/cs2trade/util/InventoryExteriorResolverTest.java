package com.cs2trade.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventoryExteriorResolverTest {

    @Test
    void derivesExteriorFromWearThresholds() {
        assertEquals("FN", InventoryExteriorResolver.fromPaintWear(new BigDecimal("0.07")));
        assertEquals("MW", InventoryExteriorResolver.fromPaintWear(new BigDecimal("0.08")));
        assertEquals("FT", InventoryExteriorResolver.fromPaintWear(new BigDecimal("0.20")));
        assertEquals("WW", InventoryExteriorResolver.fromPaintWear(new BigDecimal("0.40")));
        assertEquals("BS", InventoryExteriorResolver.fromPaintWear(new BigDecimal("0.90")));
        assertNull(InventoryExteriorResolver.fromPaintWear(new BigDecimal("1.10")));
    }

    @Test
    void prefersWearOverNameOrRawExterior() {
        String resolved = InventoryExteriorResolver.resolve(
                new BigDecimal("0.3238567"),
                "WW",
                "M4A1-S | Printstream (Factory New)"
        );

        assertEquals("FT", resolved);
    }

    @Test
    void normalizesCommonExteriorAliases() {
        assertEquals("FN", InventoryExteriorResolver.normalize("factory new"));
        assertEquals("MW", InventoryExteriorResolver.normalize("MW"));
        assertEquals("NoPaint", InventoryExteriorResolver.normalize("无涂装"));
    }
}
