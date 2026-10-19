package com.dakota.pos.core.sync

enum class SyncState {
    IDLE,
    SYNCING,
    SYNCED,
    FAILED,
    OFFLINE
}

data class SyncReport(
    val lastSyncTimestamp: Long,
    val pendingRecordsCount: Int,
    val syncedRecordsCount: Int,
    val errorsCount: Int
)

/**
 * Abstraksi antrean sinkronisasi offline-first.
 * Seluruh operasional kasir dan pencatatan transaksi disimpan lokal (Room DB)
 * dan dimasukkan ke antrean sinkronisasi ketika perangkat online.
 */
interface SyncEngine {
    val syncState: SyncState
    suspend fun queueForSync(entityType: String, entityId: String, payloadJson: String)
    suspend fun triggerSync(): SyncReport
    suspend fun getPendingCount(): Int
}
