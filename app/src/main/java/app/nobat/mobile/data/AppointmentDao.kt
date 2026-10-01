package app.nobat.mobile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments WHERE day = :day ORDER BY startMinute ASC")
    fun forDay(day: String): Flow<List<Appointment>>

    /** Distinct ISO dates with at least one appointment in [start, end] inclusive. */
    @Query(
        "SELECT DISTINCT day FROM appointments WHERE day BETWEEN :start AND :end ORDER BY day ASC"
    )
    fun daysWithAppointments(start: String, end: String): Flow<List<String>>

    @Query(
        "SELECT day, COUNT(*) as count FROM appointments WHERE day BETWEEN :start AND :end GROUP BY day"
    )
    fun countsBetween(start: String, end: String): Flow<List<DayCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(appointment: Appointment): Long

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun delete(id: Long)
}

/** Room projection for month-grid markers. */
data class DayCount(
    val day: String,
    val count: Int,
)
