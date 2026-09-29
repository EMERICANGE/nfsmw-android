<div align="center">

# NFSMW Android Evolved

**Port de Need for Speed: Most Wanted (2005), versión Xbox 360, a Android ARM64**

Recompilación estática para Android · C++ nativo · Vulkan · APK

<img alt="Android ARM64" src="https://img.shields.io/badge/Android-ARM64%20%7C%20API%2028%2B-3DDC84?logo=android&logoColor=white">
<img alt="Vulkan" src="https://img.shields.io/badge/Graphics-Vulkan-AC75C1?logo=vulkan&logoColor=white">
<img alt="Estado" src="https://img.shields.io/badge/Estado-en%20desarrollo-orange">

</div>

Este proyecto adapta a Android el trabajo de recompilación de [nfsmw-nx](https://github.com/StevensND/nfsmw-nx) y el SDK [ReXGlue](https://github.com/rexglue/rexglue-sdk). La meta es ejecutar código AArch64 recompilado de la versión Xbox 360 con un backend Vulkan nativo de Android.

> **Estado actual:** la app Android ARM64 compila e instala, carga `libnfsmw_android.so`, registra el ciclo de vida, prepara almacenamiento privado e inicializa y consulta Vulkan en el dispositivo. El runtime completo del juego todavía no está conectado: aún no inicia menú ni partida.

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
| Proyecto Gradle y APK `arm64-v8a` | Implementado |
| Biblioteca JNI `libnfsmw_android.so` | Implementada |
| Logs nativos y ciclo de vida Android | Implementados |
| Detección de dispositivo/cola Vulkan y superficie de ventana | Verificado en dispositivo Adreno |
| Importación de carpeta mediante selector SAF | Implementada; falta validar el selector en UI |
| Copia local de carpeta extraída a almacenamiento privado | Hecha en el dispositivo conectado; no incluida en Git |
| ReXGlue SDK Android | UI/Vulkan y `librexruntime.so` compilados en ARM64; falta integrarlo al APK |
| Arranque del juego, primer frame, controles y audio | Pendiente |

El registro de cambios técnicos está en [`docs/ANDROID_STATUS.md`](docs/ANDROID_STATUS.md) y el diseño del port en [`docs/ANDROID_PORT_PLAN.md`](docs/ANDROID_PORT_PLAN.md).

## Compilar

Requisitos: Android Studio/SDK, NDK `28.2.13676358`, JDK 21 y PowerShell o Bash.

```powershell
.\tools\build_android.ps1 -Variant Debug
```

El APK se genera bajo `android/app/build/outputs/apk/`. Para instalarlo en un dispositivo conectado, usa el script de compilación con `-Install`.

La app guarda sus datos en su directorio privado. Puede importar una carpeta extraída con `default.xex`, `NFS/` y `Movies/` mediante el selector de Android. La carga del juego todavía no está implementada.

## Estructura

- `android/` — app Android, JNI y configuración NDK/Gradle.
- `sdk/` — ReXGlue y las adaptaciones de plataforma en curso.
- `docs/` — plan del port y estado verificado.
- `tools/` — scripts de compilación y diagnóstico.

## Archivos del juego y licencia

Este repositorio **no contiene** `default.xex`, imágenes ISO, películas, audio, datos ni assets del juego. Aporta tu propia copia legal localmente. Las rutas de datos del juego y los productos generados a partir de `default.xex` están excluidos por `.gitignore`; no compartas esos archivos.

Proyecto de aficionados, sin afiliación con Electronic Arts. “Need for Speed” y “Need for Speed: Most Wanted” son marcas de Electronic Arts Inc. Consulta [`LICENSE`](LICENSE) y [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) para las licencias del código y dependencias.

## Agradecimientos

[StevensND/nfsmw-nx](https://github.com/StevensND/nfsmw-nx), [ReXGlue](https://github.com/rexglue/rexglue-sdk), [Xenia](https://xenia.jp), [SDL](https://www.libsdl.org/) y Khronos Vulkan.
