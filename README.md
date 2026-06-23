<p align="center">
  <a href="https://www.paypal.com/paypalme/kostyamat">
    <img src="https://thumbs.dreamstime.com/b/cute-kawaii-coffee-mug-character-smiling-steam-isolated-white-adorable-cartoon-happy-face-decorative-lace-pattern-401912575.jpg" alt="Buy me a coffee" width="200"/>
    <br>
    <strong>If you found my work helpful, buy me a coffee! It keeps me motivated ☕</strong>
  </a>
</p>

---

# DIGI Keyboard (ZTE R2A Q)

An Android TV input utility designed specifically to map, translate, and cycle physical keyboard layouts on provider-restricted TV boxes.

[**English**](#english-documentation) | [**Español**](#documentación-en-español)

---

## English Documentation

### 1. Project Purpose & Context
This application was created for provider TV boxes distributed by the Spanish-Romanian telecom operator **DIGI** (specifically the **ZTE R2A** box running **Android TV 14 / API 34**). 

The primary use case is configuring the TV box as a video source (monitors, projectors, TVs) for interactive kiosk systems, digital signage, billboards, or public displays utilizing **physical keyboards**.

#### The Problems Solved:
* **The "q/Q" Key Grab**: The operator's ROM intercepts the physical `q` / `Q` key at a low system level, hardcoding it to launch their own VOD online streaming application. As a result, the letter `q` cannot be typed into any normal input field on the system.
* **Missing OS Settings**: The firmware has completely hidden or stripped out the settings menu for selecting physical keyboard layouts and switching layouts.

#### How It Works:
This app combines a low-level **Accessibility Service** that intercept key events (re-mapping the blocked `q` key and translating other layouts) with a zero-latency **Proxy Input Method (IME)** that commits translated characters directly to the active text field.

#### Universal Compatibility & Custom Forks:
* **Universal Android TV Support**: Although the application was originally created as a workaround (a custom "crutch") for a specific provider TV box (ZTE R2A), it contains absolutely nothing proprietary or device-dependent. It can be used without any restrictions on any other Android TV box.
* **Free Alternative**: It serves as a free, open-source replacement for various paid physical keyboard mappers available on the Play Store.
* **Base for Custom Projects**: The project is open-source and can be used as a foundation for your own custom keyboard layout tools or mapping applications, provided that you credit the original author (**Kostyantyn Matviyevskyy aka kostyamat**).

### 2. Quick Setup & ADB Activation
Because Android TV 14 (API 34) enforces strict security limitations, automatic activation via the wizard requires granting special permissions via ADB:

1. **Grant Secure Settings Access** (Required for the app to enable services automatically):
   ```bash
   adb shell pm grant com.kostyamat.r2r_q android.permission.WRITE_SECURE_SETTINGS
   ```
2. **Bypass Restricted Settings** (Required if installing the release APK from a USB flash drive to prevent the Accessibility toggle from being greyed out):
   ```bash
   adb shell appops set com.kostyamat.r2r_q ACCESS_RESTRICTED_SETTINGS allow
   ```

Once the ADB commands are executed, open the app, complete the onboarding steps, and choose your active layouts. You can cycle through active layouts instantly by pressing `Ctrl + Space`.

### 3. Project Documentation Links
* [**Key Layouts Guide (JSON Customization)**](docs/KEYLAYOUTS.md) - Learn how to build and import custom keyboard layouts.
* [**Developer's Technical Notes**](docs/DEVELOPERS_NOTE.md) - Context, architecture overview, and component interaction diagrams for developers.
* [**MIT License**](LICENSE) - Project license file.

Author: **Kostyantyn Matviyevskyy aka kostyamat**

---

## Documentación en Español

### 1. Propósito y Contexto del Proyecto
Esta aplicación fue desarrollada para los decodificadores (TV boxes) distribuidos por el operador de telecomunicaciones **DIGI** (específicamente el modelo **ZTE R2A** con **Android TV 14 / API 34**).

El caso de uso principal es configurar la TV box como fuente física en monitores, proyectores o pantallas para sistemas de quioscos interactivos, señalización digital o vallas publicitarias que utilicen **teclados físicos**.

#### Problemas que Resuelve:
* **El Bloqueo de la Tecla "q/Q"**: La ROM del operador intercepta la tecla física `q` / `Q` a nivel de sistema para abrir por defecto su propia aplicación de streaming VOD. Como consecuencia, la letra `q` no se podía escribir en ningún campo de texto del dispositivo.
* **Falta de Ajustes de Sistema**: El firmware ha eliminado por completo los menús de configuración para seleccionar la distribución del teclado físico y cambiar de idioma.

#### Cómo Funciona:
La aplicación combina un **Servicio de Accesibilidad** de bajo nivel que filtra los eventos de las teclas (recuperando la tecla `q` bloqueada y traduciendo otras distribuciones) con un **Método de Entrada Proxy (IME)** sin interfaz gráfica que introduce los caracteres traducidos al instante y sin retraso.

#### Compatibilidad Universal y Bifurcaciones (Forks):
* **Soporte Universal para Android TV**: Aunque la aplicación fue creada originalmente como una solución alternativa (un "parche" o "muleta") para un decodificador específico (ZTE R2A), no contiene ningún tipo de código propietario o dependiente de ese hardware en particular. Por lo tanto, se puede utilizar sin ninguna limitación en cualquier otra TV box con Android TV.
* **Alternativa Gratuita**: Funciona como un reemplazo gratuito y de código abierto para diversos mapeadores de teclado físico de pago disponibles en la tienda de aplicaciones.
* **Base para Proyectos Propios**: El proyecto es de código abierto y puede utilizarse como base o punto de partida para sus propios proyectos, siempre que se mencione y enlace al autor original (**Kostyantyn Matviyevskyy aka kostyamat**).

### 2. Configuración Rápida y Activación por ADB
Debido a las estrictas políticas de seguridad de Android TV 14 (API 34), la activación automática a través de la aplicación requiere otorgar permisos especiales mediante comandos ADB:

1. **Otorgar acceso a los Ajustes Seguros** (Requerido para que la app autoconfigure los servicios):
   ```bash
   adb shell pm grant com.kostyamat.r2r_q android.permission.WRITE_SECURE_SETTINGS
   ```
2. **Permitir Ajustes Restringidos** (Requerido si se instala el APK de producción desde una memoria USB para evitar que el interruptor de Accesibilidad aparezca bloqueado en gris):
   ```bash
   adb shell appops set com.kostyamat.r2r_q ACCESS_RESTRICTED_SETTINGS allow
   ```

Una vez ejecutados los comandos, abra la app, complete los pasos indicados y elija sus idiomas. Puede alternar entre distribuciones activas en cualquier momento presionando `Ctrl + Espacio`.

### 3. Enlaces a la Documentación del Proyecto
* [**Guía de Distribuciones de Teclado (JSON)**](docs/KEYLAYOUTS.md) - Aprenda cómo crear e importar mapas de teclado personalizados.
* [**Notas Técnicas del Desarrollador**](docs/DEVELOPERS_NOTE.md) - Contexto de firmware, arquitectura y diagrama de interacción entre componentes.
* [**Licencia MIT**](LICENSE) - Licencia del proyecto.

Autor: **Kostyantyn Matviyevskyy aka kostyamat**
