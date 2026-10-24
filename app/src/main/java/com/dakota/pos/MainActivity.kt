package com.dakota.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dakota.pos.presentation.auth.AuthViewModel
import com.dakota.pos.presentation.auth.LoginPinScreen
import com.dakota.pos.presentation.pos.PosScreen
import com.dakota.pos.presentation.pos.PosViewModel
import com.dakota.pos.presentation.theme.DakotaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DakotaApp
        val authViewModel = AuthViewModel(
            authRepository = app.authRepository,
            getActiveUsersUseCase = app.getActiveUsersUseCase,
            loginWithPinUseCase = app.loginWithPinUseCase,
            logoutUseCase = app.logoutUseCase
        )

        setContent {
            DakotaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by authViewModel.uiState.collectAsState()

                    if (uiState.authenticatedSession == null) {
                        LoginPinScreen(
                            uiState = uiState,
                            onSelectUser = { authViewModel.selectUser(it) },
                            onDigitClick = { authViewModel.onPinDigitEntered(it) },
                            onBackspaceClick = { authViewModel.onPinBackspace() },
                            onClearClick = { authViewModel.onPinClear() },
                            onSubmitClick = { authViewModel.submitPin() }
                        )
                    } else {
                        val session = uiState.authenticatedSession!!
                        val posViewModel = remember(session.userId) {
                            PosViewModel(
                                getCatalogUseCase = app.getCatalogUseCase,
                                calculateCartUseCase = app.calculateCartUseCase,
                                checkoutOrderUseCase = app.checkoutOrderUseCase,
                                processPaymentUseCase = app.processPaymentUseCase,
                                printReceiptUseCase = app.printReceiptUseCase,
                                productRepository = app.productRepository,
                                cashierId = session.userId,
                                cashierName = session.username,
                                dispatchOrderToKitchenUseCase = app.dispatchOrderToKitchenUseCase
                            )
                        }

                        val recipeViewModel = remember(session.userId) {
                            com.dakota.pos.presentation.recipe.RecipeViewModel(
                                recipeRepository = app.recipeRepository,
                                productRepository = app.productRepository,
                                calculateHppUseCase = app.calculateHppUseCase,
                                getIngredientsUseCase = app.getIngredientsUseCase,
                                saveIngredientUseCase = app.saveIngredientUseCase,
                                saveRecipeUseCase = app.saveRecipeUseCase
                            )
                        }

                        val inventoryViewModel = remember(session.userId) {
                            com.dakota.pos.ui.inventory.InventoryViewModel(
                                getInventoryDataUseCase = app.getInventoryDataUseCase,
                                manualStockMovementUseCase = app.manualStockMovementUseCase,
                                recordWastageUseCase = app.recordWastageUseCase,
                                performStockOpnameUseCase = app.performStockOpnameUseCase
                            )
                        }

                        val purchasingViewModel = remember(session.userId) {
                            com.dakota.pos.ui.purchasing.PurchasingViewModel(
                                getPurchasingDataUseCase = app.getPurchasingDataUseCase,
                                createPOUseCase = app.createPOUseCase,
                                receiveGoodsUseCase = app.receiveGoodsUseCase
                            )
                        }

                        val shiftViewModel = remember(session.userId) {
                            com.dakota.pos.ui.shift.ShiftViewModel(
                                openShiftUseCase = app.openShiftUseCase,
                                recordCashMovementUseCase = app.recordCashMovementUseCase,
                                closeShiftUseCase = app.closeShiftUseCase,
                                getCurrentShiftUseCase = app.getCurrentShiftUseCase,
                                generateShiftReportUseCase = app.generateShiftReportUseCase
                            )
                        }

                        val kdsViewModel = remember(session.userId) {
                            com.dakota.pos.ui.kds.KdsViewModel(
                                getKdsOrdersUseCase = app.getKdsOrdersUseCase,
                                updateKitchenOrderStatusUseCase = app.updateKitchenOrderStatusUseCase,
                                printStationTicketUseCase = app.printStationTicketUseCase,
                                manageStationConfigUseCase = app.manageStationConfigUseCase
                            )
                        }

                        val financeViewModel = remember(session.userId) {
                            com.dakota.pos.ui.finance.FinanceViewModel(
                                generateProfitLossReportUseCase = app.generateProfitLossReportUseCase,
                                recordOperatingExpenseUseCase = app.recordOperatingExpenseUseCase,
                                deleteOperatingExpenseUseCase = app.deleteOperatingExpenseUseCase,
                                getOperatingExpensesUseCase = app.getOperatingExpensesUseCase
                            )
                        }

                        val staffViewModel = remember(session.userId) {
                            com.dakota.pos.ui.staff.StaffViewModel(
                                staffAttendanceRepository = app.staffAttendanceRepository,
                                recordStaffAttendanceUseCase = app.recordStaffAttendanceUseCase,
                                syncBiometricDeviceUseCase = app.syncBiometricDeviceUseCase,
                                calculateStaffPayrollUseCase = app.calculateStaffPayrollUseCase,
                                saveStaffPayrollUseCase = app.saveStaffPayrollUseCase,
                                generatePaySlipReceiptUseCase = app.generatePaySlipReceiptUseCase,
                                printer = app.printer
                            )
                        }

                        PosScreen(
                            viewModel = posViewModel,
                            session = session,
                            recipeViewModel = recipeViewModel,
                            inventoryViewModel = inventoryViewModel,
                            purchasingViewModel = purchasingViewModel,
                            shiftViewModel = shiftViewModel,
                            kdsViewModel = kdsViewModel,
                            financeViewModel = financeViewModel,
                            staffViewModel = staffViewModel,
                            onLogout = { authViewModel.logout() }
                        )
                    }
                }
            }
        }
    }
}

