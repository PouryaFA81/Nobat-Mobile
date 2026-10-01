package app.nobat.mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Appointment::class, Account::class, Personnel::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appointments(): AppointmentDao
    abstract fun accounts(): AccountDao
    abstract fun personnel(): PersonnelDao

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

        /**
         * v2 → v3: personnel table; appointments.personnelId + personnelEmail snapshot.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS personnel (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        accountId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        email TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        notes TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_personnel_accountId ON personnel(accountId)",
                )
                db.execSQL(
                    "ALTER TABLE appointments ADD COLUMN personnelId INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE appointments ADD COLUMN personnelEmail TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_appointments_personnelId ON appointments(personnelId)",
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
