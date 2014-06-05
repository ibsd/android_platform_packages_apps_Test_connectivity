
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE := sl4a.QuickAction
LOCAL_STATIC_JAVA_LIBRARIES := guava android-common
LOCAL_SRC_FILES := $(call all-java-files-under, src/net/londatiga/android)


LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

include $(BUILD_STATIC_JAVA_LIBRARY)
