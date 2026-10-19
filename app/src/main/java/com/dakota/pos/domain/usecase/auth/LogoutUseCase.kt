package com.dakota.pos.domain.usecase.auth

import com.dakota.pos.domain.repository.AuthRepository

class LogoutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Boolean {
        return authRepository.logout()
    }
}
