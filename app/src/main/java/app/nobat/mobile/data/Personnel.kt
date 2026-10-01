package app.nobat.mobile.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Staff who receive appointment confirmation + reminder emails.
 * Not an app login account — scoped to a local [Account].
 */
@Entity(
    tableName = "personnel",
    indices = [Index(value = ["accountId"])],
)
data class Personnel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val name: String,
    /** Required for SMTP confirmation / 1h reminder. */
    val email: String,
    val phone: String = "",
    val notes: String = "",
)
