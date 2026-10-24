package com.dakota.pos.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakota.pos.domain.model.User
import com.dakota.pos.domain.repository.AuthRepository
import com.dakota.pos.domain.repository.AuthResult
import com.dakota.pos.domain.usecase.auth.GetActiveUsersUseCase
import com.dakota.pos.domain.usecase.auth.LoginWithPinUseCase
import com.dakota.pos.domain.usecase.auth.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val getActiveUsersUseCase: GetActiveUsersUseCase,
    private val loginWithPinUseCase: LoginWithPinUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.seedInitialUsersIfEmpty()
            getActiveUsersUseCase().collect { users ->
                _uiState.update { state ->
                    state.copy(
                        activeUsers = users,
                        selectedUser = state.selectedUser ?: users.firstOrNull()
                    )
                }
            }
        }
    }

    fun selectUser(user: User) {
        _uiState.update { it.copy(selectedUser = user, enteredPin = "", errorMessage = null) }
    }

    fun onPinDigitEntered(digit: Char) {
        val selectedRole = _uiState.value.selectedUser?.role?.name
        val targetLength = if (selectedRole == "CASHIER" || selectedRole == "BARISTA") 4 else 6
        val currentPin = _uiState.value.enteredPin
        if (currentPin.length >= targetLength) return
        val newPin = currentPin + digit
        _uiState.update { it.copy(enteredPin = newPin, errorMessage = null) }

        // Auto submit if target digits matching user
        if (newPin.length == targetLength) {
            submitPin(newPin)
        }
    }

    fun onPinBackspace() {
        val currentPin = _uiState.value.enteredPin
        if (currentPin.isNotEmpty()) {
            _uiState.update { it.copy(enteredPin = currentPin.dropLast(1), errorMessage = null) }
        }
    }

    fun onPinClear() {
        _uiState.update { it.copy(enteredPin = "", errorMessage = null) }
    }

    fun submitPin(pinOverride: String? = null) {
        val user = _uiState.value.selectedUser ?: return
        val pin = pinOverride ?: _uiState.value.enteredPin
        if (pin.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = loginWithPinUseCase(user.id, pin)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authenticatedSession = result.session,
                            enteredPin = "",
                            errorMessage = null
                        )
                    }
                }
                is AuthResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            enteredPin = "",
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.update {
                it.copy(
                    authenticatedSession = null,
                    enteredPin = "",
                    errorMessage = null
                )
            }
        }
    }
}
