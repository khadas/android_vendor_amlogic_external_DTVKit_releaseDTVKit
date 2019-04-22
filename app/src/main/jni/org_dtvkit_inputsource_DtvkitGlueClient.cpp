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

#include "glue_client.h"


#include "org_dtvkit_inputsource_DtvkitGlueClient.h"

using namespace android;

static JavaVM   *gJavaVM = NULL;


static jmethodID notifySubtitleCallback;
static jmethodID notifyDvbCallback;
static jmethodID gReadSysfsID;
static jmethodID gWriteSysfsID;

static jobject DtvkitObject;

static void sendSubtitleData(int width, int height, int dst_x, int dst_y, int dst_width, int dst_height, uint8_t* data)
{
    //ALOGD("callback sendSubtitleData data = %p", data);
    CallbackEnv sCallbackEnv(__func__);
    if (!sCallbackEnv.valid()) {
       ALOGD("-callback event get error");
    }
    JNIEnv *env;
    int ret;
    int attached = 0;

    ret = (gJavaVM)->GetEnv((void**) &env, JNI_VERSION_1_4);
    if (ret <0) {
        ret = (gJavaVM)->AttachCurrentThread(&env,NULL);
        if (ret <0) {
            ALOGD("callback handler:failed to attach current thread");
            return;
        }
        attached = 1;
    }
    if (env != NULL) {
        if (width != 0 || height != 0) {
            ScopedLocalRef<jbyteArray> array (env, env->NewByteArray(width * height * 4));
            if (!array.get()) {
                ALOGE("Fail to new jbyteArray sharememory addr");
                return;
            }
            env->SetByteArrayRegion(array.get(), 0, width * height * 4, (jbyte*)data);
            env->CallVoidMethod(DtvkitObject, notifySubtitleCallback, width, height, dst_x, dst_y, dst_width, dst_height, array.get());
        } else {
            env->CallVoidMethod(DtvkitObject, notifySubtitleCallback, width, height, dst_x, dst_y, dst_width, dst_height, NULL);
        }
    } else {
        if (width != 0 || height != 0) {
            ScopedLocalRef<jbyteArray> array (sCallbackEnv.get(), sCallbackEnv->NewByteArray(width * height * 4));
            if (!array.get()) {
                ALOGE("Fail to new jbyteArray sharememory addr");
                return;
            }
            sCallbackEnv->SetByteArrayRegion(array.get(), 0, width * height * 4, (jbyte*)data);
            sCallbackEnv->CallVoidMethod(DtvkitObject, notifySubtitleCallback, width, height, dst_x, dst_y, dst_width, dst_height, array.get());
        } else {
            sCallbackEnv->CallVoidMethod(DtvkitObject, notifySubtitleCallback, width, height, dst_x, dst_y, dst_width, dst_height, NULL);
        }
    }
    if (attached) {
        (gJavaVM)->DetachCurrentThread();
    }
}

static void sendDvbParam(const std::string& resource, const std::string json) {
    ALOGD("-callback senddvbparam resource:%s, json:%s", resource.c_str(), json.c_str());
    CallbackEnv sCallbackEnv(__func__);
    if (!sCallbackEnv.valid()) {
        //ALOGD("-callback event get error");
    }
    JNIEnv *env;
    int ret;
    int attached = 0;

    ret = (gJavaVM)->GetEnv((void**) &env, JNI_VERSION_1_4);
    if (ret <0) {
        ret = (gJavaVM)->AttachCurrentThread(&env,NULL);
        if (ret <0) {
            ALOGD("callback handler:failed to attach current thread");
            return;
        }
        attached = 1;
    }
    if (env != NULL) {
        //ALOGD("-callback event get ok");
        ScopedLocalRef<jstring> jresource((env), (env)->NewStringUTF(resource.c_str()));
        ScopedLocalRef<jstring> jjson((env),  (env)->NewStringUTF(json.c_str()));
        (env)->CallVoidMethod(DtvkitObject, notifyDvbCallback, jresource.get(), jjson.get());
    } else {
        //ALOGD("-callback event not get env");
        ScopedLocalRef<jstring> jresource(sCallbackEnv.get(), sCallbackEnv->NewStringUTF(resource.c_str()));
        ScopedLocalRef<jstring> jjson(sCallbackEnv.get(),  sCallbackEnv->NewStringUTF(json.c_str()));
        sCallbackEnv->CallVoidMethod(DtvkitObject, notifyDvbCallback, jresource.get(), jjson.get());
    }

    if (attached) {
        (gJavaVM)->DetachCurrentThread();
    }
}

//notify java read sysfs
void readBySysControl(int ftype, const char *name, char *buf, int len)
{
    JNIEnv *env;
    int ret;
    int attached = 0;
    jint f_type = ftype;
    ret = gJavaVM->GetEnv((void **) &env, JNI_VERSION_1_4);

    if (ret < 0) {
        ret = gJavaVM->AttachCurrentThread(&env, NULL);
        if (ret < 0) {
            ALOGD("Can't attach thread read");
            return;
        }
        attached = 1;
    }
    jstring jvalue = NULL;
    const char *utf_chars = NULL;
    jstring jname = env->NewStringUTF(name);
    jvalue = (jstring)env->CallObjectMethod(DtvkitObject, gReadSysfsID,f_type,  jname);
    if (jvalue) {
        utf_chars = env->GetStringUTFChars(jvalue, NULL);
        if (utf_chars) {
            memset(buf, 0, len);
            if (len <= strlen(utf_chars) + 1) {
                memcpy(buf, utf_chars, len - 1);
                buf[strlen(buf)] = '\0';
            }else {
                strcpy(buf, utf_chars);
            }
        }
        env->ReleaseStringUTFChars(jvalue, utf_chars);
        env->DeleteLocalRef(jvalue);
    }
    env->DeleteLocalRef(jname);

    if (attached) {
        gJavaVM->DetachCurrentThread();
    }
}

//notify java write sysfs
void writeBySysControl(int ftype, const char *name, const char *cmd)
{
    JNIEnv *env;
    int ret;
    int attached = 0;
    jint f_type = ftype;
    ret = gJavaVM->GetEnv((void **) &env, JNI_VERSION_1_4);

    if (ret < 0) {
        ret = gJavaVM->AttachCurrentThread(&env, NULL);
        if (ret < 0) {
            ALOGD("Can't attach thread write");
            return;
        }
        attached = 1;
    }
    jstring jname = env->NewStringUTF(name);
    jstring jcmd = env->NewStringUTF(cmd);
    env->CallVoidMethod(DtvkitObject, gWriteSysfsID,f_type, jname, jcmd);
    env->DeleteLocalRef(jname);
    env->DeleteLocalRef(jcmd);
    if (attached) {
        gJavaVM->DetachCurrentThread();
    }
}


//notify java write sysfs
void write_sysfs_cb(const char *name, const char *cmd)
{
    writeBySysControl( SYSFS, name, cmd);
}

//notify java read sysfs
void read_sysfs_cb(const char *name, char *buf, int len)
{
    readBySysControl(SYSFS,name, buf, len);
}
//notify java write sysfs
void write_prop_cb(const char *name, const char *cmd)
{
    writeBySysControl( PROP, name, cmd);
}

//notify java read sysfs
void read_prop_cb(const char *name, char *buf, int len)
{
    readBySysControl(PROP, name, buf, len);
}

static void connectdtvkit(JNIEnv *env, jclass clazz, jobject obj)
{
    ALOGD("connect dtvkit");
    DtvkitObject = env->NewGlobalRef(obj);
    Glue_client::getInstance()->RegisterRWSysfsCallback((void*)read_sysfs_cb, (void*)write_sysfs_cb);
    Glue_client::getInstance()->RegisterRWPropCallback((void*)read_prop_cb, (void*)write_prop_cb);
    Glue_client::getInstance()->setSignalCallback((SIGNAL_CB)sendDvbParam);
    Glue_client::getInstance()->setDisPatchDrawCallback((DISPATCHDRAW_CB)sendSubtitleData);
    Glue_client::getInstance()->addInterface();
}

static void disconnectdtvkit(JNIEnv *env, jclass clazz)
{
    ALOGD("disconnect dtvkit");
    Glue_client::getInstance()->UnRegisterRWSysfsCallback();
    Glue_client::getInstance()->UnRegisterRWPropCallback();
    env->DeleteGlobalRef(DtvkitObject);
}

static jstring request(JNIEnv *env, jclass clazz, jstring jresource, jstring jjson) {
    const char *resource = env->GetStringUTFChars(jresource, nullptr);
    const char *json = env->GetStringUTFChars(jjson, nullptr);

    ALOGD("request resource[%s]  json[%s]",resource, json);
    std::string result  = Glue_client::getInstance()->request(std::string(resource),std::string( json));
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
    ALOGD("SetSurface");
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

    GET_METHOD_ID(gReadSysfsID,clazz, "readBySysControl", "(ILjava/lang/String;)Ljava/lang/String;");
    GET_METHOD_ID(gWriteSysfsID,clazz, "writeBySysControl", "(ILjava/lang/String;Ljava/lang/String;)V");

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
    gJavaVM = vm;
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


