# Creating Custom Keyboard Layouts / Creación de Distribuciones de Teclado Personalizadas

This document describes how to create and import custom keyboard layouts for the **DIGI Keyboard** application.

---

## 1. File Format and Structure / Formato y Estructura del Archivo

All layout files must be written in **JSON** format. The file name must match the language identifier (e.g., `ua.json`, `es.json`, `fr.json`).

The JSON file must have the following structure:
```json
{
  "id": "language_id",
  "name": "Full Language Name",
  "shortName": "SHORT_NAME",
  "map": {
    "ANDROID_KEYCODE": ["normal", "Shift", "AltGr", "AltGr+Shift"]
  }
}
```

### JSON Fields / Campos JSON:
* **`id`**: Unique string identifier for the layout (e.g., `ua`, `es`, `ro`). Must match the filename (without `.json`).
* **`name`**: Full name of the layout that will be shown in the UI check-boxes (e.g., `Spanish`, `Ukrainian`). For user-imported layouts, this name is displayed directly in the settings UI. / Nombre completo de la distribución que se mostrará en las casillas de verificación de la interfaz (ej. `Spanish`, `Ukrainian`). Para distribuciones importadas dinámicamente, este nombre se mostrará directamente en los ajustes.
* **`shortName`**: Short 2-3 letter capitalized code (e.g., `ES`, `UA`, `RO`) that will be shown on the top overlay HUD when cycling through layouts.
* **`map`**: A dictionary where keys are **Android KeyCode numbers** (written as strings) and values are arrays containing 2 to 4 string characters.

---

## 2. Map Array Specification / Especificación del Array de Mapa

Each key in the `"map"` block points to an array mapping keypress states:
1. **`index 0` (Normal)**: The character injected when pressing the key normally (lowercase / basic).
2. **`index 1` (Shift)**: The character injected when pressing `Shift + key` or with `CapsLock` active.
3. **`index 2` (AltGr) [Optional]**: The character injected when pressing `Right Alt (AltGr) + key`.
4. **`index 3` (AltGr + Shift) [Optional]**: The character injected when pressing `Right Alt (AltGr) + Shift + key`.

### Example (Spanish Layout excerpt / Extracto de distribución española):
```json
{
  "id": "es",
  "name": "Spanish",
  "shortName": "ES",
  "map": {
    "29": ["a", "A"],
    "75": ["'", "?", "{", "["],
    "71": ["`", "^", "[", "{"],
    "72": ["+", "*", "]", "}"]
  }
}
```
* Pressing key `75` normally yields `'`.
* Pressing `Shift + 75` yields `?`.
* Pressing `AltGr + 75` yields `{`.
* Pressing `AltGr + Shift + 75` yields `[`.

---

## 3. Finding Android KeyCodes / Cómo encontrar los KeyCodes de Android

Android uses internal integers for physical keyboard events. You can find key codes by referencing the official Android developer documentation for [KeyEvent KeyCodes](https://developer.android.com/reference/android/view/KeyEvent).

### Common KeyCodes Reference / Referencia de KeyCodes comunes:
* **`29`** to **`54`**: Keys `A` to `Z` (QWERTY layout keys)
* **`7`** to **`16`**: Keys `0` to `9`
* **`71`**: `[ {` (KEYCODE_LEFT_BRACKET)
* **`72`**: `] }` (KEYCODE_RIGHT_BRACKET)
* **`74`**: `; :` (KEYCODE_SEMICOLON)
* **`75`**: `' "` (KEYCODE_APOSTROPHE)
* **`73`**: `\ |` (KEYCODE_BACKSLASH)
* **`68`**: `` ` ~ `` (KEYCODE_GRAVE)
* **`55`**: `, <` (KEYCODE_COMMA)
* **`56`**: `. >` (KEYCODE_PERIOD)

---

## 4. How to Import Layouts / Cómo importar las distribuciones

There are two ways to load layouts into the application:

### A. Pre-built Layouts (System Assets)
Place your `.json` file inside the `app/src/main/assets/layouts/` directory before building the APK. These layouts are hardcoded in the application and cannot be deleted by the user.

### B. User Imported Layouts (Runtime)
1. Prepare your custom JSON file on your TV box storage or a USB flash drive.
2. Open the **DIGI Keyboard** app on the device.
3. Scroll to the bottom and click **Import Custom JSON Layout** / **Importar diseño JSON personalizado**.
4. Select the JSON file using the system file picker.
5. The layout will be validated, saved to the app's internal storage (`layouts/` folder in device-protected storage), and will appear in the active layouts list immediately.
