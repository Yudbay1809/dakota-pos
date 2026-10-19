# FASE 5: PRODUCT + POS

## Deskripsi Task
Mengimplementasikan katalog produk dan antarmuka transaksi kasir (POS/Cart).

## Fitur yang Diimplementasikan
- **Category** (Contoh: `Coffee`, `Non-Coffee`, `Food`, `Dessert`)
- **Product** (Nama, deskripsi, harga dasar, gambar/warna, status aktif)
- **Variant** (Contoh: Hot / Cold, Size: Regular / Large)
- **Modifier** (Contoh: Extra Espresso Shot, Oat Milk, Less Sugar, Normal Ice)
- **Cart Management** (Tambah item, ubah qty, ganti varian/modifier, hapus item)
- **Order Lifecycle**:
  ```
  DRAFT → CONFIRMED → PAID → COMPLETED
  ```
- **Order & Order Item Entities**
- **Discount & Total Calculation** (Diskon item, diskon order, subtotal, tax/pajak jika ada)
- **Customer Notes** (Catatan per item atau catatan pesanan)

## Scope Constraints (Dilarang Dikerjakan di Fase Ini)
- ❌ HPP Engine
- ❌ Recipe Definition
- ❌ Inventory Deduction (Pengurangan stok saat order)
- ❌ Purchasing
- ❌ QRIS Provider Implementation
- ❌ Printer Hardware Integration
- ❌ KDS (Kitchen Display System)

## Deliverables
- Database tables: Category, Product, ProductVariant, Modifier, Order, OrderItem
- POS Screen: Grid/List katalog produk, filter kategori, dialog opsi varian/modifier
- Cart Drawer / Panel: Review pesanan, kalkulasi diskon, subtotal, total akhir
- Unit Test untuk kalkulasi harga item ber-modifier dan total order
