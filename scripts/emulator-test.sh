#!/usr/bin/env bash
# Exécuté dans l'émulateur par .github/workflows/emulator.yml.
# Installe l'APK, lance l'app en mode debug, capture l'écran et le logcat dans $OUT.
# Replay : l'émulateur ne peut pas exécuter la lib MediaPipe (crash de la traduction ARM) ; on valide donc
# l'affichage et l'overlay avec des landmarks pré-calculés. La détection réelle se teste sur téléphone.
set -u
APK=${1:?apk}
OUT=${2:?outdir}
PKG=fr.kchallenge.touchp
mkdir -p "$OUT"

adb install -r "$APK" 2>&1 | tee "$OUT/install.txt"
adb shell pm grant $PKG android.permission.CAMERA
adb logcat -c
adb shell am start -n $PKG/.MainActivity --ez debug true --ez replay true | tee "$OUT/start.txt"

# Le mode debug enchaîne 4 images de 3 s ; une capture toutes les ~2,5 s, 20 s au total.
for i in $(seq -w 1 8); do
  sleep 2.5
  adb exec-out screencap -p > "$OUT/shot_$i.png"
done

adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && adb pull /sdcard/ui.xml "$OUT/ui.xml" >/dev/null 2>&1
adb logcat -d > "$OUT/logcat_full.txt"
adb logcat -d -s TouchP:I AndroidRuntime:E DEBUG:F libc:F > "$OUT/logcat.txt"
grep -E "HANDS|PERF" "$OUT/logcat.txt" > "$OUT/results.txt" || true
cat "$OUT/results.txt"
