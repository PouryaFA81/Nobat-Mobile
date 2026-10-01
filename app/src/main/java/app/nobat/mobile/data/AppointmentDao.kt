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

    @Query("DELETE FROM appointments WHERE id = :id AND accountId = :accountId")
    suspend fun delete(accountId: Long, id: Long)

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
