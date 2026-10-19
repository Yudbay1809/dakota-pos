package com.dakota.pos

import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.model.recipe.Recipe
import com.dakota.pos.domain.model.recipe.RecipeItem
import com.dakota.pos.domain.model.unit.UnitDimension
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.domain.recipe.HppCalculator
import com.dakota.pos.domain.recipe.IncompatibleUnitException
import com.dakota.pos.domain.recipe.UnitConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConversionAndHppTest {

    // ========================================================================
    // 1. UNIT CONVERSION ENGINE TESTS
    // ========================================================================

    @Test
    fun `test weight unit conversions between grams and kilograms`() {
        // 1 kg = 1000 g
        assertEquals(1000.0, UnitConverter.convert(1.0, UnitType.KG, UnitType.G), 0.001)
        assertEquals(2500.0, UnitConverter.convert(2.5, UnitType.KG, UnitType.G), 0.001)

        // 500 g = 0.5 kg
        assertEquals(0.5, UnitConverter.convert(500.0, UnitType.G, UnitType.KG), 0.001)
        assertEquals(0.018, UnitConverter.convert(18.0, UnitType.G, UnitType.KG), 0.0001)
    }

    @Test
    fun `test volume unit conversions between milliliters and liters`() {
        // 1 L = 1000 ml
        assertEquals(1000.0, UnitConverter.convert(1.0, UnitType.L, UnitType.ML), 0.001)
        assertEquals(100.0, UnitConverter.convert(0.1, UnitType.L, UnitType.ML), 0.001)

        // 250 ml = 0.25 L
        assertEquals(0.25, UnitConverter.convert(250.0, UnitType.ML, UnitType.L), 0.001)
        assertEquals(0.015, UnitConverter.convert(15.0, UnitType.ML, UnitType.L), 0.0001)
    }

    @Test
    fun `test discrete unit conversion for pieces`() {
        assertEquals(1.0, UnitConverter.convert(1.0, UnitType.PCS, UnitType.PCS), 0.001)
        assertEquals(5.0, UnitConverter.convert(5.0, UnitType.PCS, UnitType.PCS), 0.001)
    }

    @Test
    fun `test cross-dimensional conversion throws IncompatibleUnitException`() {
        // Berat ke Volume tidak bisa tanpa densitas
        assertFalse(UnitConverter.areCompatible(UnitType.G, UnitType.ML))
        assertThrows(IncompatibleUnitException::class.java) {
            UnitConverter.convert(100.0, UnitType.G, UnitType.ML)
        }

        // Discrete ke Berat tidak bisa
        assertFalse(UnitConverter.areCompatible(UnitType.PCS, UnitType.KG))
        assertThrows(IncompatibleUnitException::class.java) {
            UnitConverter.convert(1.0, UnitType.PCS, UnitType.KG)
        }
    }

    @Test
    fun `test unit cost conversion calculates proportional cost per target unit`() {
        // Biji kopi Rp 250.000 / kg -> untuk unit 'g' = Rp 250 / g
        val costPerGram = UnitConverter.convertCost(250000.0, UnitType.KG, UnitType.G)
        assertEquals(250.0, costPerGram, 0.001)

        // Susu Rp 20.000 / L -> untuk unit 'ml' = Rp 20 / ml
        val costPerMl = UnitConverter.convertCost(20000.0, UnitType.L, UnitType.ML)
        assertEquals(20.0, costPerMl, 0.001)

        // Sirup Gula Aren Rp 50.000 / L -> untuk unit 'ml' = Rp 50 / ml
        val syrupCostPerMl = UnitConverter.convertCost(50000.0, UnitType.L, UnitType.ML)
        assertEquals(50.0, syrupCostPerMl, 0.001)
    }

    // ========================================================================
    // 2. HPP CALCULATION REAL CASE VERIFICATION (ES KOPI SUSU PER SPEC)
    // ========================================================================

    @Test
    fun `test exact COGS and margin calculation for Es Kopi Susu per Phase 7 specification`() {
        // Komponen Bahan Baku:
        // 1. Biji Kopi: Rp 250.000 / kg
        // 2. Susu Segar: Rp 20.000 / L
        // 3. Gula Aren: Rp 50.000 / L
        // 4. Es Batu: Rp 200 per 150g -> Rp 1.33333333 / g
        // 5. Cup Plastik: Rp 500 / pcs
        // 6. Tutup Lid: Rp 150 / pcs
        // 7. Sedotan: Rp 100 / pcs
        val ingredients = mapOf(
            "ing-beans" to Ingredient("ing-beans", "Coffee Bean", "SKU-1", "Coffee", UnitType.KG, 250000.0),
            "ing-milk" to Ingredient("ing-milk", "Fresh Milk", "SKU-2", "Dairy", UnitType.L, 20000.0),
            "ing-aren" to Ingredient("ing-aren", "Gula Aren", "SKU-3", "Syrup", UnitType.L, 50000.0),
            "ing-ice" to Ingredient("ing-ice", "Es Batu", "SKU-4", "Ice", UnitType.G, 200.0 / 150.0),
            "ing-cup" to Ingredient("ing-cup", "Cup Plastik", "SKU-5", "Packaging", UnitType.PCS, 500.0),
            "ing-lid" to Ingredient("ing-lid", "Tutup Lid", "SKU-6", "Packaging", UnitType.PCS, 150.0),
            "ing-straw" to Ingredient("ing-straw", "Straw Sedotan", "SKU-7", "Packaging", UnitType.PCS, 100.0)
        )

        // Resep Es Kopi Susu:
        // 18g Kopi + 100ml Susu + 15ml Aren + 150g Es + 1 Cup + 1 Lid + 1 Straw
        val recipe = Recipe(
            id = "rcp-kopi-susu",
            productId = "prd-kopi-susu",
            productName = "Es Kopi Susu Dakota",
            items = listOf(
                RecipeItem("1", "rcp-kopi-susu", "ing-beans", "Coffee Bean", 18.0, UnitType.G),
                RecipeItem("2", "rcp-kopi-susu", "ing-milk", "Fresh Milk", 100.0, UnitType.ML),
                RecipeItem("3", "rcp-kopi-susu", "ing-aren", "Gula Aren", 15.0, UnitType.ML),
                RecipeItem("4", "rcp-kopi-susu", "ing-ice", "Es Batu", 150.0, UnitType.G),
                RecipeItem("5", "rcp-kopi-susu", "ing-cup", "Cup Plastik", 1.0, UnitType.PCS),
                RecipeItem("6", "rcp-kopi-susu", "ing-lid", "Tutup Lid", 1.0, UnitType.PCS),
                RecipeItem("7", "rcp-kopi-susu", "ing-straw", "Straw Sedotan", 1.0, UnitType.PCS)
            )
        )

        val sellingPrice = 22000.0

        val hppBreakdown = HppCalculator.calculate(
            recipe = recipe,
            ingredientsMap = ingredients,
            sellingPrice = sellingPrice
        )

        // Verifikasi Breakdown per Item:
        // 1. Biji Kopi: 18g x Rp 250 = Rp 4.500
        assertEquals(4500.0, hppBreakdown.itemBreakdowns[0].subtotalHpp, 0.01)
        // 2. Susu Segar: 100ml x Rp 20 = Rp 2.000
        assertEquals(2000.0, hppBreakdown.itemBreakdowns[1].subtotalHpp, 0.01)
        // 3. Gula Aren: 15ml x Rp 50 = Rp 750
        assertEquals(750.0, hppBreakdown.itemBreakdowns[2].subtotalHpp, 0.01)
        // 4. Es Batu: 150g x (200/150) = Rp 200
        assertEquals(200.0, hppBreakdown.itemBreakdowns[3].subtotalHpp, 0.01)
        // 5. Cup: 1 pcs x Rp 500 = Rp 500
        assertEquals(500.0, hppBreakdown.itemBreakdowns[4].subtotalHpp, 0.01)
        // 6. Lid: 1 pcs x Rp 150 = Rp 150
        assertEquals(150.0, hppBreakdown.itemBreakdowns[5].subtotalHpp, 0.01)
        // 7. Straw: 1 pcs x Rp 100 = Rp 100
        assertEquals(100.0, hppBreakdown.itemBreakdowns[6].subtotalHpp, 0.01)

        // Total HPP Es Kopi Susu = Rp 8.200 (Persis sesuai rumus spesifikasi dokumen fase 7)
        assertEquals(8200.0, hppBreakdown.totalHpp, 0.01)

        // Laba Kotor = 22.000 - 8.200 = 13.800
        assertEquals(13800.0, hppBreakdown.grossProfit, 0.01)

        // Margin Persentase = (13.800 / 22.000) * 100% = 62.727%
        val expectedMargin = (13800.0 / 22000.0) * 100.0
        assertEquals(expectedMargin, hppBreakdown.grossMarginPercentage, 0.01)
    }

    @Test
    fun `test Americano recipe HPP calculation with minimal items`() {
        val ingredients = mapOf(
            "ing-beans" to Ingredient("ing-beans", "Coffee Bean", "SKU-1", "Coffee", UnitType.KG, 250000.0),
            "ing-cup" to Ingredient("ing-cup", "Hot Cup 8oz", "SKU-CUP", "Packaging", UnitType.PCS, 600.0),
            "ing-lid" to Ingredient("ing-lid", "Hot Lid", "SKU-LID", "Packaging", UnitType.PCS, 200.0)
        )

        val recipe = Recipe(
            id = "rcp-americano",
            productId = "prd-americano",
            productName = "Americano Hot",
            items = listOf(
                RecipeItem("1", "rcp-americano", "ing-beans", "Coffee Bean", 18.0, UnitType.G),
                RecipeItem("2", "rcp-americano", "ing-cup", "Hot Cup 8oz", 1.0, UnitType.PCS),
                RecipeItem("3", "rcp-americano", "ing-lid", "Hot Lid", 1.0, UnitType.PCS)
            )
        )

        val breakdown = HppCalculator.calculate(recipe, ingredients, 18000.0)

        // HPP = (18g * 250) + 600 + 200 = 4500 + 800 = 5300
        assertEquals(5300.0, breakdown.totalHpp, 0.01)
        assertEquals(12700.0, breakdown.grossProfit, 0.01)
    }
}
