
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE := sl4a.BluetoothFacade


LOCAL_STATIC_JAVA_LIBRARIES := guava android-common 
LOCAL_STATIC_JAVA_LIBRARIES += sl4a.Utils sl4a.Common

LOCAL_SRC_FILES := $(call all-java-files-under, src/com/googlecode/android_scripting)


LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

include $(BUILD_STATIC_JAVA_LIBRARY)
