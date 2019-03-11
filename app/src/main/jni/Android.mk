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
                    $(call include-path-for, libhardware)/hardware \
                    $(call include-path-for, libhardware_legacy)/hardware_legacy \

LOCAL_MODULE := libdtvkit_jni

LOCAL_SHARED_LIBRARIES :=  \
    vendor.amlogic.hardware.dtvkitserver@1.0 \
    libhidlbase \
    libhidltransport \
    libhidlmemory \
    android.hidl.allocator@1.0 \
    libcutils \
    libutils \
    libgui \
    libandroid_runtime \
    liblog \
    libhardware \
    libhardware_legacy \
    libnativehelper \
    libdtvkithidlclient

LOCAL_STATIC_LIBRARIES := libamgralloc_ext_static
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS := optional

ifneq ($(BUILD_DTVKIT_IN_SYSTEM), true)
LOCAL_PRODUCT_MODULE := true
endif

LOCAL_PRIVATE_PLATFORM_APIS := true
include $(BUILD_SHARED_LIBRARY)
