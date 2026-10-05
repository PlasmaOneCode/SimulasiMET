# Cloud Task Scheduling dengan Minimum Execution Time (MET)

Simulasi **Cloud Task Scheduling** menggunakan algoritma heuristic **Minimum Execution Time (MET)** pada **CloudSim 7.0.1**.

Project ini dibuat untuk tugas mata kuliah **Strategi Optimasi Komputasi Awan**, Kelompok 7, Departemen Teknologi Informasi, Institut Teknologi Sepuluh Nopember Surabaya.

Dokumen ini mencakup dua tahap eksperimen:

1. **Baseline (Week 4):** satu run GoCJ 1000 task.
2. **Revisi:** GoCJ 100 sampai 1000 task dan Synthetic 1000 sampai 10000 task, masing-masing 3 run, dirata-ratakan (total 60 run).

## 1. Ringkasan Konfigurasi

| Komponen | Konfigurasi |
|---|---|
| Simulator | CloudSim 7.0.1 |
| Java | OpenJDK 21 LTS (Temurin) |
| Build tool | Apache Maven 3.9.x |
| Datacenter | 1 |
| Host | 10 (5 Class A, 5 Class B) |
| Host Class A | 4 core, 2000 MIPS/core, 8 GB RAM, 1 TB storage |
| Host Class B | 8 core, 3000 MIPS/core, 16 GB RAM, 2 TB storage |
| VM | 20 VM heterogen (4 tipe, masing-masing 5 VM) |
| VM Type 1 (VM 0-4) | 1 core, 1000 MIPS, 2 GB RAM, 20 GB storage |
| VM Type 2 (VM 5-9) | 2 core, 1500 MIPS, 2 GB RAM, 20 GB storage |
| VM Type 3 (VM 10-14) | 2 core, 2000 MIPS, 4 GB RAM, 30 GB storage |
| VM Type 4 (VM 15-19) | 4 core, 2500 MIPS, 4 GB RAM, 40 GB storage |
| Scheduler VM | `CloudletSchedulerSpaceShared` |
| Scheduler Host | `VmSchedulerTimeShared` |
| Algoritma | Minimum Execution Time (MET) |

Catatan: pada allocation log, VM 0-19 ditempatkan pada Host #5 sampai #9 (Class B). Bandwidth pada kode (Host 1.000.000, VM 10.000) adalah satuan internal CloudSim, bukan Gbps literal.

## 2. Cara Kerja MET

Untuk setiap cloudlet, MET menghitung estimated execution time terhadap seluruh VM:

```text
ET(i,j) = CloudletLength(i) / VM_MIPS(j)
```

VM dengan ET paling kecil dipilih. MET dasar tidak memakai current load maupun queue length. Jika beberapa VM memiliki ET minimum yang sama, VM pertama yang ditemukan menang (operator `<` bersifat strict).

Akibatnya VM 15 sampai VM 19 (sama-sama 2500 MIPS) memiliki ET yang sama, dan VM 15 selalu terpilih untuk **seluruh** cloudlet. Ini konsekuensi MET dasar, bukan bug. Project ini sengaja tidak menambahkan load-aware tie-breaking karena akan mengubah metode yang diuji.

## 3. Perubahan Source pada Revisi

Perubahan pada `Week4_MET_TaskScheduling.java`:

- `MAX_CLOUDLETS` dari 1000 menjadi 10000.
- `printCloudletResults()` hanya menampilkan 20 cloudlet sukses pertama. Metrics tetap dihitung dari seluruh hasil.

Tidak diubah: rumus MET, loop assignment, tie-breaking, konfigurasi VM dan Host, scheduler.

## 4. Struktur Repository

```text
SimulasiMET/
├── README.md
├── pom.xml
├── run-experiments.ps1            # otomatisasi 60 run
├── average-results.py             # validasi dan rata-rata hasil
├── generate-synthetic.py          # pembangkit dataset synthetic (seed 42)
├── dataset/
│   ├── GoCJ_Dataset_100.txt ... GoCJ_Dataset_1000.txt
│   └── synthetic/
│       └── Synthetic_1000.txt ... Synthetic_10000.txt
├── modules/
│   ├── cloudsim/
│   └── cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/
│       └── Week4_MET_TaskScheduling.java
└── results/
    ├── raw/                       # log mentah tiap run
    ├── experiment-results.csv     # hasil mentah 60 run
    ├── experiment-results-clean.csv
    ├── experiment-averages.csv    # rata-rata 20 dataset
    └── MET_Experiment_Results.xlsx  # Raw, Average, dan 10 grafik
```

## 5. Persyaratan

- JDK 21 (cek dengan `java -version`).
- Apache Maven 3.9.x (cek dengan `mvn -version`), harus memakai JDK 21.
- Python 3 (untuk `average-results.py` dan `generate-synthetic.py`).
- Windows PowerShell (untuk `run-experiments.ps1`).

Semua perintah dijalankan dari **root repository** (folder yang berisi `pom.xml`).

## 6. Build dan Uji Instalasi

```powershell
mvn clean install
mvn exec:java -pl modules/cloudsim-examples/ -Dexec.mainClass=org.cloudbus.cloudsim.examples.CloudSimExample1
```

## 7. Menjalankan Satu Simulasi

Default (GoCJ 1000):

```powershell
mvn exec:java -pl modules/cloudsim-examples/ -Dexec.mainClass=org.cloudbus.cloudsim.examples.Week4_MET_TaskScheduling
```

Dataset tertentu lewat argumen path:

```powershell
mvn exec:java -pl modules/cloudsim-examples/ -Dexec.mainClass=org.cloudbus.cloudsim.examples.Week4_MET_TaskScheduling -Dexec.args="dataset/GoCJ_Dataset_100.txt"
mvn exec:java -pl modules/cloudsim-examples/ -Dexec.mainClass=org.cloudbus.cloudsim.examples.Week4_MET_TaskScheduling -Dexec.args="dataset/synthetic/Synthetic_10000.txt"
```

## 8. Mereproduksi 60 Run

1. (Opsional) Buat ulang dataset synthetic. Hasilnya identik dengan file di repo karena seed tetap:

   ```powershell
   python generate-synthetic.py
   ```

2. Jalankan seluruh eksperimen (GoCJ 100-1000 x3 dan Synthetic 1000-10000 x3):

   ```powershell
   powershell -ExecutionPolicy Bypass -File run-experiments.ps1
   ```

   Log mentah masuk ke `results\raw\`, ringkasan masuk ke `results\experiment-results.csv`.

   Penting: skrip memanggil Maven lewat `cmd /c <satu string>`. Jangan diubah menjadi `& mvn <argumen terpisah>`, karena PowerShell akan memotong argumen `-Dexec.mainClass=...` dan Maven gagal dengan `Unknown lifecycle phase`.

   Jika `results\experiment-results.csv` sudah ada, skrip dapat menambahkan baris baru di bawahnya. Pindahkan atau hapus file lama sebelum run ulang agar tidak bercampur.

3. Validasi dan hitung rata-rata:

   ```powershell
   python average-results.py
   ```

   Skrip membuang baris `BUILD_FAILED`, mengecek Completed Cloudlets sama dengan jumlah task, mengecek tiga run per dataset, lalu menulis `results/experiment-results-clean.csv` dan `results/experiment-averages.csv`.

4. Grafik dibuat di Google Sheets (bukan di kode): unggah `results/MET_Experiment_Results.xlsx` ke Google Drive lalu buka dengan Google Sheets.

## 9. Dataset

### GoCJ (Google Cloud Jobs Dataset)

File `GoCJ_Dataset_100.txt` sampai `GoCJ_Dataset_1000.txt` adalah **file resmi terpisah** dari Mendeley Data (Hussain dan Aleem, 2018), bukan potongan satu file besar. Tiap file adalah sampel independen, sehingga statistiknya berbeda antar file. GoCJ berisi ukuran job dalam Million Instructions (MI) yang dibangkitkan berdasarkan karakteristik workload Google cluster traces, bukan raw trace.

GoCJ_Dataset_1000: min 15.000, max 900.000, mean 129.662, median 93.000 MI.

### Synthetic

Dibangkitkan oleh `generate-synthetic.py` (Python `random`, seed 42, tiap file memakai `random.Random(42)` sendiri):

| Kategori | Proporsi | Rentang (MI) |
|---|---:|---|
| Small | 30% | 15.000 - 50.000 |
| Medium | 40% | 50.001 - 150.000 |
| Large | 20% | 150.001 - 500.000 |
| Very Large | 10% | 500.001 - 900.000 |

Jumlah Small, Medium, dan Large adalah `round(N x proporsi)`, Very Large menyerap sisa pembulatan, lalu seluruh nilai diacak urutannya. Run 1, 2, dan 3 memakai file yang sama.

Mean synthetic sekitar 185.000 MI (184.085 sampai 186.363), lebih tinggi daripada GoCJ_1000 (129.662) karena proporsi Medium dan Large pada synthetic lebih besar daripada distribusi alami GoCJ. Ini disengaja dan perlu diingat saat membandingkan kedua keluarga dataset.

## 10. Hasil

### Baseline (GoCJ 1000, eksperimen awal, dipertahankan sebagai catatan historis)

| Metrik | Hasil |
|---|---:|
| Completed Cloudlets | 1000 |
| Makespan | 22779.81 s |
| Degree of Imbalance | 20 |
| Average Waiting Time | 8748.2664 s |
| Throughput | 0.0439 cloudlets/s |
| Average VM Utilization | 11.3839% |

Assignment: VM 15 menerima 1000 cloudlet, VM lainnya 0. Hasil ini identik dengan GoCJ_1000 pada revisi.

### Revisi: rata-rata 3 run, GoCJ

| Dataset | Task | Makespan (s) | Avg Waiting Time (s) | Throughput (cloudlet/s) | Degree of Imbalance | Avg VM Utilization (%) |
|---|---:|---:|---:|---:|---:|---:|
| GoCJ_100 | 100 | 2703.81 | 1199.316 | 0.037 | 20 | 9.9929 |
| GoCJ_200 | 200 | 2819.61 | 1415.346 | 0.0709 | 20 | 19.3073 |
| GoCJ_300 | 300 | 7612.21 | 3653.2327 | 0.0394 | 20 | 10.9104 |
| GoCJ_400 | 400 | 18035.41 | 7746.8145 | 0.0222 | 20 | 5.752 |
| GoCJ_500 | 500 | 6573.61 | 3204.81 | 0.0761 | 20 | 19.7765 |
| GoCJ_600 | 600 | 15021.41 | 6734.208 | 0.0399 | 20 | 10.9632 |
| GoCJ_700 | 700 | 11927.01 | 5294.5951 | 0.0587 | 20 | 14.6715 |
| GoCJ_800 | 800 | 27485.21 | 11212.0375 | 0.0291 | 20 | 7.2008 |
| GoCJ_900 | 900 | 32757.81 | 12056.4467 | 0.0275 | 20 | 7.3294 |
| GoCJ_1000 | 1000 | 22779.81 | 8748.2664 | 0.0439 | 20 | 11.3839 |

### Revisi: rata-rata 3 run, Synthetic

| Dataset | Task | Makespan (s) | Avg Waiting Time (s) | Throughput (cloudlet/s) | Degree of Imbalance | Avg VM Utilization (%) |
|---|---:|---:|---:|---:|---:|---:|
| Synthetic_1000 | 1000 | 18697.8676 | 9187.692 | 0.0535 | 20 | 19.926 |
| Synthetic_2000 | 2000 | 37392.9544 | 18707.5826 | 0.0535 | 20 | 19.9101 |
| Synthetic_3000 | 3000 | 55813.8372 | 27481.9951 | 0.0538 | 20 | 19.9544 |
| Synthetic_4000 | 4000 | 73713.864 | 36027.1377 | 0.0543 | 20 | 19.9784 |
| Synthetic_5000 | 5000 | 92886.0948 | 46383.4689 | 0.0538 | 20 | 19.9953 |
| Synthetic_6000 | 6000 | 111503.3932 | 55737.1555 | 0.0538 | 20 | 19.9879 |
| Synthetic_7000 | 7000 | 130591.384 | 65521.7715 | 0.0536 | 20 | 19.979 |
| Synthetic_8000 | 8000 | 148490.6976 | 74212.2587 | 0.0539 | 20 | 19.9988 |
| Synthetic_9000 | 9000 | 166938.2828 | 82771.7851 | 0.0539 | 20 | 19.9948 |
| Synthetic_10000 | 10000 | 185179.1696 | 94672.2837 | 0.054 | 20 | 19.993 |

### Validasi

- 60 run valid, Completed Cloudlets sama dengan jumlah task pada semua dataset.
- Tiga run pada dataset yang sama menghasilkan angka identik sampai digit terakhir. Hal ini wajar karena MET dan simulasi CloudSim pada project ini deterministik (tidak ada elemen acak), sehingga validasi repeatability terpenuhi secara trivial.

### Catatan Interpretasi

- **Degree of Imbalance selalu 20.** Hanya VM 15 yang menerima beban, sehingga Lmin = 0 dan Lavg = Lmax / 20. Nilai 20 adalah ketimpangan maksimum untuk 20 VM.
- **GoCJ tidak naik monoton terhadap jumlah task.** Tiap file GoCJ_N adalah sampel independen dengan komposisi job besar berbeda, dan karena semua job masuk satu VM, makespan sangat sensitif terhadap komposisi job besar, bukan hanya jumlah task.
- **Synthetic hampir linier.** Proporsi kategori tetap, sehingga beban rata-rata per task hampir konstan. Throughput stabil sekitar 0,054 cloudlet/s dan utilisasi sekitar 20%.
- **Utilisasi synthetic tertahan sekitar 20%.** Utilisasi dihitung per VM (total CPU time / (makespan x 20 VM)), dan semua beban hanya ada di VM 15 yang memiliki 4 core, sehingga batasnya sekitar 4/20. Ini adalah perhitungan sederhana sesuai implementasi program, bukan utilisasi cloud nyata.

## 11. Definisi Metrics

| Metrik | Definisi pada program |
|---|---|
| Makespan | max(finish time) |
| Degree of Imbalance | (Load_max - Load_min) / Load_average, Load = total actual CPU time tiap VM |
| Average Waiting Time | total start time / jumlah hasil (start time dipakai sebagai proxy waiting time) |
| Throughput | jumlah hasil / makespan |
| Average VM Utilization | (total actual CPU time / (makespan x jumlah VM)) x 100% |

## 12. Troubleshooting

- **`mvn` tidak dikenali:** periksa instalasi Maven dan `PATH`, lalu jalankan `mvn -version`.
- **Versi Java salah:** pastikan `java -version` dan `mvn -version` sama-sama memakai JDK 21.
- **Dataset tidak ditemukan:** jalankan dari root repository dan pastikan path pada `-Dexec.args` benar.
- **Main class tidak ditemukan:** pastikan file Java berada di `modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/` dengan `package org.cloudbus.cloudsim.examples;`, lalu `mvn clean install`.
- **`Unknown lifecycle phase ".mainClass=..."` saat otomatisasi:** lihat catatan `cmd /c` pada bagian 8.
- **`KeyError: 'Family'` pada `average-results.py`:** CSV berawalan karakter BOM. Gunakan versi skrip pada repo yang membuka file dengan `encoding="utf-8-sig"`.

## 13. Yang Di-commit dan yang Tidak

Di-commit: `README.md`, `run-experiments.ps1`, `average-results.py`, `generate-synthetic.py`, `dataset/`, `results/`, dan source Java yang diubah.

Jangan di-commit (sudah ada di `.gitignore`):

```gitignore
**/target/
*.class
.idea/
*.iml
.vscode/
.project
.classpath
.settings/
.DS_Store
Thumbs.db
```

## 14. Catatan Reproducibility

Jangan mengubah hal berikut tanpa mencatatnya sebagai eksperimen baru: dataset, jumlah cloudlet, jumlah host, jumlah VM, kapasitas VM, rumus MET, dan aturan tie-breaking.

## 15. Referensi

- CloudSim. Cloudslab. https://github.com/Cloudslab/cloudsim
- CloudSim v7.0.1. https://github.com/Cloudslab/cloudsim/releases/tag/7.0.1
- Hussain, A. dan Aleem, M. (2018). GoCJ: Google Cloud Jobs Dataset for Distributed and Cloud Computing Infrastructures. Data, 3(4), 38. https://doi.org/10.3390/data3040038
- GoCJ Dataset. Mendeley Data. https://doi.org/10.17632/b7bp6xhrcd.1
