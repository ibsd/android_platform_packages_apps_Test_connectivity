LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_SRC_FILES := com_googlecode_android_scripting_Exec.cpp


LOCAL_C_INCLUDES += \
    $(JNI_H_INCLUDE) \

LOCAL_SHARED_LIBRARIES := \
    libandroid_runtime \
    libnativehelper \
    libcutils \
    libutils \
    liblog 

LOCAL_MODULE    := com_googlecode_android_scripting_Exec
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
