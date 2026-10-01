package app.nobat.mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Appointment::class, Account::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appointments(): AppointmentDao
    abstract fun accounts(): AccountDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * v1 → v2: add accounts table; add appointments.accountId (default 0 = orphan).
         * Orphans are attached when the user creates the first account after upgrade
         * (see AccountRepository.createAccount attachOrphans).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        displayName TEXT NOT NULL,
                        passwordHash TEXT NOT NULL,
                        salt TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "ALTER TABLE appointments ADD COLUMN accountId INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_appointments_accountId ON appointments(accountId)",
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nobat.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
