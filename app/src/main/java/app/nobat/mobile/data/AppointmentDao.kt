package app.nobat.mobile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query(
        "SELECT * FROM appointments WHERE accountId = :accountId AND day = :day ORDER BY startMinute ASC"
    )
    fun forDay(accountId: Long, day: String): Flow<List<Appointment>>

    /** Distinct ISO dates with at least one appointment in [start, end] inclusive. */
    @Query(
        "SELECT DISTINCT day FROM appointments WHERE accountId = :accountId AND day BETWEEN :start AND :end ORDER BY day ASC"
    )
    fun daysWithAppointments(accountId: Long, start: String, end: String): Flow<List<String>>

    @Query(
        "SELECT day, COUNT(*) as count FROM appointments WHERE accountId = :accountId AND day BETWEEN :start AND :end GROUP BY day"
    )
    fun countsBetween(accountId: Long, start: String, end: String): Flow<List<DayCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(appointment: Appointment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Appointment>)

    @Query("DELETE FROM appointments WHERE id = :id AND accountId = :accountId")
    suspend fun delete(accountId: Long, id: Long)

    @Query("SELECT * FROM appointments WHERE accountId = :accountId ORDER BY day ASC, startMinute ASC")
    suspend fun allForAccount(accountId: Long): List<Appointment>

    @Query(
        "SELECT * FROM appointments WHERE accountId = :accountId AND day BETWEEN :start AND :end ORDER BY day ASC, startMinute ASC"
    )
    suspend fun between(accountId: Long, start: String, end: String): List<Appointment>


    @Query("SELECT * FROM appointments WHERE id = :id AND accountId = :accountId LIMIT 1")
    suspend fun get(accountId: Long, id: Long): Appointment?

    @Query("DELETE FROM appointments WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: Long)

    @Query("SELECT COUNT(*) FROM appointments WHERE accountId = 0")
    suspend fun countOrphans(): Int

    @Query("UPDATE appointments SET accountId = :accountId WHERE accountId = 0")
    suspend fun attachOrphans(accountId: Long): Int
}

/** Room projection for month-grid markers. */
data class DayCount(
    val day: String,
    val count: Int,
)
