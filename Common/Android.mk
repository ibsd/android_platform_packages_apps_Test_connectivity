
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE := sl4a.Common

LOCAL_STATIC_JAVA_LIBRARIES := guava android-common libGoogleAnalytics sl4a.Utils
#LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4

LOCAL_SRC_FILES := $(call all-java-files-under, src/com/googlecode/android_scripting)
LOCAL_SRC_FILES += $(call all-java-files-under, src/org/apache/commons/codec)
#LOCAL_SRC_FILES += $(call all-java-files-under, ../../../../../frameworks/base/wifi/java/android/net/wifi)

#EXCLUDES := src/com/googlecode/android_scripting/facade/SmsFacade.java

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

include $(BUILD_STATIC_JAVA_LIBRARY)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libGoogleAnalytics:libs/libGoogleAnalytics.jar
include $(BUILD_MULTI_PREBUILT)

