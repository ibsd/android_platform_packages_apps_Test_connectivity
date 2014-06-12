
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE := sl4a.SignalStrengthFacade


LOCAL_STATIC_JAVA_LIBRARIES := guava android-common sl4a.Common
LOCAL_STATIC_JAVA_LIBRARIES += sl4a.Utils 

LOCAL_SRC_FILES := $(call all-java-files-under, src/com/googlecode/android_scripting)


LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

include $(BUILD_STATIC_JAVA_LIBRARY)
