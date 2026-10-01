package app.nobat.mobile.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "appointments",
    indices = [Index(value = ["accountId"])],
)
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Owning local account; 0 = orphan (pre-migration / pending attach). */
    val accountId: Long = 0,
    /** ISO local date yyyy-MM-dd */
    val day: String,
    /** minutes from midnight */
    val startMinute: Int,
    val durationMin: Int = 60,
    val initials: String,
    val note: String = "",
    val status: String = "active",
)
