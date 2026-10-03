# Graphics diagnostic: Galaxy A55

Local test of 30 September 2026. Fix included in v0.3.4.

## Device and symptoms

- Samsung SM-A556E, Android 16, Samsung Xclipse530 GPU.
- Vulkan 1.3.279, SamsungProprietary 24.0.539 driver.
- The user reports good performance, but wrong textures and colors.
- ADB screenshots show blocks and smudges in the reflections on the car body,
  both in the menu and in a race. The geometry and the text are readable.
- The copy of the game files was verified by paths and sizes: 50
  files, 7,009,137,083 bytes. The SHA256 of default.xex, of
  nfsmw_shaders.nfsp and of the PSA video used in the sound tests was also checked.

## Capabilities checked on the phone

A standalone Vulkan tool queries the formats and creates images
with the SAMPLED and TRANSFER_DST usages, 512x512 and six mip levels.

| Format | Image supported | Memory alignment |
| --- | --- | --- |
| RGBA8, RGB10A2 | Yes | 65536 bytes |
| BC1, BC2, BC3 | Yes | 65536 bytes |
| BC4, BC5 | No: VK_ERROR_FORMAT_NOT_SUPPORTED | N/A |
| RG16F, RGBA16F | Yes | 65536 bytes |

`textureCompressionBC` is false, but BC1/2/3 are available individually.
All BC compression should not be disabled based on that flag alone.
The absence of BC4/5 does not prove that it causes this scene: it still has to
be confirmed that the game uses them in the affected draws.

The tested images neither require nor prefer a dedicated allocation. No
difference was found between their alignment and the pool's 64 KB unit.

## First test: memory dependencies

The native renderer keeps the images in GENERAL, and the passes had no
explicit dependencies with the copies and reads that follow. The upload and
work commands are submitted together, but submission order does not replace
memory dependencies.

External input and output dependencies are added to the pass, one dependency
before the uploads for the reads/writes of earlier submissions, and another
at the end of the uploads to publish the textures and the reflection faces.
The `nfsmw_nativo_sincronizacion_gpu` setting allows comparing with the previous
path; it is a startup setting and requires restarting the game. It does not modify the game files.

Reference: [Khronos synchronization examples](https://docs.vulkan.org/guide/latest/synchronization_examples.html).

## Result

The APK built, was installed over v0.3.3 and started on the A55 with the
synchronization setting enabled. The next screenshot showed the car body without
the smudges of the previous test. The user confirmed: "now it does work fine"
and asked for the fix to be released.

The FFmpeg internal bindings check still passes. Neither the audio nor the
game's shaders were modified. The functional test of v0.3.4 was done on the
Galaxy A55; the Galaxy S25 Ultra had been checked with v0.3.3. The cost of
these dependencies has not been measured on every driver, and compatibility
with other GPU models has not been validated.
