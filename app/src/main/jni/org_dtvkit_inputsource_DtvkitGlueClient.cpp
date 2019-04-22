/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#define LOG_TAG "dtvkit-jni"

#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <android_runtime/android_view_Surface.h>
#include <android/native_window.h>
#include <gui/Surface.h>
#include <gui/IGraphicBufferProducer.h>
#include <ui/GraphicBuffer.h>
#include <gralloc_usage_ext.h>
#include <hardware/gralloc1.h>
#include "amlogic/am_gralloc_ext.h"

#include <android/hidl/memory/1.0/IMemory.h>
#include <hidlmemory/mapping.h>
#include "org_dtvkit_inputsource_DtvkitGlueClient.h"

using namespace android;
using ::android::hidl::memory::V1_0::IMemory;
using ::android::sp;

sp<DTVKitClientJni> mpDtvkitJni;
static jmethodID notifySubtitleCallback;
static jmethodID notifyDvbCallback;
static jobject DtvkitObject;

static void sendSubtitleData(int width, int height, int dst_x, int dst_y, int dst_width, int dst_height, int size, uint8_t* data)
{
    ALOGD("callback sendSubtitleData data = %p", data);
    CallbackEnv sCallbackEnv(__func__);
    if (!sCallbackEnv.valid()) return;
    if (width != 0 || height != 0) {
        ScopedLocalRef<jbyteArray> array (sCallbackEnv.get(), sCallbackEnv->NewByteArray(size));
        if (!array.get()) {
            ALOGE("Fail to new jbyteArray sharememory addr");
            return;
        }
        sCallbackEnv->SetByteArrayRegion(array.get(), 0, size, (jbyte*)data);
        sCallbackEnv->CallVoidMethod(DtvkitObject, notifySubtitleCallback, width, height, dst_x, dst_y, dst_width, dst_height, array.get());
    } else {
        sCallbackEnv->CallVoidMethod(DtvkitObject, notifySubtitleCallback, width, height, dst_x, dst_y, dst_width, dst_height, NULL);
    }
}

static void sendDvbParam(const std::string& resource, const std::string json) {
    ALOGD("callback senddvbparam resource:%s, json:%s", resource.c_str(), json.c_str());
    CallbackEnv sCallbackEnv(__func__);
    if (!sCallbackEnv.valid()) return;

    ScopedLocalRef<jstring> jresource(sCallbackEnv.get(), sCallbackEnv->NewStringUTF(resource.c_str()));
    ScopedLocalRef<jstring> jjson(sCallbackEnv.get(),  sCallbackEnv->NewStringUTF(json.c_str()));
    sCallbackEnv->CallVoidMethod(DtvkitObject, notifyDvbCallback, jresource.get(), jjson.get());
}

DTVKitClientJni::DTVKitClientJni()  {
    mDkSession = DTVKitHidlClient::connect(CONNECT_TYPE_HAL);
    mDkSession->setListener(this);
}

DTVKitClientJni *DTVKitClientJni::mInstance = NULL;
DTVKitClientJni *DTVKitClientJni::GetInstance() {
    if (NULL == mInstance)
         mInstance = new DTVKitClientJni();
    return mInstance;
}

DTVKitClientJni::~DTVKitClientJni()  {

}

std::string DTVKitClientJni::request(const std::string& resource, const std::string& json) {
    return mDkSession->request(resource, json);
}

void DTVKitClientJni::notify(const parcel_t &parcel) {
    AutoMutex _l(mLock);
    ALOGD("notify msgType = %d", parcel.msgType);
    if (parcel.msgType == DRAW) {
        datablock_t datablock;
        datablock.width      = parcel.bodyInt[0];
        datablock.height     = parcel.bodyInt[1];
        datablock.dst_x      = parcel.bodyInt[2];
        datablock.dst_y      = parcel.bodyInt[3];
        datablock.dst_width  = parcel.bodyInt[4];
        datablock.dst_height = parcel.bodyInt[5];

        if (datablock.width != 0 || datablock.height != 0) {
            sp<IMemory> memory = mapMemory(parcel.mem);
            if (memory == nullptr) {
                ALOGE("[%s] memory map is null", __FUNCTION__);
                return;
            }
            uint8_t *data = static_cast<uint8_t*>(static_cast<void*>(memory->getPointer()));
            memory->read();
            memory->commit();
            int size = memory->getSize();

            sendSubtitleData(datablock.width, datablock.height, datablock.dst_x, datablock.dst_y,
            datablock.dst_width, datablock.dst_height, size, data);
        } else {
            sendSubtitleData(datablock.width, datablock.height, datablock.dst_x, datablock.dst_y,
            datablock.dst_width, datablock.dst_height, 0, NULL);

        }
    }

    if (parcel.msgType == REQUEST) {
        dvbparam_t dvbparam;
        dvbparam.resource = parcel.bodyString[0];
        dvbparam.json     = parcel.bodyString[1];
        sendDvbParam(dvbparam.resource, dvbparam.json);
    }

}

static void connectdtvkit(JNIEnv *env, jclass clazz, jobject obj)
{
    ALOGI("ref dtvkit");
    mpDtvkitJni  =  DTVKitClientJni::GetInstance();
    DtvkitObject = env->NewGlobalRef(obj);
}

static void disconnectdtvkit(JNIEnv *env, jclass clazz)
{
    ALOGI("disconnect dtvkit");
    env->DeleteGlobalRef(DtvkitObject);
}

static jstring request(JNIEnv *env, jclass clazz, jstring jresource, jstring jjson) {
    const char *resource = env->GetStringUTFChars(jresource, nullptr);
    const char *json = env->GetStringUTFChars(jjson, nullptr);
    if (mpDtvkitJni == nullptr) {
        ALOGE("dtvkitJni is null");
        mpDtvkitJni  =  DTVKitClientJni::GetInstance();
    }
    std::string result   = mpDtvkitJni->request(resource, json);
    env->ReleaseStringUTFChars(jresource, resource);
    env->ReleaseStringUTFChars(jjson, json);
    return env->NewStringUTF(result.c_str());
}

static int updateNative(sp<ANativeWindow> nativeWin) {
    char* vaddr;
    int ret = 0;
    ANativeWindowBuffer* buf;

    if (nativeWin.get() == NULL) {
        return 0;
    }

    int err = nativeWin->dequeueBuffer_DEPRECATED(nativeWin.get(), &buf);

    if (err != 0) {
        ALOGE("dequeueBuffer failed: %s (%d)", strerror(-err), -err);
        return -1;
    }

    return nativeWin->queueBuffer_DEPRECATED(nativeWin.get(), buf);
}

static void SetSurface(JNIEnv *env, jclass thiz, jobject jsurface) {
    sp<IGraphicBufferProducer> new_st = NULL;

    if (jsurface) {
        sp<Surface> surface(android_view_Surface_getSurface(env, jsurface));

        if (surface != NULL) {
            new_st = surface->getIGraphicBufferProducer();

            if (new_st == NULL) {
                jniThrowException(env, "java/lang/IllegalArgumentException",
                                  "The surface does not have a binding SurfaceTexture!");
                return;
            }
        } else {
            jniThrowException(env, "java/lang/IllegalArgumentException",
                              "The surface has been released");
            return;
        }
    }

    sp<ANativeWindow> tmpWindow = NULL;

    if (new_st != NULL) {
        tmpWindow = new Surface(new_st);
        status_t err = native_window_api_connect(tmpWindow.get(),
                       NATIVE_WINDOW_API_MEDIA);
        ALOGI("set native window overlay");
        native_window_set_usage(tmpWindow.get(),
                                am_gralloc_get_video_overlay_producer_usage());
        //native_window_set_usage(tmpWindow.get(), GRALLOC_USAGE_HW_TEXTURE |
        //   GRALLOC_USAGE_EXTERNAL_DISP  | GRALLOC1_PRODUCER_USAGE_VIDEO_DECODER );
        native_window_set_buffers_format(tmpWindow.get(), WINDOW_FORMAT_RGBA_8888);

        updateNative(tmpWindow);
    }
}

static JNINativeMethod gMethods[] = {
{
    "nativeconnectdtvkit", "(Lorg/dtvkit/inputsource/DtvkitGlueClient;)V",
    (void *) connectdtvkit
},
{
    "nativedisconnectdtvkit", "()V",
    (void *) disconnectdtvkit
},
{
    "nativeSetSurface", "(Landroid/view/Surface;)V",
    (void *) SetSurface
},
{
    "nativerequest", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
    (void*) request
},

};


#define FIND_CLASS(var, className) \
        var = env->FindClass(className); \
        LOG_FATAL_IF(! var, "Unable to find class " className);

#define GET_METHOD_ID(var, clazz, methodName, methodDescriptor) \
        var = env->GetMethodID(clazz, methodName, methodDescriptor); \
        LOG_FATAL_IF(! var, "Unable to find method " methodName);

int register_org_dtvkit_inputsource_DtvkitGlueClient(JNIEnv *env)
{
    static const char *const kClassPathName = "org/dtvkit/inputsource/DtvkitGlueClient";
    jclass clazz;
    FIND_CLASS(clazz, kClassPathName);
    GET_METHOD_ID(notifyDvbCallback, clazz, "notifyDvbCallback", "(Ljava/lang/String;Ljava/lang/String;)V");
    GET_METHOD_ID(notifySubtitleCallback, clazz, "notifySubtitleCallback", "(IIIIII[B)V");
    return jniRegisterNativeMethods(env, kClassPathName, gMethods, sizeof(gMethods) / sizeof(gMethods[0]));
}

jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *env = NULL;
    jint result = -1;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK)
    {
        ALOGI("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

    if (register_org_dtvkit_inputsource_DtvkitGlueClient(env) < 0)
    {
        ALOGE("Can't register DtvkitGlueClient");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}


