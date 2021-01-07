package com.amarchaud.estats.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import javax.annotation.Nullable

@Entity(tableName = "Locations")
data class LocationInfo(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Int = 0,
    @ColumnInfo(name = "name") @Nullable val name: String? = null,
    @ColumnInfo(name = "delta") val delta: Int = 7,
    @ColumnInfo(name = "lat") val lat: Double = 0.0,
    @ColumnInfo(name = "lon") val lon: Double = 0.0,
    @ColumnInfo(name = "duration_day") var duration_day: Long = 0, // all these value in ms
    @ColumnInfo(name = "duration_week") var duration_week: Long = 0,
    @ColumnInfo(name = "duration_month") var duration_month: Long = 0,
    @ColumnInfo(name = "duration_year") var duration_year: Long = 0,
    @ColumnInfo(name = "duration_all_time") var duration_all_time: Long = 0
)