# FASE 7: RECIPE + HPP ENGINE

## Deskripsi Task
Mengimplementasikan domain resep produk, unit pengukuran, konversi unit, dan kalkulasi otomatis Harga Pokok Penjualan (HPP / COGS).

## Prinsip Penting
- **Product TIDAK SAMA DENGAN Ingredient.**
- **Recipe menghubungkan Product dengan satu atau lebih Ingredient.**
- **HPP dihitung otomatis dari Recipe + Ingredient Cost, BUKAN di-hardcode.**

## Fitur yang Diimplementasikan
- **Ingredient**: Bahan baku mentah (nama, sku, unit dasar, cost per unit dasar)
- **Unit & Conversion**:
  - Gram (`g`)
  - Kilogram (`kg`) [1 kg = 1000 g]
  - Mililiter (`ml`)
  - Liter (`L`) [1 L = 1000 ml]
  - Pieces (`pcs`)
- **Recipe & RecipeItem**: Relasi produk/varian ke komposisi bahan baku
- **IngredientCost**: Riwayat atau current cost dari setiap ingredient

## Rumus HPP (Cost of Goods Sold per Product)
$$\text{HPP Product} = \sum (\text{Recipe Quantity} \times \text{Ingredient Unit Cost})$$

### Contoh Kasus Nyata (Es Kopi Susu):
- Biji Kopi (Coffee Bean): 18 g (Harga Rp 250.000 / kg → Rp 250 / g → Rp 4.500)
- Susu Segar (Fresh Milk): 100 ml (Harga Rp 20.000 / L → Rp 20 / ml → Rp 2.000)
- Gula Aren / Syrup: 15 ml (Harga Rp 50.000 / L → Rp 50 / ml → Rp 750)
- Es Batu (Ice): 150 g (Rp 200)
- Cup Gelas Plastik: 1 pcs (Rp 500)
- Tutup Lid: 1 pcs (Rp 150)
- Straw / Sedotan: 1 pcs (Rp 100)
**Total HPP Es Kopi Susu = Rp 8.200**

## Scope Constraints (Dilarang Dikerjakan di Fase Ini)
- ❌ Pengurangan stok inventory (Stok belum boleh berkurang di fase ini)
- ❌ Purchasing UI
- ❌ Mengubah alur Payment

## Deliverables
- Entity: Ingredient, Recipe, RecipeItem, UnitType
- Engine Konversi Satuan (Weight & Volume converter)
- HppCalculator Engine & UseCase
- UI Manajemen Bahan Baku & Pengaturan Resep Produk
- **Wajib:** Unit Test komprehensif untuk konversi satuan dan akurasi kalkulasi HPP
