LOCAL_PATH := $(call my-dir)

### shared library

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    org_dtvkit_inputsource_DtvkitGlueClient.cpp

$(warning $(JNI_H_INCLUDE))
LOCAL_C_INCLUDES += $(JNI_H_INCLUDE) \
                    libnativehelper/include/nativehelper \
                    frameworks/base/core/jni/include \
                    frameworks/base/libs/hwui \
                    frameworks/native/libs/nativewindow \
                    external/skia/include/core \
                    hardware/amlogic/gralloc \
                    vendor/amlogic/common/external/DTVKit/android-rpcservice/apps/binder/inc \

LOCAL_MODULE := libdtvkit_jni
LOCAL_SHARED_LIBRARIES :=  \
    libhidlbase \
    libhidltransport \
    libhidlmemory \
    libcutils \
    libutils \
    libgui \
    libandroid_runtime \
    liblog \
    libhardware \
    libhardware_legacy \
    libnativehelper \
    lib_dtvkitserver

LOCAL_STATIC_LIBRARIES := libamgralloc_ext_static
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS := optional

LOCAL_PRODUCT_MODULE := true

LOCAL_PRIVATE_PLATFORM_APIS := true
include $(BUILD_SHARED_LIBRARY)
