# Audio on Android

The game's output converts the six Xbox channels to stereo, at 48 kHz. The SDL
pump requests blocks of 256 samples every 5.33 ms and keeps a reserve of 12 blocks
(64 ms). The output can consume several blocks per request without imposing that
bursty pace on the game's audio server. The queue lock is held
only to take out or return buffers, which lets the producer refill the queue during
the conversion and the submission to SDL.

The settings `audio_sdl_bomba = true` and `audio_sdl_bomba_cola = 12` are the Android
defaults. `audio_maxqframes` stays at 12. The reserve adds
latency and absorbs short delays; longer loads can still drain the queue.
The `[audio] SDL en 10 s` log line counts delivered blocks and silences caused by
missing data. It does not detect silences that already come inside a game mix.

Android's stereo mix keeps its floating-point peaks until the limiter.
Clipping before limiting destroyed those peaks and could alter the stereo balance
at high volumes. The limiter keeps both channels linked, limits to 0.97
and recovers gain in about 80 ms. The recovery uses the sample rate of the source,
including in the 44.1 kHz movies. The other outputs keep the clipping
they already used.

The movies have their own SDL track, decoded with FFmpeg. WMA Pro and
WMA v2 are supported: the `ealogo` file of the Spanish PAL edition uses WMA v2,
while the other intros use WMA Pro. While the native track is playing, the output
of the game's mixer is muted to avoid overlap and noise. That mix is still
consumed and its buffers returned. The game's output is restored when the
track runs empty, when the movie is destroyed or after 800 ms without frame requests.
The log shows when it is muted and restored.

The test `tools/tests/audio_output_test.cpp` checks the conversion of the Xbox
channels, peaks above the previous headroom, the stereo balance after limiting, that
soft sounds pass through, continuity between blocks and recovery at 44.1 and
48 kHz. It was run on ARM64 Android; the Release build also passed.
To check playback, listen to an intro, skip a cutscene and
start a race; review the counters and the restoring of the output.

The container's WAVE headers also give FFmpeg the bitrate and the bit depth of the
source. The EA logo uses WMA v2 at 192 kb/s: with the default bitrate,
26 of its 28 packets failed to decode.

The fix for the intros is in the Android linking: `libmain` and `rexruntime`
each contain FFmpeg and its private FFT tables. The C symbols were already
hidden, but the NEON assembly functions were exported from the runtime.
As a result, the player initialized its own tables and called functions of the runtime,
which read other tables that were not initialized yet. The symbols of
both static FFmpeg archives are hidden with `--exclude-libs` in the two libraries.
This keeps NEON and avoids depending on the order in which sounds are played.

`python tools/tests/android_ffmpeg_bindings_test.py <APK>` checks that the two
libraries neither import nor export FFmpeg's internal DSP. The test fails with
the previous APK and passes with the fixed one. On the S25 Ultra, the capture of the
first five seconds of the PSA and of the EA logo matches exactly the
same decoding done outside the game (maximum error 0). The user confirmed that
the intros now sound right. The temporary audio capture was removed from the final APK.
