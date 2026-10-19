package com.dakota.pos.domain.usecase.auth

import com.dakota.pos.domain.auth.Permission
import com.dakota.pos.domain.auth.SessionProvider

class CheckPermissionUseCase(
    private val sessionProvider: SessionProvider
) {
    suspend operator fun invoke(permission: Permission): Boolean {
        return sessionProvider.hasPermission(permission)
    }
}
