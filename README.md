# VoiceTranslatorClean

🎙️ Android + Flask App that converts voice in any Indian language into accurate Hindi text using Whisper + LLM backend.

## Features
- Android app built with Kotlin (XML layout)
- Flask backend using Faster-Whisper + OpenAI/GPT
- Converts Hindi, Telugu, English voice to clean Hindi text
- Multi-accent, slang-aware speech recognition
- API-secured with environment variables

## How to Run
1. Clone the repo
2. Set your `OPENAI_API_KEY` in a `.env` file inside `backendenv/backend/`
3. Run backend:
   ```bash
   cd backendenv/backend
   flask run
