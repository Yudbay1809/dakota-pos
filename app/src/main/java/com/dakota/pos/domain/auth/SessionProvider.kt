package com.dakota.pos.domain.auth

enum class UserRole {
    OWNER,
    MANAGER,
    CASHIER,
    BARISTA
}

enum class Permission {
    MANAGE_USERS,
    MANAGE_PRODUCTS,
    MANAGE_RECIPES,
    VIEW_HPP,
    ADJUST_INVENTORY,
    RECEIVE_PURCHASE_ORDERS,
    OPERATE_POS,
    PROCESS_REFUND,
    OPEN_CLOSE_SHIFT,
    MANAGE_PETTY_CASH,
    VIEW_KDS_SCREEN,
    VIEW_FINANCIAL_REPORTS
}

data class UserSession(
    val userId: String,
    val username: String,
    val role: UserRole,
    val permissions: Set<Permission>,
    val loginTimestamp: Long,
    val activeShiftId: String? = null
)

/**
 * Abstraksi penyedia sesi otentikasi lokal untuk penegakan izin akses (RBAC)
 * pada domain UseCase layer, bukan hanya penyembunyian komponen UI.
 */
interface SessionProvider {
    suspend fun getCurrentSession(): UserSession?
    suspend fun hasPermission(permission: Permission): Boolean {
        val session = getCurrentSession() ?: return false
        return session.permissions.contains(permission) || session.role == UserRole.OWNER
    }
    suspend fun clearSession()
}
