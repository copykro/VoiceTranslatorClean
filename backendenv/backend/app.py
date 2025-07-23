# from flask import Flask, request, jsonify
# from flask_cors import CORS
# from faster_whisper import WhisperModel
# from langdetect import detect
# from deep_translator import GoogleTranslator  # ✅ Add this line

# app = Flask(__name__)
# CORS(app)

# @app.route("/", methods=["GET"])
# def home():
#     return jsonify({"message": "Voice Translator Backend is running!"})

# @app.route("/transcribe", methods=["POST"])
# def transcribe_audio():
#     if 'file' not in request.files:
#         return jsonify({"error": "No file part in request"}), 400

#     audio_file = request.files['file']
#     if audio_file.filename == '':
#         return jsonify({"error": "No selected file"}), 400

#     file_path = "temp_audio.wav"
#     audio_file.save(file_path)

#     model = WhisperModel("base", compute_type="int8", device="cpu")
#     segments, _ = model.transcribe(file_path)

#     full_text = " ".join([segment.text for segment in segments])
#     lang = detect(full_text)

#     # ✅ Translate to Hindi
#     translated_text = GoogleTranslator(source='auto', target='hi').translate(full_text)

#     return jsonify({
#         "original_text": full_text,
#         "detected_language": lang,
#         "translated_text_hindi": translated_text
#     })


# if __name__ == "__main__":
#     app.run(debug=True, host="0.0.0.0", port=5001)



from flask import Flask, request, jsonify
from flask_cors import CORS
from faster_whisper import WhisperModel
from deep_translator import GoogleTranslator
import os

# ✅ Optional: Set huggingface cache path to avoid re-downloading
os.environ["TRANSFORMERS_CACHE"] = "D:/VoiceTranslatorProject/huggingface_cache"

app = Flask(__name__)
CORS(app)

# ✅ Load Whisper model (base is lightweight)
model = WhisperModel("large", device="cpu", compute_type="int8")

# ✅ Manual language mapping
lang_map = {
    "hi": "Hindi",
    "en": "English",
    "te": "Telugu",
    "ur": "Urdu",
    "bn": "Bengali",
    "kn": "Kannada",
    "ta": "Tamil",
    "ml": "Malayalam",
    "gu": "Gujarati",
    "pa": "Punjabi",
    "mr": "Marathi"
}

@app.route("/")
def home():
    return "✅ Voice Translator Backend Running (Whisper-based)"

@app.route("/transcribe", methods=["POST"])
def transcribe():
    try:
        if 'file' not in request.files:
            return jsonify({"error": "No audio file provided"}), 400

        audio_file = request.files['file']
        audio_path = "temp.mp4"
        audio_file.save(audio_path)

        # ✅ Transcribe with Whisper
        segments, info = model.transcribe(audio_path)

        # ✅ CORRECTED: Access language as attribute, not dict
        detected_lang_code = info.language or "unknown"
        detected_lang = lang_map.get(detected_lang_code, detected_lang_code)

        # ✅ Combine segment texts
        full_text = "".join([segment.text.strip() for segment in segments])

        if not full_text.strip():
            return jsonify({"error": "No speech detected"}), 400

        # ✅ Translate to Hindi
        translated = GoogleTranslator(source='auto', target='hi').translate(full_text)

        return jsonify({
            "original": full_text,
            "language": detected_lang,
            "translated": translated
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=5001)
