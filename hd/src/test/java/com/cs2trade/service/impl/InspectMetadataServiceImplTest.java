package com.cs2trade.service.impl;

import com.cs2trade.entity.UserInventory;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.util.Cs2InspectDecoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InspectMetadataServiceImplTest {

    private static final String WRAPPED_LINK =
            "steam://rungame/730/76561202255233023/+csgo_econ_action_preview 00180720DA03280638FBEE88F90340B2026BC03C96";

    @Mock
    private UserInventoryMapper userInventoryMapper;

    private InspectMetadataServiceImpl inspectMetadataService;

    @BeforeEach
    void setUp() {
        inspectMetadataService = new InspectMetadataServiceImpl(new Cs2InspectDecoder(), userInventoryMapper);
    }

    @Test
    void repairsMissingInspectFieldsAndPersistsSnapshot() {
        UserInventory inventory = new UserInventory();
        inventory.setId(1L);
        inventory.setInspectUrl(WRAPPED_LINK);

        boolean changed = inspectMetadataService.repairAndPersist(inventory);

        assertTrue(changed);
        assertEquals(474, inventory.getPaintIndex());
        assertEquals(306, inventory.getPaintSeed());
        assertEquals(0.6336590648D, inventory.getPaintWear().doubleValue(), 0.0000001D);
        assertEquals("BS", inventory.getExterior());
        verify(userInventoryMapper).updateById(inventory);
    }

    @Test
    void syncPathCanOverwriteFallbackParsedWear() {
        UserInventory inventory = new UserInventory();
        inventory.setInspectUrl(WRAPPED_LINK);
        inventory.setPaintWear(new BigDecimal("0.01"));
        inventory.setPaintSeed(1);
        inventory.setPaintIndex(2);

        boolean changed = inspectMetadataService.applyDecodedInspectMetadata(inventory, true);

        assertTrue(changed);
        assertEquals(474, inventory.getPaintIndex());
        assertEquals(306, inventory.getPaintSeed());
        assertEquals(0.6336590648D, inventory.getPaintWear().doubleValue(), 0.0000001D);
        assertEquals("BS", inventory.getExterior());
        verifyNoInteractions(userInventoryMapper);
    }

    @Test
    void repairsExteriorFromStoredWearEvenWithoutInspectLink() {
        UserInventory inventory = new UserInventory();
        inventory.setId(2L);
        inventory.setExterior("FN");
        inventory.setPaintWear(new BigDecimal("0.3238567"));

        boolean changed = inspectMetadataService.repairAndPersist(inventory);

        assertTrue(changed);
        assertEquals("FT", inventory.getExterior());
        verify(userInventoryMapper).updateById(inventory);
    }
}
