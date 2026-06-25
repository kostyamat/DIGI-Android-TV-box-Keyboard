<p align="center">
  <a href="https://www.paypal.com/paypalme/kostyamat">
    <img src="https://thumbs.dreamstime.com/b/cute-kawaii-coffee-mug-character-smiling-steam-isolated-white-adorable-cartoon-happy-face-decorative-lace-pattern-401912575.jpg" alt="Buy me a coffee" width="200"/>
    <br>
    <strong>If you found my work helpful, buy me a coffee! It keeps me motivated ☕</strong>
  </a>
</p>

---

# DIGI Keyboard (ZTE R2A Q)

An Android TV input utility designed specifically to unlock physical keyboard input, enable custom layouts, and remap remote buttons on provider-restricted TV boxes.

[**English**](#english-documentation) | [**Español**](#documentación-en-español)

---

## English Documentation

### 1. Project Purpose & Context
This application was created for the users of provider TV boxes distributed by the telecom operator **DIGI** (specifically the **ZTE R2A** box running **Android TV 14 / API 34**). 

The primary use case is to allow normal users to freely use a physical keyboard with their TV box without being blocked by provider firmware limitations, and seamlessly switch back to the on-screen keyboard when using the TV remote.

#### The Problems Solved:
* **The "q/Q" Key Grab**: The operator's ROM intercepts the physical `q` / `Q` key (and the remote's Q button) at a low system level, hardcoding it to launch their own VOD online streaming application. As a result, the letter `q` cannot be typed into any normal input field.
* **Missing OS Settings**: The firmware completely hides the Android settings menu for selecting physical keyboard layouts and switching languages.
* **Smart IME Switching**: Android natively struggles to use both a physical keyboard and an on-screen TV keyboard at the same time. This app includes a **Smart Switcher** that detects when your physical wireless keyboard connects/sleeps, and dynamically hot-swaps between DIGI Keyboard and your default on-screen keyboard (e.g. Gboard) based on whether you are actively typing.
* **Catcher Mode (Remote Remapping)**: The app can intercept custom hardware buttons on your remote (like the provider's VOD buttons) and remap them to launch other apps, trigger media controls, or go back to the Home screen.

#### Universal Compatibility & Custom Forks:
* **Universal Android TV Support**: Although originally created for the ZTE R2A, it contains no proprietary hardware code. It can be used without restrictions on any Android TV box to enable advanced layout management and auto-switching.
* **Free Alternative**: A free, open-source replacement for paid physical keyboard mappers on the Play Store.
* **Bonus Layouts**: The `docs/` directory contains two extra layouts (`de.json` and `pl.json`) as examples. You can import them directly into the app for testing or edit them for your own needs.

### 2. Automated Setup (No PC Required)
DIGI Keyboard features a built-in Local ADB system that automatically grants itself the necessary permissions without needing a computer.

**Primary Method (Target Device: DIGI R2A):**
1. **Developer Options**: Click the first button in the app's Wizard to instantly open Android Developer Options. (If it doesn't open, manually go to Settings -> System -> About -> click "Build" 7 times).
2. **Enable USB Debugging**: Inside Developer Options, turn on **USB Debugging** (on some non-DIGI boxes, you might need to try Wireless Debugging instead).
3. **Auto-Grant**: Return to the app and click the second Wizard button. The app will use a local ADB loopback to automatically grant itself the `WRITE_SECURE_SETTINGS` and `ACCESS_RESTRICTED_SETTINGS` permissions.

Once permissions are granted, the app configures itself. Cycle layouts with `Ctrl + Space` or `Win / Meta`.

**Fallback Method (For PC):**
If the automated method fails on your specific TV box, you can grant the permissions manually using a computer via ADB:
```bash
adb shell pm grant com.kostyamat.r2r_q android.permission.WRITE_SECURE_SETTINGS
adb shell appops set com.kostyamat.r2r_q ACCESS_RESTRICTED_SETTINGS allow
```

### 3. Project Documentation Links
* [**Key Layouts Guide (JSON Customization)**](docs/KEYLAYOUTS.md) - Learn how to build and import custom keyboard maps.
* [**Developer's Technical Notes**](docs/DEVELOPERS_NOTE.md) - System architecture and component interactions.
* [**MIT License**](LICENSE) - Project license.

Author: **Kostyantyn Matviyevskyy aka kostyamat**

---

## Documentación en Español

### 1. Propósito y Contexto del Proyecto
Esta aplicación fue desarrollada para los usuarios de decodificadores (TV boxes) distribuidos por el operador **DIGI** (específicamente el modelo **ZTE R2A** con **Android TV 14 / API 34**).

El caso de uso principal es permitir a los usuarios utilizar libremente un teclado físico con su TV box sin verse bloqueados por las limitaciones del firmware del proveedor, y cambiar fluidamente al teclado en pantalla al usar el mando de la TV.

#### Problemas que Resuelve:
* **El Bloqueo de la Tecla "q/Q"**: La ROM del proveedor intercepta la tecla física `q` / `Q` para abrir su propia aplicación de streaming. Como consecuencia, la letra `q` no se puede escribir.
* **Falta de Ajustes de Sistema**: El firmware oculta por completo los menús para seleccionar la distribución del teclado físico.
* **Cambio Inteligente de IME**: Android tiene dificultades para manejar un teclado físico y uno en pantalla a la vez. Esta app incluye un **Cambio Inteligente** que detecta cuando el teclado físico se conecta/desconecta (o entra en reposo) y alterna dinámicamente entre el Teclado DIGI y su teclado en pantalla (ej. Gboard).
* **Modo Captura (Mapeo de Mando)**: La app puede interceptar botones físicos del mando a distancia y reasignarlos para abrir otras apps, controlar multimedia o volver a Inicio.

#### Compatibilidad Universal y Diseños Adicionales:
* **Soporte Universal**: Aunque fue creada para el ZTE R2A, no contiene código propietario y funciona en cualquier Android TV.
* **Alternativa Gratuita**: Un reemplazo gratuito y de código abierto para mapeadores de teclado de pago.
* **Diseños Extra**: El directorio `docs/` contiene dos plantillas adicionales (`de.json` y `pl.json`). Puede importarlos directamente en la app para pruebas o editarlos según sus necesidades.

### 2. Configuración Automática (Sin PC)
DIGI Keyboard incluye un sistema ADB Local integrado que se otorga a sí mismo los permisos necesarios automáticamente sin necesidad de un ordenador.

**Método Principal (Para DIGI R2A):**
1. **Opciones de Desarrollador**: Pulse el primer botón en el Asistente de la app para abrir instantáneamente las Opciones de Desarrollador. (Si no se abre, vaya manualmente a Ajustes -> Sistema -> Información -> pulse "Compilación" 7 veces).
2. **Activar Depuración USB**: Dentro de las Opciones de Desarrollador, active la **Depuración USB** (en algunas TV boxes que no sean de DIGI, puede que necesite probar con la Depuración Inalámbrica).
3. **Auto-Otorgar**: Vuelva a la app y pulse el segundo botón. La aplicación usará un bucle ADB local para otorgarse automáticamente los permisos `WRITE_SECURE_SETTINGS` y `ACCESS_RESTRICTED_SETTINGS`.

Una vez otorgados, la aplicación se configura sola. Cambie de idioma con `Ctrl + Espacio` o `Win / Meta`.

**Método de Respaldo (Por PC):**
Si el método automático falla en su dispositivo, puede otorgar los permisos manualmente usando un PC a través de ADB:
```bash
adb shell pm grant com.kostyamat.r2r_q android.permission.WRITE_SECURE_SETTINGS
adb shell appops set com.kostyamat.r2r_q ACCESS_RESTRICTED_SETTINGS allow
```

### 3. Enlaces a la Documentación
* [**Guía de Distribuciones de Teclado (JSON)**](docs/KEYLAYOUTS.md) - Aprenda a crear mapas de teclado personalizados.
* [**Notas Técnicas del Desarrollador**](docs/DEVELOPERS_NOTE.md) - Arquitectura e interacción entre componentes.
* [**Licencia MIT**](LICENSE) - Licencia del proyecto.

Autor: **Kostyantyn Matviyevskyy aka kostyamat**
