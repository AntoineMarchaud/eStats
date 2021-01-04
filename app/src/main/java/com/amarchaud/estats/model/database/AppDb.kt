package com.amarchaud.estats.model.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.amarchaud.estats.model.entity.LocationInfo

@Database(entities = [LocationInfo::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun AppDao(): AppDao
}