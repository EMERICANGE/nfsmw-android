#include <jni.h>

#include <rex/input/sdl/sdl_input_driver.h>

extern "C" JNIEXPORT void JNICALL
Java_com_nfsmw_android_GameActivity_nativeSetTouchState(JNIEnv*, jclass, jint buttons,
                                                        jint steering, jint brake,
                                                        jint throttle) {
  rex_sdl_set_touch_gamepad_state(
      static_cast<uint16_t>(buttons), static_cast<int16_t>(steering),
      static_cast<uint8_t>(brake), static_cast<uint8_t>(throttle));
}
