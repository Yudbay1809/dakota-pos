package com.dakota.pos.data.repository

import com.dakota.pos.data.local.dao.AuditLogDao
import com.dakota.pos.data.local.dao.UserDao
import com.dakota.pos.data.local.entity.AuditLogEntity
import com.dakota.pos.data.local.entity.UserEntity
import com.dakota.pos.domain.auth.Permission
import com.dakota.pos.domain.auth.SessionProvider
import com.dakota.pos.domain.auth.UserRole
import com.dakota.pos.domain.auth.UserSession
import com.dakota.pos.domain.model.User
import com.dakota.pos.domain.repository.AuthRepository
import com.dakota.pos.domain.repository.AuthResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID

class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao
) : AuthRepository, SessionProvider {

    @Volatile
    private var activeSession: UserSession? = null

    companion object {
        fun hashString(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    override fun getActiveUsers(): Flow<List<User>> {
        return userDao.getAllActiveUsers().map { entities ->
            entities.map { entity ->
                User(
                    id = entity.id,
                    username = entity.username,
                    fullName = entity.fullName,
                    role = entity.role,
                    isActive = entity.isActive
                )
            }
        }
    }

    override suspend fun loginWithPin(userId: String, pin: String): AuthResult {
        val user = userDao.getUserById(userId)
            ?: return AuthResult.Failure("Pengguna tidak ditemukan").also {
                recordAuditLog("LOGIN_FAILED", "User ID $userId not found")
            }

        if (!user.isActive) {
            recordAuditLog("LOGIN_FAILED", "User ${user.username} is deactivated")
            return AuthResult.Failure("Akun ini telah dinonaktifkan")
        }

        val hashedInput = hashString(pin)
        if (user.pinHash != hashedInput) {
            recordAuditLog("LOGIN_FAILED", "Invalid PIN attempt for user ${user.username}")
            return AuthResult.Failure("PIN yang Anda masukkan salah")
        }

        val domainUser = User(
            id = user.id,
            username = user.username,
            fullName = user.fullName,
            role = user.role,
            isActive = user.isActive
        )

        val session = UserSession(
            userId = user.id,
            username = user.username,
            role = user.role,
            permissions = domainUser.defaultPermissions,
            loginTimestamp = System.currentTimeMillis()
        )
        activeSession = session

        recordAuditLog("LOGIN_SUCCESS", "User ${user.username} logged in with PIN")
        return AuthResult.Success(session)
    }

    override suspend fun loginWithPassword(username: String, password: String): AuthResult {
        val user = userDao.getUserByUsername(username)
            ?: return AuthResult.Failure("Pengguna tidak ditemukan").also {
                recordAuditLog("LOGIN_FAILED", "Username $username not found")
            }

        if (!user.isActive) {
            recordAuditLog("LOGIN_FAILED", "User $username is deactivated")
            return AuthResult.Failure("Akun ini telah dinonaktifkan")
        }

        val hashedInput = hashString(password)
        if (user.passwordHash == null || user.passwordHash != hashedInput) {
            recordAuditLog("LOGIN_FAILED", "Invalid password attempt for user $username")
            return AuthResult.Failure("Password yang Anda masukkan salah")
        }

        val domainUser = User(
            id = user.id,
            username = user.username,
            fullName = user.fullName,
            role = user.role,
            isActive = user.isActive
        )

        val session = UserSession(
            userId = user.id,
            username = user.username,
            role = user.role,
            permissions = domainUser.defaultPermissions,
            loginTimestamp = System.currentTimeMillis()
        )
        activeSession = session

        recordAuditLog("LOGIN_SUCCESS", "User $username logged in with Password")
        return AuthResult.Success(session)
    }

    override suspend fun logout(): Boolean {
        val session = activeSession
        if (session != null) {
            recordAuditLog("LOGOUT", "User ${session.username} logged out")
        }
        activeSession = null
        return true
    }

    override suspend fun getCurrentSession(): UserSession? = activeSession

    override suspend fun clearSession() {
        logout()
    }

    override suspend fun recordAuditLog(action: String, details: String?) {
        val current = activeSession
        val log = AuditLogEntity(
            id = UUID.randomUUID().toString(),
            userId = current?.userId ?: "SYSTEM",
            username = current?.username ?: "ANONYMOUS",
            action = action,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        try {
            auditLogDao.insertLog(log)
        } catch (_: Exception) {
            // Failsafe: logging failure shouldn't crash POS
        }
    }

    override suspend fun seedInitialUsersIfEmpty() {
        if (userDao.getUserCount() == 0) {
            val initialUsers = listOf(
                UserEntity(
                    id = "usr-owner-01",
                    username = "owner",
                    fullName = "Owner Dakota",
                    role = UserRole.OWNER,
                    pinHash = hashString("123456"),
                    passwordHash = hashString("admin123")
                ),
                UserEntity(
                    id = "usr-manager-01",
                    username = "manager",
                    fullName = "Operational Manager",
                    role = UserRole.MANAGER,
                    pinHash = hashString("112233"),
                    passwordHash = hashString("manager123")
                ),
                UserEntity(
                    id = "usr-cashier-01",
                    username = "cashier1",
                    fullName = "Kasir Utama (Shift 1)",
                    role = UserRole.CASHIER,
                    pinHash = hashString("1234")
                ),
                UserEntity(
                    id = "usr-barista-01",
                    username = "barista1",
                    fullName = "Barista Bar 1",
                    role = UserRole.BARISTA,
                    pinHash = hashString("0000")
                )
            )
            userDao.insertUsers(initialUsers)
            recordAuditLog("SYSTEM_INIT", "Seeded default users for Dakota POS")
        }
    }
}
