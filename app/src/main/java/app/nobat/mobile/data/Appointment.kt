package app.nobat.mobile.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "appointments",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["personnelId"]),
    ],
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
    /** Staff who owns this slot (SMTP target). 0 = unset / legacy. */
    val personnelId: Long = 0,
    /**
     * Email snapshot at book time so 1h reminders still know the target
     * if the personnel row is later edited or deleted.
     */
    val personnelEmail: String = "",
)
