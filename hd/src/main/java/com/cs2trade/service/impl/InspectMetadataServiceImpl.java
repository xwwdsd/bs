package com.cs2trade.service.impl;

import com.cs2trade.entity.UserInventory;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.service.InspectMetadataService;
import com.cs2trade.util.Cs2InspectDecoder;
import com.cs2trade.util.InventoryExteriorResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectMetadataServiceImpl implements InspectMetadataService {

    private final Cs2InspectDecoder cs2InspectDecoder;
    private final UserInventoryMapper userInventoryMapper;

    @Override
    public boolean applyDecodedInspectMetadata(UserInventory inventory, boolean overwriteExisting) {
        if (inventory == null || inventory.getInspectUrl() == null || inventory.getInspectUrl().isBlank()) {
            return false;
        }

        return cs2InspectDecoder.decodeSafely(inventory.getInspectUrl())
                .map(decoded -> {
                    boolean changed = false;
                    changed |= applyInteger(decoded.paintSeed(), inventory.getPaintSeed(), inventory::setPaintSeed, overwriteExisting);
                    changed |= applyInteger(decoded.paintIndex(), inventory.getPaintIndex(), inventory::setPaintIndex, overwriteExisting);
                    changed |= applyDecimal(decoded.paintWear(), inventory.getPaintWear(), inventory::setPaintWear, overwriteExisting);
                    changed |= applyResolvedExterior(inventory);
                    return changed;
                })
                .orElse(false);
    }

    @Override
    public boolean repairAndPersist(UserInventory inventory) {
        if (inventory == null || inventory.getId() == null) {
            return false;
        }

        boolean changed = applyResolvedExterior(inventory);
        boolean needsInspectDecode = inventory.getInspectUrl() != null
                && !inventory.getInspectUrl().isBlank()
                && (inventory.getPaintWear() == null || inventory.getPaintSeed() == null || inventory.getPaintIndex() == null);
        if (needsInspectDecode) {
            changed |= applyDecodedInspectMetadata(inventory, false);
        }
        if (!changed) {
            return false;
        }

        userInventoryMapper.updateById(inventory);
        log.debug("Backfilled inspect metadata for inventoryId={}", inventory.getId());
        return true;
    }

    private boolean applyInteger(Integer decodedValue, Integer currentValue,
                                 java.util.function.Consumer<Integer> setter, boolean overwriteExisting) {
        if (decodedValue == null) {
            return false;
        }
        if (!overwriteExisting && currentValue != null) {
            return false;
        }
        if (decodedValue.equals(currentValue)) {
            return false;
        }
        setter.accept(decodedValue);
        return true;
    }

    private boolean applyDecimal(BigDecimal decodedValue, BigDecimal currentValue,
                                 java.util.function.Consumer<BigDecimal> setter, boolean overwriteExisting) {
        if (decodedValue == null) {
            return false;
        }
        if (!overwriteExisting && currentValue != null) {
            return false;
        }
        if (currentValue != null && decodedValue.compareTo(currentValue) == 0) {
            return false;
        }
        setter.accept(decodedValue);
        return true;
    }

    private boolean applyResolvedExterior(UserInventory inventory) {
        String resolved = InventoryExteriorResolver.resolve(
                inventory.getPaintWear(),
                inventory.getExterior(),
                inventory.getName()
        );
        if (resolved == null || resolved.equals(inventory.getExterior())) {
            return false;
        }
        inventory.setExterior(resolved);
        return true;
    }
}
