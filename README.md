# Cloud Task Scheduling with Minimum Execution Time (MET)

Implementasi simulasi **Cloud Task Scheduling** menggunakan algoritma heuristic **Minimum Execution Time (MET)** pada **CloudSim 7.0.1**.

Project ini dibuat untuk tugas mata kuliah **Strategi Optimasi Komputasi Awan**, Kelompok 7.

## 1. Ringkasan Project

Eksperimen menggunakan workload **independent Bag of Tasks (BoT)** dari dataset **GoCJ_Dataset_1000.txt**.

Konfigurasi utama:

| Komponen | Konfigurasi |
|---|---|
| Simulator | CloudSim 7.0.1 |
| Java | OpenJDK 21 LTS |
| Build tool | Apache Maven 3.9.16 |
| Datacenter | 1 |
| Host | 10 |
| Host Class A | 5 host, 4 core, 2000 MIPS/core, 8 GB RAM, 1 TB storage, 1 Gbps |
| Host Class B | 5 host, 8 core, 3000 MIPS/core, 16 GB RAM, 2 TB storage, 1 Gbps |
| VM | 20 VM heterogen |
| VM Type 1 | 5 VM, 1 core, 1000 MIPS, 2 GB RAM, 20 GB storage |
| VM Type 2 | 5 VM, 2 core, 1500 MIPS, 2 GB RAM, 20 GB storage |
| VM Type 3 | 5 VM, 2 core, 2000 MIPS, 4 GB RAM, 30 GB storage |
| VM Type 4 | 5 VM, 4 core, 2500 MIPS, 4 GB RAM, 40 GB storage |
| Cloudlet | 1000 |
| Dataset | GoCJ_Dataset_1000.txt |
| Algoritma | Minimum Execution Time (MET) |

## 2. Cara Kerja MET

Untuk setiap cloudlet, MET menghitung estimated execution time terhadap seluruh VM:

```text
ET(i,j) = CloudletLength(i) / VM_MIPS(j)
```

VM dengan estimated execution time paling kecil dipilih sebagai VM tujuan cloudlet.

Implementasi ini menggunakan **MET dasar**. MET tidak menggunakan current VM load atau queue length dalam keputusan penempatan.

Jika beberapa VM memiliki ET minimum yang sama, implementasi memilih VM pertama yang memenuhi kondisi minimum.

## 3. Struktur Repository

Repository direkomendasikan menggunakan struktur berikut:

```text
cloudsim-met-scheduling/
│
├── README.md
├── dataset/
│   └── GoCJ_Dataset_1000.txt
│
├── modules/
│   ├── cloudsim/
│   └── cloudsim-examples/
│       └── src/
│           └── main/
│               └── java/
│                   └── org/
│                       └── cloudbus/
│                           └── cloudsim/
│                               └── examples/
│                                   └── Week4_MET_TaskScheduling.java
│
├── pom.xml
└── ...
```

Repository ini sebaiknya merupakan **fork atau salinan CloudSim 7.0.1**, kemudian file simulasi dan dataset ditambahkan ke dalamnya. Dengan cara ini, repository dapat langsung dibangun menggunakan konfigurasi Maven CloudSim.

## 4. Persyaratan Sistem

### 4.1 Java JDK 21

Project menggunakan Java 21.

Pada Windows, instal JDK 21, misalnya Eclipse Temurin 21.

Setelah instalasi, buka Command Prompt atau PowerShell dan jalankan:

```powershell
java -version
```

Versi yang digunakan seharusnya menunjukkan Java 21.

Pastikan `JAVA_HOME` dan `PATH` sudah mengarah ke instalasi JDK 21 apabila Windows belum mengenal perintah `java`.

### 4.2 Apache Maven

Instal Apache Maven 3.9.x.

Periksa instalasi:

```powershell
mvn -version
```

Pastikan Maven menggunakan JDK 21.

## 5. Mendapatkan Repository

Clone repository:

```powershell
git clone <URL_REPOSITORY_ANDA>
```

Masuk ke folder repository:

```powershell
cd <NAMA_REPOSITORY>
```

Penting: jalankan perintah Maven dari **root repository**, yaitu folder yang berisi `pom.xml`.

## 6. Memastikan Dataset Berada di Lokasi yang Benar

Pastikan file berikut tersedia:

```text
dataset/GoCJ_Dataset_1000.txt
```

Struktur minimal:

```text
<NAMA_REPOSITORY>/
├── dataset/
│   └── GoCJ_Dataset_1000.txt
└── pom.xml
```

Program membaca dataset menggunakan path:

```text
dataset/GoCJ_Dataset_1000.txt
```

Karena itu, program sebaiknya dijalankan dari root repository.

## 7. Build CloudSim

Dari root repository jalankan:

```powershell
mvn clean install
```

Perintah ini membangun modul CloudSim dan modul contoh yang digunakan project.

## 8. Uji Instalasi CloudSim

Sebelum menjalankan simulasi MET, uji terlebih dahulu CloudSim menggunakan example bawaan:

```powershell
mvn exec:java -pl modules/cloudsim-examples/ -Dexec.mainClass=org.cloudbus.cloudsim.examples.CloudSimExample1
```

Jika example berjalan, environment CloudSim sudah dapat digunakan.

## 9. Menjalankan Simulasi MET

Jalankan:

```powershell
mvn exec:java -pl modules/cloudsim-examples/ -Dexec.mainClass=org.cloudbus.cloudsim.examples.Week4_MET_TaskScheduling
```

Program akan:

1. Memuat `GoCJ_Dataset_1000.txt`
2. Membuat 1 datacenter
3. Membuat 10 host
4. Membuat 20 VM heterogen
5. Membuat 1000 cloudlet
6. Menjalankan algoritma MET
7. Menetapkan VM tujuan untuk setiap cloudlet
8. Menjalankan simulasi CloudSim
9. Menampilkan distribusi cloudlet dan metrik hasil

## 10. Hasil Baseline yang Diperoleh

Pada konfigurasi eksperimen yang digunakan dalam tugas, hasil yang diperoleh adalah:

| Metrik | Hasil |
|---|---:|
| Completed Cloudlets | 1000 |
| Makespan | 22779.81 s |
| Degree of Imbalance | 20 |
| Average Waiting Time | 8748.2664 s |
| Throughput | 0.0439 cloudlets/s |
| Average VM Utilization | 11.3839% |

Distribusi assignment:

```text
VM 15 -> 1000 cloudlets
VM lainnya -> 0 cloudlets
```

Hasil tersebut merupakan baseline eksperimen yang dijalankan pada konfigurasi project ini.

## 11. Mengapa VM 15 Mendapat Semua Cloudlet?

MET memilih VM berdasarkan estimated execution time terkecil.

Beberapa VM memiliki kapasitas MIPS tertinggi yang menghasilkan nilai ET minimum yang sama. Pada kondisi tie, implementasi memilih VM pertama yang memenuhi kondisi minimum.

Akibatnya, seluruh cloudlet pada eksperimen ini dialokasikan ke VM 15.

Hal ini merupakan konsekuensi dari implementasi MET dasar yang tidak mempertimbangkan current VM load.

Project ini tidak menambahkan load-aware tie-breaking karena hal tersebut akan mengubah metode yang diuji menjadi variasi MET yang berbeda.

## 12. Troubleshooting

### `mvn` tidak dikenali

Jika muncul:

```text
'mvn' is not recognized as an internal or external command
```

Periksa instalasi Maven dan `PATH`.

Jalankan:

```powershell
mvn -version
```

### `java` menggunakan versi yang salah

Periksa:

```powershell
java -version
mvn -version
```

Pastikan keduanya menggunakan JDK 21.

### Dataset tidak ditemukan

Pastikan file berada di:

```text
dataset/GoCJ_Dataset_1000.txt
```

dan jalankan program dari root repository.

### Main class tidak ditemukan

Pastikan file Java berada di:

```text
modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/Week4_MET_TaskScheduling.java
```

dan deklarasi package sesuai:

```java
package org.cloudbus.cloudsim.examples;
```

Kemudian jalankan kembali:

```powershell
mvn clean install
```

## 13. File yang Perlu Di-commit

### Wajib

Jika repository adalah fork atau salinan CloudSim 7.0.1, pertahankan seluruh source dan konfigurasi yang diperlukan CloudSim. Tambahkan:

```text
README.md
dataset/GoCJ_Dataset_1000.txt
modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/Week4_MET_TaskScheduling.java
```

### Disarankan

Tambahkan hasil eksperimen dalam folder terpisah:

```text
results/
└── met-baseline-output.txt
```

Ini membantu anggota kelompok atau dosen melihat hasil tanpa harus langsung menjalankan simulasi.

### Jangan di-commit

```text
**/target/
*.class
.idea/
.vscode/
*.iml
```

Contoh `.gitignore`:

```gitignore
# Maven
**/target/

# Java
*.class

# IntelliJ IDEA
.idea/
*.iml

# VS Code
.vscode/

# OS
.DS_Store
Thumbs.db
```

## 14. Dataset

GoCJ berarti **Google Cloud Jobs Dataset**.

Dataset:

```text
GoCJ_Dataset_1000.txt
```

Jumlah job:

```text
1000
```

Task length dinyatakan dalam:

```text
Million Instructions (MI)
```

Sumber:

[GoCJ: Google Cloud Jobs Dataset - Mendeley Data](https://data.mendeley.com/datasets/b7bp6xhrcd/1)

## 15. CloudSim

Project menggunakan **CloudSim 7.0.1**.

Repository resmi:

[Cloudslab/CloudSim](https://github.com/Cloudslab/cloudsim)

Release yang digunakan:

[CloudSim v7.0.1](https://github.com/Cloudslab/cloudsim/releases/tag/7.0.1)

CloudSim menyediakan instruksi instalasi Windows yang menggunakan JDK 21 dan Maven, serta perintah Maven untuk menjalankan `CloudSimExample1`. Lihat dokumentasi resmi CloudSim untuk referensi tambahan.

## 16. Reproduksi Eksperimen

Untuk mereproduksi eksperimen dari komputer baru:

```powershell
git clone <URL_REPOSITORY_ANDA>
cd <NAMA_REPOSITORY>

java -version
mvn -version

mvn clean install

mvn exec:java -pl modules/cloudsim-examples/ -Dexec.mainClass=org.cloudbus.cloudsim.examples.CloudSimExample1

mvn exec:java -pl modules/cloudsim-examples/ -Dexec.mainClass=org.cloudbus.cloudsim.examples.Week4_MET_TaskScheduling
```

Setelah selesai, periksa output distribusi VM dan metrik simulasi.

## 17. Catatan Reproducibility

Konfigurasi eksperimen harus dipertahankan agar hasil dapat dibandingkan dengan hasil baseline.

Jangan mengubah hal berikut tanpa mencatatnya sebagai eksperimen baru:

- dataset
- jumlah cloudlet
- jumlah host
- jumlah VM
- kapasitas VM
- rumus MET
- aturan tie-breaking

## 18. Referensi

- CloudSim. Cloudslab. https://github.com/Cloudslab/cloudsim
- CloudSim v7.0.1. https://github.com/Cloudslab/cloudsim/releases/tag/7.0.1
- GoCJ: Google Cloud Jobs Dataset. Mendeley Data. https://data.mendeley.com/datasets/b7bp6xhrcd/1
