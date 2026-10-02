#!/usr/bin/env python3
"""Uji mutasi: rusak kondisi penting di kode produksi satu per satu; tiap mutan HARUS membuat tes gagal.
Mutan yang selamat = ada perilaku yang tidak dijaga tes."""
import os, shutil, subprocess, sys, tempfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
CORE = os.path.join(ROOT, "shared/src/commonMain/kotlin/id/biojelan/app/core")
RUN = os.path.join(ROOT, "tools/logic-tests/run.sh")

MUTANTS = [
    # (file, nama, potongan asli, pengganti)
    ("AutoRefreshGate.kt", "abaikan polling yang masih berjalan", "if (job?.isActive == true || !canRun()) return", "if (!canRun()) return"),
    ("AutoRefreshGate.kt", "abaikan canRun", "if (job?.isActive == true || !canRun()) return", "if (job?.isActive == true) return"),
    ("AutoRefreshGate.kt", "batas jarak minimum off-by-one", "elapsed in 0 until minGapMs", "elapsed in 0..minGapMs"),
    ("AutoRefreshGate.kt", "tidak mencatat waktu mulai", "        lastStartedAt = t\n", ""),
    ("AutoRefreshGate.kt", "tanpa masa tenggang awal", "private var lastStartedAt: Long = now()", "private var lastStartedAt: Long = Long.MIN_VALUE / 2"),
    ("AutoRefreshGate.kt", "exception bocor", "            } catch (e: Exception) {\n                // Polling gagal diam-diam: data lama tetap tampil, putaran berikutnya mencoba lagi.\n            }", "            } finally {\n            }"),
    ("AutoRefreshGate.kt", "cancel() tidak membatalkan", "        job?.cancel()\n", ""),
    ("AutoRefreshGate.kt", "jam mundur memblokir", "if (elapsed in 0 until minGapMs) return", "if (elapsed < minGapMs) return"),
    ("DedupeWindow.kt", "abaikan isi pesan", "if (text == lastText && elapsed in 0 until windowMs) return false", "if (elapsed in 0 until windowMs) return false"),
    ("DedupeWindow.kt", "batas jendela off-by-one", "elapsed in 0 until windowMs", "elapsed in 0..windowMs"),
    ("DedupeWindow.kt", "pesan yang dibuang memperpanjang jendela", "        if (text == lastText && elapsed in 0 until windowMs) return false\n", "        val inside = text == lastText && elapsed in 0 until windowMs\n        lastEmittedAt = t\n        if (inside) return false\n"),
    ("DedupeWindow.kt", "jam mundur menekan pesan", "elapsed in 0 until windowMs", "elapsed < windowMs"),
    ("ChangeDetection.kt", "item lama dianggap baru", "return after.any { actionable(it) && key(it) !in known }", "return after.any { actionable(it) }"),
    ("ChangeDetection.kt", "item tanpa aksi ikut dihitung", "return after.any { actionable(it) && key(it) !in known }", "return after.any { key(it) !in known }"),
    ("ChangeDetection.kt", "item baru dianggap ganti status", "id(item) in previous && previous[id(item)] != status(item)", "previous[id(item)] != status(item)"),
    ("ChangeDetection.kt", "status sama dianggap berubah", "previous[id(item)] != status(item)", "previous[id(item)] == status(item)"),
    ("ChangeDetection.kt", "abaikan status muat sebelumnya", "previousLoadedOk && !incomingId", "!incomingId"),
    ("ChangeDetection.kt", "id kosong dianggap penugasan", "!incomingId.isNullOrBlank()", "incomingId != null"),
    ("ChangeDetection.kt", "penugasan sama diumumkan lagi", " && incomingId != previousId", ""),
]

START = int(sys.argv[1]) if len(sys.argv) > 1 else 0  # lanjutkan dari mutan ke-N kalau terpotong batas waktu
survivors = []
for fname, name, old, new in MUTANTS[START:]:
    tmp = tempfile.mkdtemp()
    for f in ("AutoRefreshGate.kt", "DedupeWindow.kt", "ChangeDetection.kt"):
        shutil.copy(os.path.join(CORE, f), tmp)
    path = os.path.join(tmp, fname)
    src = open(path).read()
    if old not in src:
        print(f"  ?? {fname}: potongan tidak ditemukan -> {name}"); survivors.append(name + " (SKRIP RUSAK)"); continue
    open(path, "w").write(src.replace(old, new, 1))
    r = subprocess.run([RUN], env={**os.environ, "CORE_DIR": tmp}, capture_output=True, text=True)
    out = r.stdout + r.stderr
    summary = [l for l in out.splitlines() if "lulus" in l and "gagal" in l]
    killed = r.returncode != 0
    print(f"  {'DIBUNUH ' if killed else 'SELAMAT '} {fname:22s} {name}   [{summary[-1].strip() if summary else 'compile error?'}]")
    if not killed: survivors.append(name)
    shutil.rmtree(tmp, ignore_errors=True)

print(f"\n{len(MUTANTS[START:]) - len(survivors)}/{len(MUTANTS[START:])} mutan dibunuh")
sys.exit(1 if survivors else 0)
