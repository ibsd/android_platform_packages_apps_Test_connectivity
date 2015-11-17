
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE := sl4a.Utils
LOCAL_MODULE_OWNER := google
LOCAL_STATIC_JAVA_LIBRARIES := guava android-common
LOCAL_SRC_FILES := $(call all-java-files-under, src)

include $(BUILD_STATIC_JAVA_LIBRARY)
