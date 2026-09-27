# Profiles for profile guided optimization

One folder per edition with the GCC profile (`.gcda`) the released NROs were built with:

| Folder | Edition |
|---|---|
| `pal_es` | PAL Spanish (the default tree `app/`) |
| `pal_en` | PAL English |
| `pal_de` | PAL German |
| `pal_it` | PAL Italian |
| `usa` | NTSC-U |
| `jpn` | NTSC-J |

`pal_es` was recorded on the console with an instrumented build (`-DNFSMW_PGO=generar`) while playing. The
others are that same profile translated to the addresses of each edition by `tools/editions/pgo/traducir_perfil.py`:
the recompiled functions are named after their guest address (`__imp__sub_823B5A40`), so the name hash that GCC uses
to find a function in the profile changes from one edition to another.

The files contain counters and hashes of function names, nothing from the game. Each file name is the object path
relative to the build folder with `#` instead of `/`, so they are found as long as the build folder layout is the same.
See [docs/building.md](../docs/building.md) and [docs/toolchain.md](../docs/toolchain.md).
