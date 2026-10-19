package com.dakota.pos.domain.usecase.auth

import com.dakota.pos.domain.repository.AuthRepository
import com.dakota.pos.domain.repository.AuthResult

class LoginWithPinUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(userId: String, pin: String): AuthResult {
        if (pin.isBlank() || pin.length < 4 || !pin.all { it.isDigit() }) {
            return AuthResult.Failure("PIN harus berupa 4-6 digit angka")
        }
        return authRepository.loginWithPin(userId, pin)
    }
}
