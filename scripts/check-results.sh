#!/usr/bin/env bash
# Vérifie, par image de test, le nombre de mains et les gestes lissés
# (lignes "HANDS image=... n=... gestures=SIDE:GESTE:doigts,...").
set -u
RESULTS=${1:?results.txt}
fail=0
# expect IMAGE NB_MAINS [GESTE:DOIGTS]
expect() {
  local img=$1 n=$2 gesture=${3:-}
  local line
  line=$(grep "HANDS image=$img " "$RESULTS" | tail -1)
  if [[ "$line" == *" n=$n "* && ( -z "$gesture" || "$line" == *":$gesture"* ) ]]; then
    echo "OK   $img -> $n main(s) ${gesture}"
  else
    echo "FAIL $img : attendu n=$n ${gesture} ; obtenu: ${line:-aucune ligne}"
    fail=1
  fi
}
expect two_hands.jpg 2
expect thumb_up.jpg 1 THUMBS_UP:1
expect victory.jpg 1 VICTORY:2
expect pointing_up.jpg 1
expect fist.jpg 1 FIST:0
exit $fail
