"""Pré-calcule les landmarks des images de test (MediaPipe Python, même modèle que l'app).

Sert à (1) vérifier la détection attendue sur chaque image et (2) alimenter le mode "replay" de
l'app, utilisé dans l'émulateur CI où la lib native arm64 de MediaPipe plante (traduction ARM).
Usage : python scripts/gen_landmarks.py   (depuis la racine du repo)
"""
import json, pathlib
import mediapipe as mp
from mediapipe.tasks import python as mpt
from mediapipe.tasks.python import vision

assets = pathlib.Path("app/src/main/assets")
opts = vision.HandLandmarkerOptions(
    base_options=mpt.BaseOptions(model_asset_path=str(assets / "hand_landmarker.task")),
    num_hands=2,
)
landmarker = vision.HandLandmarker.create_from_options(opts)
for img in sorted((assets / "debug").glob("*.jpg")):
    image = mp.Image.create_from_file(str(img))
    res = landmarker.detect(image)
    hands = [
        {
            "label": res.handedness[i][0].category_name,
            "score": res.handedness[i][0].score,
            "landmarks": [[p.x, p.y, p.z] for p in pts],
        }
        for i, pts in enumerate(res.hand_landmarks)
    ]
    out = img.with_suffix(".json")
    out.write_text(json.dumps({"width": image.width, "height": image.height, "hands": hands}))
    print(f"{img.name}: {len(hands)} main(s) {[h['label'] for h in hands]}")
