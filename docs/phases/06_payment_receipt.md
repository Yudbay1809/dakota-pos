# FASE 6: PAYMENT + RECEIPT

## Deskripsi Task
Mengimplementasikan modul pembayaran multichannel, kalkulasi kembalian, format struk, serta interface printer.

## Fitur yang Diimplementasikan
- **Metode Pembayaran**:
  - Cash (Tunai)
  - QRIS (Interface / Abstraction Provider agar vendor mudah diganti)
  - Debit Card
  - Credit Card
  - Bank Transfer
  - E-wallet
- **Kalkulasi Cash**:
  - Input `Cash Received` (Uang diterima, dengan quick buttons: Uang Pas, 20k, 50k, 100k)
  - `Change Calculation` (Kembalian = Cash Received - Total Bill)
- **Komponen Struk Minimal (Receipt Specs)**:
  1. Nama Outlet / Coffee Shop
  2. Order Number / Invoice ID
  3. Tanggal & Waktu Transaksi
  4. Nama Kasir yang bertugas
  5. Detail Item: Nama produk, varian/modifier, Qty, Harga satuan, Subtotal item
  6. Diskon (jika ada)
  7. Total Pembayaran
  8. Metode Pembayaran & Detail Bayar (Cash + Kembalian / Ref Number)
  9. Footer Struk (Pesan terima kasih, media sosial)
- **Printer Abstraction**:
  - Interface printer terisolasi dari business logic (`ReceiptPrinterInterface`)
  - Mock Printer implementation untuk development & testing

## Scope Constraints (Dilarang Dikerjakan di Fase Ini)
- ❌ Barista Printer / Kitchen Printer Routing
- ❌ KDS (Kitchen Display System)
- ❌ Direct hardware driver binding yang menembus business logic

## Deliverables
- Payment Processor & Abstraction Layer
- Layar Pembayaran (Dialog pemilihan metode pembayaran, input nominal, kembalian)
- Generator Format Struk (Teks / ESC-POS byte generator / Print Preview)
- Unit Test kalkulasi kembalian dan validasi format struk
