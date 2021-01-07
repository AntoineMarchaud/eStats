package com.amarchaud.estats.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import javax.annotation.Nullable

@Entity(tableName = "SubLocations")
data class LocationInfoSub(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "idSub") @Nullable val idSub: Int? = null,
    @ColumnInfo(name = "idMain") @Nullable val idMain: Int? = null, // the id from LocationInfo
    @ColumnInfo(name = "name") @Nullable val name: String? = null,
    @ColumnInfo(name = "delta") val delta: Int = 7,
    @ColumnInfo(name = "lat") val lat: Double = 0.0,
    @ColumnInfo(name = "lon") val lon: Double = 0.0,
    @ColumnInfo(name = "duration_day") var duration_day: Int = 0,
    @ColumnInfo(name = "duration_week") var duration_week: Int = 0,
    @ColumnInfo(name = "duration_month") var duration_month: Int = 0,
    @ColumnInfo(name = "duration_year") var duration_year: Int = 0,
    @ColumnInfo(name = "duration_all_time") var duration_all_time: Int = 0,
)