
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE := sl4a.Utils
LOCAL_STATIC_JAVA_LIBRARIES := guava android-common
#LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4
LOCAL_SRC_FILES := $(call all-java-files-under, src)
EXCLUDES := src/com/googlecode/android_scripting/facade/ScanFacade.java


LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

include $(BUILD_STATIC_JAVA_LIBRARY)
