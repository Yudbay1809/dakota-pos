package com.dakota.pos.domain.usecase.auth

import com.dakota.pos.domain.model.User
import com.dakota.pos.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class GetActiveUsersUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<List<User>> {
        return authRepository.getActiveUsers()
    }
}
