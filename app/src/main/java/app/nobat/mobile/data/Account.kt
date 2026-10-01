package app.nobat.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    /** Base64-encoded PBKDF2 hash — never plaintext. */
    val passwordHash: String,
    /** Base64-encoded salt. */
    val salt: String,
    val createdAt: Long = System.currentTimeMillis(),
)
