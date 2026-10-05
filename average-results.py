import csv, sys
from collections import OrderedDict

SRC = sys.argv[1] if len(sys.argv) > 1 else "results/experiment-results.csv"
OUT_AVG = "results/experiment-averages.csv"
METRICS = ["Makespan", "AverageWaitingTime", "Throughput", "DegreeOfImbalance", "AverageVMUtilization"]

# utf-8-sig membuang BOM di awal file (penyebab KeyError: 'Family')
with open(SRC, newline="", encoding="utf-8-sig") as f:
    rows = list(csv.DictReader(f))

ok, skipped = [], 0
for r in rows:
    if r["CompletedCloudlets"] in ("", "BUILD_FAILED"):
        skipped += 1
        continue
    ok.append(r)
print(f"Total baris: {len(rows)}, valid: {len(ok)}, dilewati (BUILD_FAILED/kosong): {skipped}")

groups = OrderedDict()
problems = []
for r in ok:
    key = (r["Family"], r["Dataset"], int(r["Tasks"]))
    groups.setdefault(key, []).append(r)
    if int(r["CompletedCloudlets"]) != int(r["Tasks"]):
        problems.append(f"Completed != Tasks: {r['Dataset']} run {r['Run']}")

avg_rows = []
for (fam, ds, tasks), rs in groups.items():
    if len(rs) != 3:
        problems.append(f"{ds}: jumlah run valid = {len(rs)} (harus 3)")
    vals = {m: [float(x[m]) for x in rs] for m in METRICS}
    for m in METRICS:
        if max(vals[m]) != min(vals[m]):
            problems.append(f"{ds}: {m} tidak identik antar run {vals[m]}")
    avg_rows.append([fam, ds, tasks] + [round(sum(vals[m]) / len(rs), 4) for m in METRICS])

with open(OUT_AVG, "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["Family", "Dataset", "Tasks", "AvgMakespan", "AvgWaitingTime",
                "AvgThroughput", "AvgDegreeOfImbalance", "AvgVMUtilization"])
    w.writerows(avg_rows)

print(f"Dataset: {len(avg_rows)} (harus 20)")
print("Masalah validasi:" if problems else "Validasi OK: tidak ada masalah.")
for p in problems: print("  -", p)
