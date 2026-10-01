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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(appointment: Appointment): Long

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun delete(id: Long)
}
