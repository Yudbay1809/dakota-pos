package com.dakota.pos.domain.repository

import com.dakota.pos.domain.auth.UserSession
import com.dakota.pos.domain.model.User
import kotlinx.coroutines.flow.Flow

sealed class AuthResult {
    data class Success(val session: UserSession) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

interface AuthRepository {
    fun getActiveUsers(): Flow<List<User>>
    suspend fun loginWithPin(userId: String, pin: String): AuthResult
    suspend fun loginWithPassword(username: String, password: String): AuthResult
    suspend fun logout(): Boolean
    suspend fun getCurrentSession(): UserSession?
    suspend fun seedInitialUsersIfEmpty()
    suspend fun recordAuditLog(action: String, details: String?)
}
