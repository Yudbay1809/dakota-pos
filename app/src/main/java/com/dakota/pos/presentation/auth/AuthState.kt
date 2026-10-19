package com.dakota.pos.presentation.auth

import com.dakota.pos.domain.auth.UserSession
import com.dakota.pos.domain.model.User

data class AuthUiState(
    val activeUsers: List<User> = emptyList(),
    val selectedUser: User? = null,
    val enteredPin: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authenticatedSession: UserSession? = null
)
