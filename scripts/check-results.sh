#!/usr/bin/env bash
# Vérifie le nombre de mains détectées par image de test (lignes "HANDS image=... n=...").
set -u
RESULTS=${1:?results.txt}
fail=0
expect() {
  local img=$1 n=$2
  if grep -q "HANDS image=$img n=$n " "$RESULTS"; then
    echo "OK   $img -> $n main(s)"
  else
    echo "FAIL $img : attendu $n ; obtenu: $(grep "image=$img " "$RESULTS" | tail -1 || echo 'aucune ligne')"
    fail=1
  fi
}
expect two_hands.jpg 2
expect thumb_up.jpg 1
expect victory.jpg 1
expect pointing_up.jpg 1
exit $fail
