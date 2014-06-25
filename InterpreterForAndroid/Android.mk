
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE := sl4a.InterpreterForAndroid
LOCAL_STATIC_JAVA_LIBRARIES := guava android-common sl4a.Utils
#LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4
LOCAL_SRC_FILES := $(call all-java-files-under, src/com/googlecode/android_scripting)

include $(BUILD_STATIC_JAVA_LIBRARY)
