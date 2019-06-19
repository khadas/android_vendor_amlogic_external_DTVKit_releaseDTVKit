LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_SUFFIX :=.so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := optional
LOCAL_PRELINK_MODULE := false
#directory
DVBCORE_DIRECTORY := $(LOCAL_PATH)/../DVBCore/
HAVE_DVBCORE_DIRECTORY := $(shell test -d $(DVBCORE_DIRECTORY) && echo yes)
ifeq ($(HAVE_DVBCORE_DIRECTORY),yes)
$(warning "DVBCore directory exist")
LOCAL_MODULE := libinvaildDTVKit
LOCAL_SRC_FILES := libinvaildDTVKit.so
else
LOCAL_MODULE := lib_dtvkitserver
LOCAL_SRC_FILES := lib_dtvkitserver.so
endif
LOCAL_MODULE_PATH := $(TARGET_OUT_PRODUCT)/lib
include $(BUILD_PREBUILT)
