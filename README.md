<div align="center">

# NFSMW Android Evolved

**Need for Speed: Most Wanted (2005) para Android ARM64**

APK nativo · Vulkan · controles táctiles · generación local de shaders

[Descargar la última versión](https://github.com/codepdbh/nfsmw-android/releases/latest)

</div>

Este proyecto adapta a Android el trabajo de recompilación de [nfsmw-nx](https://github.com/StevensND/nfsmw-nx) y el SDK [ReXGlue](https://github.com/rexglue/rexglue-sdk). La aplicación usa código C++ recompilado, SDL3 y el renderizador nativo sobre Vulkan. No emula una Xbox 360 ni incluye los archivos del juego.

## Requisitos

- Android 8 o posterior, dispositivo ARM64 con Vulkan.
- Una copia propia del juego extraída, con `default.xex`, `NFS/` y `Movies/`.
- Espacio libre suficiente para guardar la copia del juego en la memoria interna.

## Instalación y primer inicio

1. Descarga e instala el APK desde [Releases](https://github.com/codepdbh/nfsmw-android/releases/latest).
2. Abre la app y permite el acceso a archivos que solicita Android para guardar el juego en la memoria compartida.
3. Copia la carpeta extraída del juego a `Memoria interna/nsfmw-androidevolved/`, o usa **Seleccionar carpeta** en la app para importarla.
4. Pulsa **Jugar**. En el primer inicio, la app genera `nfsmw_shaders.nfsp` localmente a partir de tu copia del juego y muestra el avance. La generación puede tardar varios minutos y solo hace falta una vez.
5. Al terminar, se abre el juego. Los siguientes inicios usan la biblioteca generada.

La carpeta debe quedar así:

```text
Memoria interna/nsfmw-androidevolved/
├── default.xex
├── NFS/
├── Movies/
└── nfsmw_shaders.nfsp   (generado por la app)
```

No descargues ni compartas archivos del juego. La biblioteca de shaders se genera en el dispositivo desde los archivos locales de tu copia.

## Launcher, ajustes y controles

En el launcher puedes cambiar resolución interna, límite de FPS, antialiasing, sombras, reflejos del coche y del asfalto, resplandor del cielo y filtro de imagen. La app también guarda ajustes de controles y formato de pantalla.

El juego se abre en horizontal. La superposición táctil incluye dirección, botones de acción, START, freno y acelerador. Desde el editor de controles puedes mover y redimensionar botones, ocultarlos y ajustar su opacidad. También se admiten mandos Bluetooth y USB.

## Compilar

Requisitos: Android SDK, NDK `28.2.13676358`, JDK 17 o posterior y PowerShell.

```powershell
.\build_android.ps1
```

El APK Release se genera en `android/app/build/outputs/apk/release/app-release.apk`. La compilación usa optimización nativa Release y está configurada para `arm64-v8a`.

## Proyecto y licencias

- `android/`: launcher, integración SDL y build Android.
- `app/`, `sdk/`: aplicación recompilada y ReXGlue.
- `docs/`: notas del port y compilación.

Los archivos del juego y el código generado desde `default.xex` se mantienen fuera de Git. Consulta [`LICENSE`](LICENSE) y [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) para las licencias. Proyecto de aficionados, sin afiliación con Electronic Arts; “Need for Speed” es una marca de Electronic Arts Inc.