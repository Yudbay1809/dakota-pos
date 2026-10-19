# FASE 9: PURCHASING + SUPPLIER

## Deskripsi Task
Mengimplementasikan pengadaan bahan baku, manajemen pemasok (*supplier*), pembuatan *Purchase Order (PO)*, dan pencatatan penerimaan barang (*Goods Receiving*) yang otomatis menambah stok dan memperbarui harga pokok bahan baku.

## Lifecycle Purchase Order
```
DRAFT → SUBMITTED → RECEIVED → COMPLETED
```

## Fitur yang Diimplementasikan
- **Supplier**: Data nama supplier, kontak, alamat, termin pembayaran
- **Purchase Order (PO)**: Nomor PO, tanggal pesan, estimasi kedatangan, status
- **Purchase Item**: Bahan baku yang dipesan, kuantitas, harga beli per unit
- **Goods Receiving**:
  - Validasi fisik barang datang vs PO
  - Mendukung penerimaan bertahap (*partial receiving*)
  - Otomatis membuat mutasi stok masuk (`IN / PURCHASE`)
  - Memperbarui `Ingredient Unit Cost` (metode FIFO atau Moving Average sesuai spesifikasi yang ditentukan)

## Scope Constraints (Dilarang Dikerjakan di Fase Ini)
- ❌ Mengubah modul POS
- ❌ Mengubah modul Payment & QRIS
- ❌ KDS & Barista Printer
- ❌ Finance Dashboard umum

## Deliverables
- Entity: Supplier, PurchaseOrder, PurchaseOrderItem, GoodsReceipt
- UseCase: CreatePOUseCase, ReceiveGoodsUseCase, UpdateIngredientCostUseCase
- Layar Manajemen Supplier, List PO, dan Form Penerimaan Barang
- Unit Test alur pembuatan PO, partial receiving, dan update stock & cost
