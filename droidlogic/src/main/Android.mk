LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := DvbSSettings

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res \


LOCAL_JAVA_LIBRARIES += \
    android.hidl.base-V1.0-java \
    android.hidl.manager-V1.0-java

LOCAL_STATIC_JAVA_LIBRARIES += \
    vendor.amlogic.hardware.dtvkitserver-V1.0-java

LOCAL_SRC_FILES := $(call all-subdir-java-files, ($(LOCAL_PATH)/java))

LOCAL_CERTIFICATE := platform
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PRODUCT_MODULE := true
LOCAL_PRIVATE_PLATFORM_APIS := true
include $(BUILD_PACKAGE)
include $(call all-makefiles-under, $(LOCAL_PATH))

