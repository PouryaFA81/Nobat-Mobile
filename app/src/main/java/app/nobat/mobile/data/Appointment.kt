package app.nobat.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO local date yyyy-MM-dd */
    val day: String,
    /** minutes from midnight */
    val startMinute: Int,
    val durationMin: Int = 60,
    val initials: String,
    val note: String = "",
    val status: String = "active",
)
