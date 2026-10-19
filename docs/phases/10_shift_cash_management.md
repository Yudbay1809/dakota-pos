# FASE 10: SHIFT + CASH MANAGEMENT

## Deskripsi Task
Mengimplementasikan manajemen shift kasir dan rekonsiliasi laci kasir (*cash drawer*).

## Siklus Shift
```
OPEN SHIFT → ACTIVE SHIFT (Transaksi & Mutasi Kas) → CLOSE SHIFT (Cash Count & Rekonsiliasi)
```

## Fitur yang Diimplementasikan
- **Open Shift**: Input modal awal kasir (`Opening Cash`) oleh kasir yang bertugas
- **Active Shift Tracking**:
  - Penjualan Tunai (`Cash Sales`)
  - Kas Masuk Non-Penjualan (`Cash In` / setoran modal tambahan)
  - Kas Keluar Operasional Kecil (`Cash Out` / `Petty Cash Expense`, misal: beli es batu darurat)
  - Pengembalian Tunai (`Cash Refund`)
- **Close Shift & Rekonsiliasi**:
  - Kasir menghitung uang fisik di laci (`Actual Cash`) tanpa diberitahu angka ekspektasi sistem terlebih dahulu untuk mencegah manipulasi
  - Sistem menghitung `Expected Cash` dan `Variance` (Selisih)
  - Pencatatan catatan/alasan jika terjadi selisih kas
- **Audit Trail Shift**: Log riwayat shift per kasir, jam buka, jam tutup, dan ringkasan finansial shift

## Rumus Rekonsiliasi Kas:
$$\text{Expected Cash} = \text{Opening Cash} + \text{Cash Sales} + \text{Cash In} - \text{Cash Out} - \text{Cash Refund}$$

$$\text{Variance (Selisih)} = \text{Actual Cash} - \text{Expected Cash}$$

*(Jika Variance > 0 = Kas Lebih / Over; Jika Variance < 0 = Kas Kurang / Short)*

## Scope Constraints (Dilarang Dikerjakan di Fase Ini)
- ❌ Inventory & Recipe
- ❌ HPP Engine
- ❌ Payroll / Penggajian Karyawan
- ❌ Purchasing & Supplier

## Deliverables
- Entity: CashierShift, CashMovement (CASH_IN, CASH_OUT, EXPENSE, REFUND)
- ShiftRepository & UseCases (OpenShiftUseCase, RecordCashMovementUseCase, CloseShiftUseCase)
- Layar Buka Shift, Catat Kas Masuk/Keluar, dan Tutup Shift
- Struk Ringkasan Shift (X-Report / Z-Report)
- **Wajib Unit Tests**: Formula perhitungan Expected Cash, Variance, dan status penutupan shift
