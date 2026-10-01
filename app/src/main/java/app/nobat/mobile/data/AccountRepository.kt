package app.nobat.mobile.data

import app.nobat.mobile.security.PasswordHasher
import app.nobat.mobile.session.AccountSession
import kotlinx.coroutines.flow.Flow

class AccountRepository(
    private val accounts: AccountDao,
    private val appointments: AppointmentDao,
    private val personnel: PersonnelDao,
    private val session: AccountSession,
) {
    fun observeAccounts(): Flow<List<Account>> = accounts.observeAll()

    suspend fun listAccounts(): List<Account> = accounts.listAll()

    suspend fun accountCount(): Int = accounts.count()

    suspend fun orphanAppointmentCount(): Int = appointments.countOrphans()

    /**
     * True when upgrading from v1 with existing rows and no accounts yet —
     * UI should force Create account and attach orphans.
     */
    suspend fun needsOrphanMigration(): Boolean =
        accounts.count() == 0 && appointments.countOrphans() > 0

    suspend fun getAccount(id: Long): Account? = accounts.getById(id)

    /**
     * Create a local account. When [attachOrphans] is true (post-upgrade first account),
     * existing appointments with accountId=0 are reassigned to this account.
     * Account password is for create / change-password only — day-to-day unlock is PIN/biometric.
     * New accounts default to Admin / Secretary with no personnel link.
     */
    suspend fun createAccount(
        displayName: String,
        password: CharArray,
        attachOrphans: Boolean,
    ): Result<Account> {
        val name = displayName.trim()
        if (name.isEmpty()) return Result.failure(IllegalArgumentException("empty_name"))
        if (password.isEmpty()) return Result.failure(IllegalArgumentException("empty_password"))

        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash(password, salt)
        password.fill('\u0000')

        val id = accounts.insert(
            Account(
                displayName = name,
                passwordHash = PasswordHasher.encode(hash),
                salt = PasswordHasher.encode(salt),
                role = AccountRole.ADMIN,
                linkedPersonnelId = 0L,
            ),
        )
        if (attachOrphans) {
            appointments.attachOrphans(id)
        }
        val created = accounts.getById(id)
            ?: return Result.failure(IllegalStateException("insert_failed"))
        session.unlock(id)
        return Result.success(created)
    }

    suspend fun changePassword(
        accountId: Long,
        currentPassword: CharArray,
        newPassword: CharArray,
    ): Boolean {
        val account = accounts.getById(accountId) ?: return false
        val salt = PasswordHasher.decode(account.salt)
        val expected = PasswordHasher.decode(account.passwordHash)
        val ok = PasswordHasher.verify(currentPassword, salt, expected)
        currentPassword.fill('\u0000')
        if (!ok) {
            newPassword.fill('\u0000')
            return false
        }
        if (newPassword.isEmpty()) {
            newPassword.fill('\u0000')
            return false
        }
        val newSalt = PasswordHasher.generateSalt()
        val newHash = PasswordHasher.hash(newPassword, newSalt)
        newPassword.fill('\u0000')
        accounts.update(
            account.copy(
                passwordHash = PasswordHasher.encode(newHash),
                salt = PasswordHasher.encode(newSalt),
            ),
        )
        return true
    }

    /** Set role to [AccountRole.ADMIN] or [AccountRole.STAFF]. */
    suspend fun setRole(accountId: Long, role: String): Boolean {
        if (role != AccountRole.ADMIN && role != AccountRole.STAFF) return false
        val account = accounts.getById(accountId) ?: return false
        accounts.update(account.copy(role = role))
        return true
    }

    /**
     * Link this account to a personnel row for My schedule.
     * Pass 0 to clear. Staff should keep a valid link to see appointments.
     */
    suspend fun setLinkedPersonnel(accountId: Long, personnelId: Long): Boolean {
        val account = accounts.getById(accountId) ?: return false
        if (personnelId > 0L) {
            val person = personnel.get(accountId, personnelId) ?: return false
            accounts.update(account.copy(linkedPersonnelId = person.id))
        } else {
            accounts.update(account.copy(linkedPersonnelId = 0L))
        }
        return true
    }

    fun switchAccount() {
        session.lock()
    }
}
