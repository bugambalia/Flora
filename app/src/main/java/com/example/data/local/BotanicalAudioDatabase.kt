package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    TrackEntity::class,
    PlaylistEntity::class,
    PlaylistTrackCrossRef::class,
    BotanicalProgressEntity::class,
  ],
  version = 3,
  exportSchema = false,
)
abstract class BotanicalAudioDatabase : RoomDatabase() {
  abstract fun audioDao(): BotanicalAudioDao

  companion object {
    @Volatile
    private var INSTANCE: BotanicalAudioDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN sourceFolder TEXT NOT NULL DEFAULT 'Herbario'")
        db.execSQL("ALTER TABLE tracks ADD COLUMN sourcePath TEXT NOT NULL DEFAULT 'herbarium://bundled'")
      }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN lyrics TEXT")
      }
    }

    fun getInstance(context: Context): BotanicalAudioDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          BotanicalAudioDatabase::class.java,
          "herbarium_audio.db"
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}
