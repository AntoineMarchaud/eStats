package com.amarchaud.estats.model.database

import android.content.ContentValues
import androidx.annotation.NonNull
import androidx.room.Database
import androidx.room.OnConflictStrategy
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.model.entity.LocationInfoSub


@Database(entities = [LocationInfo::class, LocationInfoSub::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun AppDao(): AppDao
}