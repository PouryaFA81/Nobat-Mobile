package app.nobat.mobile.data

/**
 * Per-account role for schedule visibility and booking (Phase 1 — same phone).
 * Stored as plain string on [Account.role].
 */
object AccountRole {
    const val ADMIN = "admin"
    const val STAFF = "staff"

    fun isAdmin(role: String): Boolean = role == ADMIN
    fun isStaff(role: String): Boolean = role == STAFF
}
