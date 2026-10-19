# FASE 12: FINANCE + PROFIT REPORTING

## Deskripsi Task
Mengimplementasikan modul pembukuan laba-rugi (*Profit & Loss*), pencatatan biaya operasional (*expenses*), dan laporan keuangan analitis (Harian, Mingguan, Bulanan).

## Rumus Finansial Standar

$$\text{Net Sales} = \text{Gross Sales} - \text{Discount} - \text{Refund}$$

$$\text{COGS (HPP Terjual)} = \sum (\text{Qty Produk Terjual} \times \text{Recipe HPP})$$

$$\text{Gross Profit (Laba Kotor)} = \text{Net Sales} - \text{COGS}$$

$$\text{Net Profit (Laba Bersih)} = \text{Gross Profit} - \text{Operating Expense}$$

## Kategori Biaya Operasional (Expense Categories)
- `Rent` (Sewa tempat)
- `Electricity` (Listrik)
- `Water` (Air PDAM / Galon)
- `Internet` (Wifi / Telkom)
- `Salary` (Gaji & Insentif Karyawan)
- `Marketing` (Iklan, Promosi, Sosmed)
- `Maintenance` (Servis mesin espresso, grinder, AC)
- `Cleaning` (Sabun, sanitasi, pest control)
- `Other` (Biaya lain-lain)

## Fitur Laporan yang Diimplementasikan
- Filter Periode: **Harian (Daily)**, **Mingguan (Weekly)**, **Bulanan (Monthly)**, dan **Custom Date Range**
- Ringkasan Metrik:
  - Total Sales (Gross & Net)
  - Total COGS / HPP
  - Total Gross Profit & Margin %
  - Total Biaya Operasional (Breakdown per kategori)
  - Total Net Profit (Laba Bersih Akhir)
- Ekspor / Cetak Laporan Ringkas

## Scope Constraints (Dilarang Dikerjakan di Fase Ini)
- ❌ Mengubah flow POS kasir (kecuali perbaikan bug integrasi penting)
- ❌ Modul payroll otomatis / absensi fingerprint

## Deliverables
- Entity: OperatingExpense, ExpenseCategory
- FinancialAnalyticsEngine & UseCases
- Dashboard Laporan Keuangan (Cards, Grafik tren pendapatan & profit, Tabel breakdown)
- Unit Test verifikasi ketepatan formula Net Sales, COGS, Gross Profit, dan Net Profit
