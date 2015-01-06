
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE := sl4a.Common
LOCAL_MODULE_OWNER := google

LOCAL_STATIC_JAVA_LIBRARIES := guava android-common sl4a.Utils libGoogleAnalytics
LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_JAVA_LIBRARIES += ims-common

LOCAL_SRC_FILES := $(call all-java-files-under, src/com/googlecode/android_scripting)
LOCAL_SRC_FILES += $(call all-java-files-under, src/org/apache/commons/codec)

include $(BUILD_STATIC_JAVA_LIBRARY)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libGoogleAnalytics:libs/libGoogleAnalytics.jar
include $(BUILD_MULTI_PREBUILT)
