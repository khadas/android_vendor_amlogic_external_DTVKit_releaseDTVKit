#include <jni.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <string.h>

#ifdef PLATFORM_BROADCOM
#include <cutils/native_handle.h>
#include <bcmsideband.h>
#include <bcmsidebandplayerfactory.h>
static struct bcmsideband_ctx *context = NULL;
#endif
static jboolean set = JNI_FALSE;
static int surface_x = 0;
static int surface_y = 0;
static int surface_width = 1920;
static int surface_height = 1080;

#ifdef PLATFORM_BROADCOM
static void onRectangleUpdated(void *context, unsigned int x, unsigned int y, unsigned int width, unsigned int height)
{
   __android_log_print(ANDROID_LOG_INFO, "DTVKitSource", "onRectangleUpdated: x %d, y %d, width %d, height %d\n",
      x, y, width, height);
   surface_x = x;
   surface_y = y;
   surface_width = width;
   surface_height = height;
}
#endif

extern "C" JNIEXPORT jboolean JNICALL Java_org_dtvkit_inputsource_Platform_setNativeSurface(JNIEnv *env, jclass thiz, jobject surface)
{
   if (set != JNI_TRUE)
   {
      ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
      #ifdef PLATFORM_BROADCOM
      __android_log_print(ANDROID_LOG_INFO, "DTVKitSource", "setNativeSurface: Broadcom platform selected (width %d, height %d)\n",
         ANativeWindow_getWidth(window), ANativeWindow_getHeight(window));
      int index = 0, videoId = -1, audioId = -1, surfaceId = -1;
      if ((context = libbcmsideband_init_sideband(index, window, &videoId, &audioId, &surfaceId, &onRectangleUpdated)))
      {
         set = JNI_TRUE;
      }
      #else
      __android_log_print(ANDROID_LOG_WARN, "DTVKitSource", "setNativeSurface: no platform selected (width %d, height %d)\n",
         ANativeWindow_getWidth(window), ANativeWindow_getHeight(window));
      ANativeWindow_setBuffersGeometry(window, surface_width, surface_height, WINDOW_FORMAT_RGBA_8888);
      ANativeWindow_Buffer buffer;
      if (ANativeWindow_lock(window, &buffer, 0) == 0)
      {
         memset(buffer.bits, 0x00, surface_width * surface_height * 4);
         ANativeWindow_unlockAndPost(window);
      }
      set = JNI_TRUE;
      #endif
   }

   return set;
}

extern "C" JNIEXPORT void JNICALL Java_org_dtvkit_inputsource_Platform_unsetNativeSurface(JNIEnv* env, jclass thiz) {
   if (set == JNI_TRUE)
   {
      #ifdef PLATFORM_BROADCOM
      libbcmsideband_release(context);
      context = NULL;
      #endif
      set = JNI_FALSE;
   }
}

extern "C" JNIEXPORT jint JNICALL Java_org_dtvkit_inputsource_Platform_getNativeSurfaceX(JNIEnv *env, jobject instance) {
   return surface_x;
}

extern "C" JNIEXPORT jint JNICALL Java_org_dtvkit_inputsource_Platform_getNativeSurfaceY(JNIEnv *env, jobject instance) {
   return surface_y;
}

extern "C" JNIEXPORT jint JNICALL Java_org_dtvkit_inputsource_Platform_getNativeSurfaceWidth(JNIEnv *env, jobject instance) {
   return surface_width;
}

extern "C" JNIEXPORT jint JNICALL Java_org_dtvkit_inputsource_Platform_getNativeSurfaceHeight(JNIEnv *env, jobject instance) {
   return surface_height;
}
