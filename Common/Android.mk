
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE := sl4a.Common

LOCAL_JAVA_LIBRARIES := libGoogleAnalytics
LOCAL_STATIC_JAVA_LIBRARIES := guava android-common sl4a.Utils

LOCAL_SRC_FILES := $(call all-java-files-under, src/com/googlecode/android_scripting)
LOCAL_SRC_FILES += $(call all-java-files-under, src/org/apache/commons/codec)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

include $(BUILD_STATIC_JAVA_LIBRARY)


