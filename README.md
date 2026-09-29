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
| APK Android `arm64-v8a` | Compilado (`app-debug.apk`) |
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

El APK se genera en `android/app/build/outputs/apk/debug/app-debug.apk`. Para instalarlo en un dispositivo conectado, ejecuta `adb install -r` con esa ruta.

Al abrir la app, selecciona la carpeta extraída que contiene `default.xex`, `NFS/` y `Movies/`. Se copiará a `files/nfsmw/game_root` dentro del almacenamiento privado de la app. Cuando aparezca **Play**, tócala para abrir el juego en horizontal. La primera ejecución en un teléfono puede revelar incompatibilidades de GPU o rendimiento que todavía no se han podido medir.

Los archivos del juego y las fuentes generadas a partir del XEX permanecen locales y están excluidos de Git. El APK tampoco se versiona.

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
