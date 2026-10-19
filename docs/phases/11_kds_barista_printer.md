# FASE 11: KDS + PRINTER BARISTA

## Deskripsi Task
Mengimplementasikan Kitchen Display System (KDS) dan perutean tiket pesanan ke printer stasiun kerja (Bar / Kitchen / Dessert).

## Alur Pemrosesan Pesanan Dapur / Bar:
```
PAID ORDER
  → Kitchen Order Dispatcher
  → Category-Based Routing (misal: Coffee → Bar Station; Food → Kitchen Station)
  → Antrean KDS Stasiun
  → Lifecycle Status:
      NEW → ACCEPTED → PREPARING → READY → SERVED
  → Cetak Tiket Stasiun (Opsional otomatis / manual)
```

## Fitur yang Diimplementasikan
- **KDS (Kitchen Display System) Interface**:
  - Tampilan grid kartu pesanan dengan indikator timer durasi tunggu (menit berlalu)
  - Status perubahan tombol satu klik (Accept, Ready, Done)
  - Filter stasiun: Barista (Minuman), Kitchen (Makanan), atau All
- **Station & Routing**:
  - Konfigurasi kategori menu ke ID stasiun kerja
- **Barista / Kitchen Printer Abstraction**:
  - Protokol ESC/POS
  - Interface untuk Bluetooth ESC/POS printer
  - Interface untuk Network / LAN (Ethernet/WiFi) ESC/POS printer
  - **Mock Printer Implementation** untuk pengujian tanpa perangkat fisik

## Scope Constraints (Dilarang Dikerjakan di Fase Ini)
- ❌ Integrasi Payment Gateway / QRIS
- ❌ Perubahan besar pada UI Kasir / Catalog

## Deliverables
- Entity: KitchenOrder, KitchenOrderItem, StationConfig
- Dispatcher Service & State Machine status pesanan dapur
- Layar KDS untuk tablet barista di area bar / dapur
- Printer abstraction adapter (Mock, Bluetooth, Network)
- Unit Test state transition pesanan dan routing berdasarkan kategori
