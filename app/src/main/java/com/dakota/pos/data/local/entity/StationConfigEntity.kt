package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "station_configs")
data class StationConfigEntity(
    @PrimaryKey
    val stationId: String,
    val name: String,
    val printerType: String,
    val targetAddress: String,
    val autoPrintOnDispatch: Boolean,
    val categoryIdsJson: String // Comma-separated or JSON list of category IDs
)
