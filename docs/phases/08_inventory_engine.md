# FASE 8: INVENTORY ENGINE

## Deskripsi Task
Mengimplementasikan pelacakan stok bahan baku, mutasi stok, pengurangan stok otomatis saat order dibayar (*idempotent deduction*), penyesuaian stok (*adjustment*), dan pencatatan sisa / terbuang (*wastage*).

## Prinsip Penting
- Inventory berkurang pada tingkat **Ingredient**, berdasarkan **Recipe** dari produk/varian yang terjual.
- Pengurangan stok harus **IDEMPOTENT**: Transaksi yang sama tidak boleh memotong stok dua kali jika terjadi sinkronisasi ulang atau retry.

## Alur Pengurangan Stok (Flow):
```
PAID ORDER 
  → Ambil Daftar Produk & Varian
  → Ambil Recipe Terkait
  → Breakdown ke Recipe Items & Quantities
  → Buat Record Inventory Movement (Type: SALE)
  → Potong Stock Saldo Ingredient
```

## Fitur yang Diimplementasikan
- **Ingredient Stock**: Saldo stok saat ini untuk setiap bahan baku
- **Stock Movement Ledger**: Log audit keluar masuk stok (IN, OUT, SALE, ADJUSTMENT, WASTAGE, RETURN)
- **Stock In & Stock Out manual**
- **Adjustment & Wastage**: Pencatatan bahan tumpah, basi, kalibrasi espresso grinder
- **Stock Opname**: Pencocokan stok fisik vs stok sistem
- **Minimum Stock & Reorder Level**: Indikator peringatan jika stok menipis

## Scope Constraints (Dilarang Dikerjakan di Fase Ini)
- ❌ Purchasing UI & Purchase Order Module
- ❌ Mengubah Payment UI kecuali integrasi minimal listener order paid

## Deliverables
- Entity: StockMovement, StockAdjustment, StockOpname
- IdempotentStockDeductionEngine
- Layar Monitoring Stok, Log Mutasi, dan Form Stock Opname / Wastage
- **Wajib Unit Tests**:
  - Normal deduction saat transaksi sukses
  - Insufficient stock handling (stok minus / pencegahan)
  - Duplicate transaction idempotency (mencegah double cut)
  - Cancellation / refund handling (pengembalian stok)
  - Manual stock adjustment & opname
