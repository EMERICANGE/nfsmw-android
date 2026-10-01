# Tester diagnostics (since build 0.3.5)

The launcher includes **Enviar crash o log** (Send crash or log). The phone
prepares a local ZIP with the app version, model, Android version, GPU/Vulkan,
launcher settings, recent logs of this app and the crash history that Android
lets apps query. When available, it includes the trace of a native crash or
ANR. It also keeps the last Java exception.

- **Correo** (Email) opens the share sheet for the ZIP with the recipient
  `daniebatuani@gmail.com`, a subject and an initial text. Pick your email app,
  describe what happened and tap Send.
- **GitHub** asks you to save the ZIP and opens a new issue at
  https://github.com/codepdbh/nfsmw-android/issues/new with the basic data
  filled in. Sign in if needed, describe how to reproduce the failure and
  attach the saved ZIP before posting the issue.
- **Guardar ZIP** (Save ZIP) saves the report without opening email or GitHub.

If the game closes, open the launcher again and use the button before trying
several new sessions. If it hangs, close the app from Android, open it again
and generate the report. State the game edition and the steps to reproduce the
problem. The log does not always contain a trace: it depends on whether Android
keeps it and on how the process ended.

The logs are trimmed, keeping their beginning and end. The app does not attach
game files, saves, phone identifiers or logs of other apps. The ZIP is not sent
automatically. GitHub does not accept attachments from a link: the tester
attaches the file in the form.

## Redmi Note 8 review

On the device connected on 1 October 2026, the following was checked:

- Adreno 610 GPU, Qualcomm driver dated 25/09/2020, Vulkan 1.1.128.
- `shaderInt64 = false`; it does not advertise `VK_EXT_descriptor_indexing`, and
  `runtimeDescriptorArray`, `descriptorBindingPartiallyBound`,
  `descriptorBindingSampledImageUpdateAfterBind` and
  `descriptorBindingUpdateUnusedWhilePending` are missing.
- It advertises buffer device address as an extension, but the SDK's native
  interface currently enables it together with the rest of the Vulkan 1.2 features.
- BC1, BC2, BC3, BC4 and BC5 are not supported as sampled images.
- The executable and the shader library match, by SHA256, the PAL Spain copy
  used in the earlier tests.
- Startup gets as far as creating Vulkan, but the renderer logs (no shaderInt64,
  so nothing is drawn): `C6: el dispositivo Vulkan no tiene shaderInt64: no se dibuja`.
- Kernel 4.14 ignores `MAP_FIXED_NOREPLACE` and can return a different address.
  The runtime accepted that address as a correct fixed reservation, which left
  the reused guest stack protected. This is fixed by checking the returned
  address, discarding the displaced reservation and enabling the original region
  when it is already reserved. The native test failed before the fix and passed
  after it on this phone: fixed commit, collisions and reuse of protected pages.
- An unhandled exception now follows Android's normal crash path; before, the
  handler returned and repeated the same faulting instruction without ending.
  The handled, delegated and fatal cases were verified on the phone.

The user's change to `-march=armv8-a` is kept: it allows generating code for
ARMv8.0 processors, but it does not solve the driver's limitations.
The launcher now checks the features required by the native renderer and
offers **Probar compatibilidad** (Try compatibility) and access to the report.
BC4/5 are not required in this check, because the tested Galaxy A55 supports
BC1/2/3 and lacks BC4/5.

## Trying the compatibility mode

In the launcher, select **Renderizador → Compatibilidad · experimental**
(Renderer → Compatibility · experimental), or accept **Probar compatibilidad**
when missing native features are detected. Tap **Jugar** (Play). This mode
does not need to generate the native renderer's shader library.

It uses the Xenos backend with conventional descriptors, FBO render targets
and conversion of the unsupported BC textures. It disables the native
replacements for D3D, materials, matrices, scenery and rendering without tiling
to keep the game's original path. Shader compilation is synchronous.
These choices prioritize getting the game to start and can cause pauses.

With this mode and the memory fix, the tested Redmi plays the startup videos
and the user confirms that it gets as far as driving and that the car steers
with the joystick, although it runs very slowly. Long GPU waits and later
black screens were also observed: support is still experimental.
It has not been validated yet in a full race or on Helio G99/G200.
Testers with those devices should attach their ZIP, stating the mode they
used and the last screen that worked.

The touch joystick can be used even with **Inclinar** (Tilt) enabled:
while the finger is held down, the joystick is in control; on release, tilt
takes over again. This fixes the tilt setting blocking the drag.
