#!/usr/bin/env sh
set -eu
command -v adb >/dev/null 2>&1 || { echo 'adb was not found on PATH.' >&2; exit 1; }
# logcat compares tags exactly. NFSMW is the launcher (Java) and NFSMW-rex the runtime and the game (a copy of the
# native log, info and above). SDL and SDL/<category> come from SDLActivity and native SDL. AndroidRuntime shows Java
# crashes; DEBUG (the tombstone) and libc ("Fatal signal") show native ones.
adb logcat -v time -s NFSMW NFSMW-rex SDL SDL/APP SDL/ERROR SDL/ASSERT SDL/SYSTEM SDL/AUDIO SDL/VIDEO SDL/INPUT \
    AndroidRuntime DEBUG libc
