# Vision Read Number

ScaleVision keeps the CameraX preview active, but numeric inference is executed only when the user requests a reading with **LEER NÚMERO**.

## Flow

1. Android captures the current frame from `PreviewView`.
2. Android sends the JPEG to `POST /api/vision/read-number`.
3. The backend stores the frame temporarily and returns a `vision_session_id`.
4. The backend executes the configured vision model.
5. The model must return either `ok` with an unambiguous visible number or `not_readable`.
6. The backend validates the returned numeric text before converting it to `value`.
7. ScaleVision updates its current value only for a valid reading.
8. When the Gemini command flow receives the same `vision_session_id`, Gemini can call `vision_read_number` when a numeric image reading is actually required.

## Safety rules

- No continuous OCR loop.
- No inference from previous values.
- No completion of missing digits.
- No calculation or estimation from the image.
- Ambiguous digits, signs, or decimal separators return `not_readable`.
- The OpenAI API key remains in the backend and is never packaged in the APK.
