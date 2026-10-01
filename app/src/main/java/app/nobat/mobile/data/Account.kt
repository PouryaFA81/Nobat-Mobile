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
    /**
     * [AccountRole.ADMIN] (default) or [AccountRole.STAFF].
     * Admin can see Everyone + My schedule and book; Staff sees My schedule only.
     */
    val role: String = AccountRole.ADMIN,
    /**
     * Linked [Personnel] id for My schedule filtering (and required for Staff).
     * 0 = not linked.
     */
    val linkedPersonnelId: Long = 0,
)
