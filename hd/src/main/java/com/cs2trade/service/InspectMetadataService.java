package com.cs2trade.service;

import com.cs2trade.entity.UserInventory;

/**
 * Repairs and hydrates inspect-derived inventory metadata.
 */
public interface InspectMetadataService {

    /**
     * Applies decoded inspect metadata onto an inventory snapshot.
     *
     * @param inventory          inventory snapshot
     * @param overwriteExisting  whether decoded values should overwrite existing values
     * @return true if any field changed
     */
    boolean applyDecodedInspectMetadata(UserInventory inventory, boolean overwriteExisting);

    /**
     * Repairs missing inspect metadata and persists the snapshot if any field is backfilled.
     *
     * @param inventory inventory snapshot
     * @return true if any field changed and was persisted
     */
    boolean repairAndPersist(UserInventory inventory);
}
