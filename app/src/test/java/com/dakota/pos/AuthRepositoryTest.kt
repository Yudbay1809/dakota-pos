package com.dakota.pos

import com.dakota.pos.data.local.dao.AuditLogDao
import com.dakota.pos.data.local.dao.UserDao
import com.dakota.pos.data.local.entity.AuditLogEntity
import com.dakota.pos.data.local.entity.UserEntity
import com.dakota.pos.data.repository.AuthRepositoryImpl
import com.dakota.pos.domain.auth.Permission
import com.dakota.pos.domain.auth.UserRole
import com.dakota.pos.domain.repository.AuthResult
import com.dakota.pos.domain.usecase.auth.CheckPermissionUseCase
import com.dakota.pos.domain.usecase.auth.LoginWithPinUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private val inMemoryUsers = mutableListOf<UserEntity>()
    private val inMemoryAuditLogs = mutableListOf<AuditLogEntity>()

    private val fakeUserDao = object : UserDao {
        override fun getAllActiveUsers(): Flow<List<UserEntity>> = flowOf(inMemoryUsers.filter { it.isActive })
        override suspend fun getUserById(id: String): UserEntity? = inMemoryUsers.find { it.id == id }
        override suspend fun getUserByUsername(username: String): UserEntity? = inMemoryUsers.find { it.username == username }
        override suspend fun insertUser(user: UserEntity) { inMemoryUsers.add(user) }
        override suspend fun insertUsers(users: List<UserEntity>) { inMemoryUsers.addAll(users) }
        override suspend fun updateUser(user: UserEntity) {
            val idx = inMemoryUsers.indexOfFirst { it.id == user.id }
            if (idx != -1) inMemoryUsers[idx] = user
        }
        override suspend fun getUserCount(): Int = inMemoryUsers.size
    }

    private val fakeAuditLogDao = object : AuditLogDao {
        override suspend fun insertLog(log: AuditLogEntity) { inMemoryAuditLogs.add(log) }
        override fun getRecentLogs(limit: Int): Flow<List<AuditLogEntity>> = flowOf(inMemoryAuditLogs)
        override fun getLogsByUserId(userId: String, limit: Int): Flow<List<AuditLogEntity>> =
            flowOf(inMemoryAuditLogs.filter { it.userId == userId })
    }

    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var loginWithPinUseCase: LoginWithPinUseCase
    private lateinit var checkPermissionUseCase: CheckPermissionUseCase

    @Before
    fun setup() = runTest {
        inMemoryUsers.clear()
        inMemoryAuditLogs.clear()
        authRepository = AuthRepositoryImpl(fakeUserDao, fakeAuditLogDao)
        loginWithPinUseCase = LoginWithPinUseCase(authRepository)
        checkPermissionUseCase = CheckPermissionUseCase(authRepository)

        // Seed initial users
        authRepository.seedInitialUsersIfEmpty()
    }

    @Test
    fun `test initial seeding creates 4 standard roles`() = runTest {
        assertEquals(4, inMemoryUsers.size)
        assertTrue(inMemoryUsers.any { it.role == UserRole.OWNER })
        assertTrue(inMemoryUsers.any { it.role == UserRole.MANAGER })
        assertTrue(inMemoryUsers.any { it.role == UserRole.CASHIER })
        assertTrue(inMemoryUsers.any { it.role == UserRole.BARISTA })
        assertTrue(inMemoryAuditLogs.any { it.action == "SYSTEM_INIT" })
    }

    @Test
    fun `test cashier login with correct PIN succeeds and enforces permissions`() = runTest {
        val cashier = inMemoryUsers.first { it.role == UserRole.CASHIER }
        val result = loginWithPinUseCase(cashier.id, "1234")

        assertTrue(result is AuthResult.Success)
        val session = (result as AuthResult.Success).session
        assertEquals(cashier.id, session.userId)
        assertEquals(UserRole.CASHIER, session.role)

        // Permission check
        assertTrue(checkPermissionUseCase(Permission.OPERATE_POS))
        assertFalse(checkPermissionUseCase(Permission.VIEW_FINANCIAL_REPORTS))
        assertFalse(checkPermissionUseCase(Permission.VIEW_HPP))

        // Check audit log
        assertTrue(inMemoryAuditLogs.any { it.action == "LOGIN_SUCCESS" && it.userId == cashier.id })
    }

    @Test
    fun `test login with incorrect PIN fails and records audit log`() = runTest {
        val cashier = inMemoryUsers.first { it.role == UserRole.CASHIER }
        val result = loginWithPinUseCase(cashier.id, "9999")

        assertTrue(result is AuthResult.Failure)
        assertEquals("PIN yang Anda masukkan salah", (result as AuthResult.Failure).message)
        assertNull(authRepository.getCurrentSession())

        // Audit log failed attempt
        assertTrue(inMemoryAuditLogs.any { it.action == "LOGIN_FAILED" })
    }

    @Test
    fun `test logout clears session and records audit log`() = runTest {
        val cashier = inMemoryUsers.first { it.role == UserRole.CASHIER }
        loginWithPinUseCase(cashier.id, "1234")
        assertNotNull(authRepository.getCurrentSession())

        authRepository.logout()
        assertNull(authRepository.getCurrentSession())
        assertTrue(inMemoryAuditLogs.any { it.action == "LOGOUT" })
    }
}
