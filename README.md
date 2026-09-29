<div align="center">

# NFSMW Android Evolved

**Port de Need for Speed: Most Wanted (2005), versión Xbox 360, a Android ARM64**

Recompilación estática para Android · C++ nativo · Vulkan · APK

<img alt="Android ARM64" src="https://img.shields.io/badge/Android-ARM64%20%7C%20API%2028%2B-3DDC84?logo=android&logoColor=white">
<img alt="Vulkan" src="https://img.shields.io/badge/Graphics-Vulkan-AC75C1?logo=vulkan&logoColor=white">
<img alt="Estado" src="https://img.shields.io/badge/Estado-en%20desarrollo-orange">

</div>

Este proyecto adapta a Android el trabajo de recompilación de [nfsmw-nx](https://github.com/StevensND/nfsmw-nx) y el SDK [ReXGlue](https://github.com/rexglue/rexglue-sdk). La app ya genera un APK ARM64 que incluye el código recompilado localmente desde el XEX, ReXGlue, SDL3 y el backend gráfico Xenos sobre Vulkan.

> **Estado actual:** el APK de depuración se compiló correctamente y contiene las bibliotecas necesarias para iniciar la app SDL y el juego. La importación a almacenamiento privado y el arranque del juego dentro de esta nueva Activity todavía esperan una prueba en el teléfono; aún no se ha confirmado el primer menú ni una carrera.

## Enfoque

```text
default.xex del usuario → ReXGlue → C++ recompilado → Android ARM64
                                                      ↓
                                      SDL3 / Android · Vulkan · APK
```

No utiliza emulación de Xbox 360, Xenia, Wine, Winlator, Box64 ni traducción JIT. El backend Vulkan usa el driver del sistema Android; Turnip queda como opción futura, no como requisito.

## Progreso

| Área | Estado |
|---|---|
| APK Android `arm64-v8a` | Release optimizado (`app-release.apk`: `-O3`, ThinLTO) |
| SDL3 + entrada de app en `libmain.so` | Integrados en el APK |
| ReXGlue runtime + plugin Xenos/Vulkan | Integrados en el APK |
| Código de juego recompilado desde el XEX local | Compilado; excluido de Git |
| Alineación de bibliotecas para páginas de 16 KiB | Verificada |
| Importador SAF a almacenamiento interno privado | Implementado; falta probar la pantalla en el teléfono |
| Primer inicio, menú y carrera en Android | Pendiente de prueba en el teléfono |
| Controles táctiles y audio | Pendiente de validar |

El registro de cambios técnicos está en [`docs/ANDROID_STATUS.md`](docs/ANDROID_STATUS.md) y el diseño del port en [`docs/ANDROID_PORT_PLAN.md`](docs/ANDROID_PORT_PLAN.md).

## Compilar

Requisitos: Android Studio/SDK, NDK `28.2.13676358`, JDK 17 o posterior y PowerShell o Bash.

```powershell
.\build_android.ps1
```

El APK se genera en `android/app/build/outputs/apk/release/app-release.apk` (Release: `-O3`, ThinLTO y ARMv8.2). Para instalarlo en un dispositivo conectado, ejecuta `adb install -r` con esa ruta. No uses la variante Debug para jugar: compila el código recompilado con `-O0` y es varias veces más lenta.

### Rendimiento: renderizador nativo y biblioteca de shaders

El APK usa el renderizador nativo de nfsmw-nx (`nfsmw_renderizador = "nativo"` en `android/app/src/main/assets/nfsmw.toml`), no la emulación de la GPU Xenos (1-3 FPS en carrera). Necesita `nfsmw_shaders.nfsp`, que se genera desde tu propio disco y no se distribuye:

```sh
git clone https://github.com/StevensND/nfsmw-nx-installer out/host-tools/installer
node tools/biblioteca_shaders.mjs out/host-tools/installer "<carpeta con default.xex y NFS/>" nfsmw_shaders.nfsp
```

(`tools/biblioteca_shaders.mjs` hace lo mismo que la página del instalador, con sus módulos WASM.) Copia el `.nfsp` en la carpeta del juego del teléfono, junto a `default.xex`. Para PAL España su SHA-256 debe ser `a27aea2318740f39c4e7fb31167f52a8c2aa4162ee5acc0962a79abaea41a76a`, el mismo que la versión de Switch.

Los ajustes se copian del APK a `files/nfsmw/user/nfsmw.toml`. Parten de la configuración publicada para Switch, con 1280x720 interno y límite de 60 FPS.

Al abrir la app, selecciona la carpeta extraída que contiene `default.xex`, `NFS/` y `Movies/`. Se copiará a `files/nfsmw/game_root` dentro del almacenamiento privado de la app. Cuando aparezca **Play**, tócala para abrir el juego en horizontal. La primera ejecución en un teléfono puede revelar incompatibilidades de GPU o rendimiento que todavía no se han podido medir.

Los archivos del juego y las fuentes generadas a partir del XEX permanecen locales y están excluidos de Git. El APK tampoco se versiona.

## Controles y opciones

- **Mando táctil completo**: stick de dirección analógico, cruceta, A/B/X/Y, LB/RB, BACK, START y pedales de gas y freno. El stick de cámara y L3/R3 están ocultos por defecto.
- **Editor**: toca el engranaje para mover los controles, cambiar su tamaño (pellizcando o con los botones), ocultarlos o restablecerlos. En **Ajustes** están la opacidad, la vibración, la dirección por inclinación y la imagen estirada.
- **Mandos Bluetooth/USB**: SDL los usa como jugador 1. Los controles táctiles se ocultan al usar el mando y vuelven al tocar la pantalla.
- **Launcher**: resolución interna (hasta 1920x1080), límite de FPS (30, 60, y 90/120 experimentales), antialiasing, sombras, reflejos, filtros de imagen y formato de imagen (estirada u original 16:9). La app se usa siempre en horizontal.

## Estructura

- `android/` — launcher Android, Activity SDL y configuración NDK/Gradle.
- `sdk/` — ReXGlue y las adaptaciones de plataforma en curso.
- `docs/` — plan del port y estado verificado.
- `tools/` — scripts de compilación y diagnóstico.

## Archivos del juego y licencia

Este repositorio **no contiene** `default.xex`, imágenes ISO, películas, audio, datos ni assets del juego. Aporta tu propia copia legal localmente. Las rutas de datos del juego y los productos generados a partir de `default.xex` están excluidos por `.gitignore`; no compartas esos archivos.

Proyecto de aficionados, sin afiliación con Electronic Arts. “Need for Speed” y “Need for Speed: Most Wanted” son marcas de Electronic Arts Inc. Consulta [`LICENSE`](LICENSE) y [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) para las licencias del código y dependencias.

## Agradecimientos

[StevensND/nfsmw-nx](https://github.com/StevensND/nfsmw-nx), [ReXGlue](https://github.com/rexglue/rexglue-sdk), [Xenia](https://xenia.jp), [SDL](https://www.libsdl.org/) y Khronos Vulkan.
