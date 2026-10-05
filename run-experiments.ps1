# run-experiments.ps1
# Otomatisasi 60 run (GoCJ 100-1000 x3, Synthetic 1000-10000 x3) untuk
# Week4_MET_TaskScheduling, lalu parse metrics langsung ke CSV.
#
# Jalankan dari ROOT repository (folder yang berisi pom.xml), misalnya:
#   cd C:\Users\Abiyyu\Documents\Coding\SOKA\SimulasiMET
#   powershell -ExecutionPolicy Bypass -File run-experiments.ps1
#
# Syarat: mvn clean install sudah pernah dijalankan dan BUILD SUCCESS.
#
# CATATAN PERBAIKAN (v2): perintah mvn sekarang dijalankan lewat
# "cmd /c <string tunggal>", bukan "& mvn <token terpisah>". PowerShell
# kadang memotong argumen "-Dexec.mainClass=..." saat diteruskan sebagai
# token terpisah ke file .cmd eksternal (muncul sebagai error
# "Unknown lifecycle phase .mainClass=..."). Dengan cmd /c, seluruh
# command diparse oleh cmd.exe sendiri, persis seperti saat diketik
# manual di terminal.

$ErrorActionPreference = "Stop"

$mainClass = "org.cloudbus.cloudsim.examples.Week4_MET_TaskScheduling"
$rawDir    = "results\raw"
$csvPath   = "results\experiment-results.csv"
$runsPerDataset = 3

New-Item -ItemType Directory -Force -Path $rawDir | Out-Null
New-Item -ItemType Directory -Force -Path "results" | Out-Null

# Daftar dataset: (Family, Label, Path, Tasks)
$datasets = @()
foreach ($n in 100,200,300,400,500,600,700,800,900,1000) {
    $datasets += [PSCustomObject]@{
        Family = "GoCJ"
        Label  = "GoCJ_$n"
        Path   = "dataset/GoCJ_Dataset_$n.txt"
        Tasks  = $n
    }
}
foreach ($n in 1000,2000,3000,4000,5000,6000,7000,8000,9000,10000) {
    $datasets += [PSCustomObject]@{
        Family = "Synthetic"
        Label  = "Synthetic_$n"
        Path   = "dataset/synthetic/Synthetic_$n.txt"
        Tasks  = $n
    }
}

# Siapkan CSV header kalau belum ada
if (-not (Test-Path $csvPath)) {
    "Family,Dataset,Tasks,Run,CompletedCloudlets,Makespan,DegreeOfImbalance,AverageWaitingTime,Throughput,AverageVMUtilization" |
        Out-File -FilePath $csvPath -Encoding utf8
}

function Parse-Metric($text, $pattern) {
    $m = [regex]::Match($text, $pattern)
    if ($m.Success) { return $m.Groups[1].Value.Trim() } else { return "" }
}

$totalRuns = $datasets.Count * $runsPerDataset
$current = 0

foreach ($ds in $datasets) {
    if (-not (Test-Path $ds.Path)) {
        Write-Warning "LEWATI $($ds.Label): file tidak ditemukan di $($ds.Path)"
        continue
    }

    for ($run = 1; $run -le $runsPerDataset; $run++) {
        $current++
        Write-Host "[$current/$totalRuns] $($ds.Label) run $run ..."

        $logFile = Join-Path $rawDir "$($ds.Label)_run$run.log"

        # Bangun SATU string command persis seperti yang diketik manual,
        # lalu serahkan ke cmd.exe untuk diparse sendiri.
        $cmdString = "mvn exec:java -pl modules/cloudsim-examples/ " +
                     "-Dexec.mainClass=$mainClass " +
                     "-Dexec.args=`"$($ds.Path)`""

        $output = cmd /c $cmdString 2>&1
        $output | Out-File -FilePath $logFile -Encoding utf8

        $text = $output -join "`n"

        if ($text -notmatch "BUILD SUCCESS") {
            Write-Warning "  -> BUILD GAGAL untuk $($ds.Label) run $run, cek $logFile"
            "$($ds.Family),$($ds.Label),$($ds.Tasks),$run,BUILD_FAILED,,,,," |
                Out-File -FilePath $csvPath -Append -Encoding utf8
            continue
        }

        $completed   = Parse-Metric $text "Completed Cloudlets\s*:\s*(\d+)"
        $makespan    = Parse-Metric $text "Makespan\s*:\s*([\d.]+)"
        $imbalance   = Parse-Metric $text "Degree of Imbalance\s*:\s*([\d.]+)"
        $waiting     = Parse-Metric $text "Average Waiting Time\s*:\s*([\d.]+)"
        $throughput  = Parse-Metric $text "Throughput\s*:\s*([\d.]+)"
        $utilization = Parse-Metric $text "Average VM Utilization\*?\s*:\s*([\d.]+)"

        if ($completed -eq "") {
            Write-Warning "  -> BUILD SUCCESS tapi metrics tidak ketemu di output untuk $($ds.Label) run $run. Cek $logFile dan beri tahu Claude format barisnya."
        }

        "$($ds.Family),$($ds.Label),$($ds.Tasks),$run,$completed,$makespan,$imbalance,$waiting,$throughput,$utilization" |
            Out-File -FilePath $csvPath -Append -Encoding utf8

        Write-Host "  -> Completed=$completed Makespan=$makespan Imbalance=$imbalance Waiting=$waiting Throughput=$throughput Util=$utilization"
    }
}

Write-Host ""
Write-Host "SELESAI. Raw log ada di $rawDir\, hasil terkumpul di $csvPath"
Write-Host "Baris 'Average' per dataset belum dihitung otomatis oleh skrip ini -- lihat average-results.py untuk itu."
