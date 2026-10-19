package com.dakota.pos

import android.app.Application
import com.dakota.pos.core.database.DakotaDatabase
import com.dakota.pos.data.repository.AuthRepositoryImpl
import com.dakota.pos.data.repository.OrderRepositoryImpl
import com.dakota.pos.data.repository.ProductRepositoryImpl
import com.dakota.pos.domain.usecase.auth.CheckPermissionUseCase
import com.dakota.pos.domain.usecase.auth.GetActiveUsersUseCase
import com.dakota.pos.domain.usecase.auth.LoginWithPinUseCase
import com.dakota.pos.domain.usecase.auth.LogoutUseCase
import com.dakota.pos.domain.usecase.pos.CalculateCartUseCase
import com.dakota.pos.domain.usecase.pos.CheckoutOrderUseCase
import com.dakota.pos.domain.usecase.pos.GetCatalogUseCase

class DakotaApp : Application() {

    val database by lazy { DakotaDatabase.getInstance(this) }

    // Auth
    val authRepository by lazy {
        AuthRepositoryImpl(
            userDao = database.userDao(),
            auditLogDao = database.auditLogDao()
        )
    }

    // Product & Order
    val productRepository by lazy {
        ProductRepositoryImpl(
            categoryDao = database.categoryDao(),
            productDao = database.productDao()
        )
    }
    val orderRepository by lazy {
        OrderRepositoryImpl(orderDao = database.orderDao())
    }

    // Auth Use Cases
    val loginWithPinUseCase by lazy { LoginWithPinUseCase(authRepository) }
    val getActiveUsersUseCase by lazy { GetActiveUsersUseCase(authRepository) }
    val logoutUseCase by lazy { LogoutUseCase(authRepository) }
    val checkPermissionUseCase by lazy { CheckPermissionUseCase(authRepository) }

    // POS Use Cases
    val getCatalogUseCase by lazy { GetCatalogUseCase(productRepository) }
    val calculateCartUseCase by lazy { CalculateCartUseCase() }
    val checkoutOrderUseCase by lazy { CheckoutOrderUseCase(orderRepository) }

    // Hardware & Payment (Phase 6)
    val printer: com.dakota.pos.core.hardware.printer.PrinterInterface by lazy {
        com.dakota.pos.core.hardware.printer.MockPrinter()
    }
    val paymentProcessorRegistry by lazy {
        com.dakota.pos.domain.payment.PaymentProcessorRegistry()
    }
    val processPaymentUseCase by lazy {
        com.dakota.pos.domain.usecase.payment.ProcessPaymentUseCase(
            paymentProcessorRegistry = paymentProcessorRegistry,
            orderRepository = orderRepository
        )
    }
    val printReceiptUseCase by lazy {
        com.dakota.pos.domain.usecase.payment.PrintReceiptUseCase(
            printer = printer
        )
    }

    // Recipe & HPP Engine (Phase 7)
    val recipeRepository: com.dakota.pos.domain.repository.RecipeRepository by lazy {
        com.dakota.pos.data.repository.RecipeRepositoryImpl(
            ingredientDao = database.ingredientDao(),
            recipeDao = database.recipeDao()
        )
    }
    val calculateHppUseCase by lazy {
        com.dakota.pos.domain.usecase.recipe.CalculateHppUseCase(recipeRepository)
    }
    val getIngredientsUseCase by lazy {
        com.dakota.pos.domain.usecase.recipe.GetIngredientsUseCase(recipeRepository)
    }
    val saveIngredientUseCase by lazy {
        com.dakota.pos.domain.usecase.recipe.SaveIngredientUseCase(recipeRepository)
    }
    val saveRecipeUseCase by lazy {
        com.dakota.pos.domain.usecase.recipe.SaveRecipeUseCase(recipeRepository)
    }

    // Inventory Engine (Phase 8)
    val inventoryRepository: com.dakota.pos.domain.repository.InventoryRepository by lazy {
        com.dakota.pos.data.repository.InventoryRepositoryImpl(
            ingredientDao = database.ingredientDao(),
            stockMovementDao = database.stockMovementDao(),
            stockOpnameDao = database.stockOpnameDao()
        )
    }
    val stockDeductionEngine by lazy {
        com.dakota.pos.domain.inventory.IdempotentStockDeductionEngine(
            inventoryRepository = inventoryRepository,
            recipeRepository = recipeRepository
        )
    }
    val deductStockOnPaidUseCase by lazy {
        com.dakota.pos.domain.usecase.inventory.DeductStockOnPaidUseCase(stockDeductionEngine)
    }
    val returnStockOnCancelUseCase by lazy {
        com.dakota.pos.domain.usecase.inventory.ReturnStockOnCancelUseCase(stockDeductionEngine)
    }
    val manualStockMovementUseCase by lazy {
        com.dakota.pos.domain.usecase.inventory.ManualStockMovementUseCase(inventoryRepository)
    }
    val recordWastageUseCase by lazy {
        com.dakota.pos.domain.usecase.inventory.RecordWastageUseCase(inventoryRepository)
    }
    val performStockOpnameUseCase by lazy {
        com.dakota.pos.domain.usecase.inventory.PerformStockOpnameUseCase(inventoryRepository)
    }
    val getInventoryDataUseCase by lazy {
        com.dakota.pos.domain.usecase.inventory.GetInventoryDataUseCase(inventoryRepository)
    }

    // Purchasing & Supplier (Phase 9)
    val purchasingRepository: com.dakota.pos.domain.repository.PurchasingRepository by lazy {
        com.dakota.pos.data.repository.PurchasingRepositoryImpl(
            supplierDao = database.supplierDao(),
            purchaseOrderDao = database.purchaseOrderDao(),
            goodsReceiptDao = database.goodsReceiptDao()
        )
    }
    val updateIngredientCostUseCase by lazy {
        com.dakota.pos.domain.usecase.purchasing.UpdateIngredientCostUseCase(recipeRepository)
    }
    val createPOUseCase by lazy {
        com.dakota.pos.domain.usecase.purchasing.CreatePOUseCase(purchasingRepository)
    }
    val receiveGoodsUseCase by lazy {
        com.dakota.pos.domain.usecase.purchasing.ReceiveGoodsUseCase(
            purchasingRepository = purchasingRepository,
            inventoryRepository = inventoryRepository,
            recipeRepository = recipeRepository,
            updateIngredientCostUseCase = updateIngredientCostUseCase
        )
    }
    val getPurchasingDataUseCase by lazy {
        com.dakota.pos.domain.usecase.purchasing.GetPurchasingDataUseCase(
            purchasingRepository = purchasingRepository,
            recipeRepository = recipeRepository
        )
    }

    // Shift & Cash Management (Phase 10)
    val shiftRepository: com.dakota.pos.domain.repository.ShiftRepository by lazy {
        com.dakota.pos.data.repository.ShiftRepositoryImpl(
            cashierShiftDao = database.cashierShiftDao(),
            cashMovementDao = database.cashMovementDao(),
            orderDao = database.orderDao()
        )
    }
    val generateShiftReportUseCase by lazy {
        com.dakota.pos.domain.usecase.shift.GenerateShiftReportUseCase()
    }
    val openShiftUseCase by lazy {
        com.dakota.pos.domain.usecase.shift.OpenShiftUseCase(shiftRepository)
    }
    val recordCashMovementUseCase by lazy {
        com.dakota.pos.domain.usecase.shift.RecordCashMovementUseCase(shiftRepository)
    }
    val closeShiftUseCase by lazy {
        com.dakota.pos.domain.usecase.shift.CloseShiftUseCase(
            shiftRepository = shiftRepository,
            generateShiftReportUseCase = generateShiftReportUseCase
        )
    }
    val getCurrentShiftUseCase by lazy {
        com.dakota.pos.domain.usecase.shift.GetCurrentShiftUseCase(shiftRepository)
    }

    // KDS + Barista Printer (Phase 11)
    val printerManager by lazy {
        com.dakota.pos.core.hardware.printer.PrinterManager()
    }
    val kdsRepository: com.dakota.pos.domain.repository.KdsRepository by lazy {
        com.dakota.pos.data.repository.KdsRepositoryImpl(
            kitchenOrderDao = database.kitchenOrderDao(),
            stationConfigDao = database.stationConfigDao()
        )
    }
    val printStationTicketUseCase by lazy {
        com.dakota.pos.domain.usecase.kds.PrintStationTicketUseCase(printerManager)
    }
    val dispatchOrderToKitchenUseCase by lazy {
        com.dakota.pos.domain.usecase.kds.DispatchOrderToKitchenUseCase(
            kdsRepository = kdsRepository,
            productRepository = productRepository,
            printStationTicketUseCase = printStationTicketUseCase
        )
    }
    val updateKitchenOrderStatusUseCase by lazy {
        com.dakota.pos.domain.usecase.kds.UpdateKitchenOrderStatusUseCase(kdsRepository)
    }
    val getKdsOrdersUseCase by lazy {
        com.dakota.pos.domain.usecase.kds.GetKdsOrdersUseCase(kdsRepository)
    }
    val manageStationConfigUseCase by lazy {
        com.dakota.pos.domain.usecase.kds.ManageStationConfigUseCase(kdsRepository, printerManager)
    }

    // Finance + Profit Reporting (Phase 12)
    val financeRepository: com.dakota.pos.domain.repository.FinanceRepository by lazy {
        com.dakota.pos.data.repository.FinanceRepositoryImpl(
            operatingExpenseDao = database.operatingExpenseDao()
        )
    }
    val generateProfitLossReportUseCase by lazy {
        com.dakota.pos.domain.usecase.finance.GenerateProfitLossReportUseCase(
            orderRepository = orderRepository,
            recipeRepository = recipeRepository,
            financeRepository = financeRepository
        )
    }
    val recordOperatingExpenseUseCase by lazy {
        com.dakota.pos.domain.usecase.finance.RecordOperatingExpenseUseCase(financeRepository)
    }
    val deleteOperatingExpenseUseCase by lazy {
        com.dakota.pos.domain.usecase.finance.DeleteOperatingExpenseUseCase(financeRepository)
    }
    val getOperatingExpensesUseCase by lazy {
        com.dakota.pos.domain.usecase.finance.GetOperatingExpensesUseCase(financeRepository)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
