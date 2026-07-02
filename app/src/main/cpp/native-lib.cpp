#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/bitmap.h>
#include <vector>
#include <stdlib.h>
#include "libretro.h"

#define LOG_TAG "RetroPlayNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM *g_jvm = NULL;
static jobject g_obj = NULL;
static jintArray g_pixel_array = NULL;
static int g_pixel_array_size = 0;

static JNIEnv* getEnv() {
    JNIEnv *env;
    int status = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (status < 0) {
        status = g_jvm->AttachCurrentThread(&env, NULL);
        if (status < 0) return NULL;
    }
    return env;
}

// Libretro callback implementations
static bool env_callback(unsigned cmd, void *data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
            enum retro_pixel_format fmt = *(const enum retro_pixel_format *)data;
            LOGI("Pixel format: %d", fmt);
            return true;
        }
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
            *(bool*)data = true;
            return true;
        default:
            return false;
    }
}

static void video_refresh_callback(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (!data) return;

    JNIEnv *env = getEnv();
    if (!env || !g_obj) return;

    jclass cls = env->GetObjectClass(g_obj);
    jmethodID mid = env->GetMethodID(cls, "onNativeVideoFrame", "([III)V");

    int size = width * height;

    // Reuse or allocate pixel array to avoid GC pressure
    if (g_pixel_array == NULL || g_pixel_array_size < size) {
        if (g_pixel_array) env->DeleteGlobalRef(g_pixel_array);
        jintArray localArray = env->NewIntArray(size);
        g_pixel_array = (jintArray)env->NewGlobalRef(localArray);
        g_pixel_array_size = size;
        env->DeleteLocalRef(localArray);
    }

    jint *pixels = env->GetIntArrayElements(g_pixel_array, NULL);

    // Convert RGB565 to ARGB8888
    const uint16_t *src = (const uint16_t*)data;
    for (int i = 0; i < size; i++) {
        uint16_t p = src[i];
        uint32_t r = ((p >> 11) & 0x1F) << 3;
        uint32_t g = ((p >> 5) & 0x3F) << 2;
        uint32_t b = (p & 0x1F) << 3;
        pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    env->ReleaseIntArrayElements(g_pixel_array, pixels, 0);
    env->CallVoidMethod(g_obj, mid, g_pixel_array, (jint)width, (jint)height);
}

static void audio_sample_callback(int16_t left, int16_t right) {}

static size_t audio_sample_batch_callback(const int16_t *data, size_t frames) {
    return frames;
}

static void input_poll_callback(void) {}

static int16_t input_state_callback(unsigned port, unsigned device, unsigned index, unsigned id) {
    if (port != 0 || device != RETRO_DEVICE_JOYPAD) return 0;

    JNIEnv *env = getEnv();
    if (!env || !g_obj) return 0;

    jclass cls = env->GetObjectClass(g_obj);
    jmethodID mid = env->GetMethodID(cls, "getNativeInputState", "(I)S");
    return env->CallShortMethod(g_obj, mid, (jint)id);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_emulator_NativeBridge_loadCore(JNIEnv *env, jobject obj, jstring corePath) {
    if (g_obj) env->DeleteGlobalRef(g_obj);
    g_obj = env->NewGlobalRef(obj);
    env->GetJavaVM(&g_jvm);

    retro_set_environment(env_callback);
    retro_set_video_refresh(video_refresh_callback);
    retro_set_audio_sample(audio_sample_callback);
    retro_set_audio_sample_batch(audio_sample_batch_callback);
    retro_set_input_poll(input_poll_callback);
    retro_set_input_state(input_state_callback);

    retro_init();

    // Set pixel format to RGB565 as preferred for mobile performance
    enum retro_pixel_format fmt = RETRO_PIXEL_FORMAT_RGB565;
    env_callback(RETRO_ENVIRONMENT_SET_PIXEL_FORMAT, &fmt);

    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_emulator_NativeBridge_loadGame(JNIEnv *env, jobject obj, jstring gamePath) {
    const char *path = env->GetStringUTFChars(gamePath, NULL);
    struct retro_game_info info = {0};
    info.path = path;

    bool success = retro_load_game(&info);

    env->ReleaseStringUTFChars(gamePath, path);
    return success ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_emulator_NativeBridge_runFrame(JNIEnv *env, jobject obj) {
    retro_run();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_emulator_NativeBridge_reset(JNIEnv *env, jobject obj) {
    retro_reset();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_emulator_NativeBridge_stop(JNIEnv *env, jobject obj) {
    retro_unload_game();
    retro_deinit();

    if (g_obj) {
        env->DeleteGlobalRef(g_obj);
        g_obj = NULL;
    }
    if (g_pixel_array) {
        env->DeleteGlobalRef(g_pixel_array);
        g_pixel_array = NULL;
        g_pixel_array_size = 0;
    }
}
