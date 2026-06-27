package com.example.automationtool.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.automationtool.data.dao.AutomationDao
import com.example.automationtool.data.entities.Action
import com.example.automationtool.data.entities.Automation
import com.example.automationtool.data.entities.AutomationStep

class Converters {
    @TypeConverter
    fun fromAction(value: Action) = value.name

    @TypeConverter
    fun toAction(value: String) = enumValueOf<Action>(value)
}

@Database(entities = [Automation::class, AutomationStep::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AutomationDatabase : RoomDatabase() {
    abstract fun automationDao(): AutomationDao

    companion object {
        @Volatile
        private var INSTANCE: AutomationDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE automation_steps ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AutomationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AutomationDatabase::class.java,
                    "automation_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
