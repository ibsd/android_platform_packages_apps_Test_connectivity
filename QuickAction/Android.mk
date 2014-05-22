
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_PACKAGE_NAME := sl4a.QuickAction-res

# Tell aapt to create "extending (non-application)" resource IDs,
# since these resources will be used by many apps.
LOCAL_AAPT_FLAGS := -x

LOCAL_MODULE_TAGS := optional

# Install this alongside the libraries.
LOCAL_MODULE_PATH := $(TARGET_OUT_JAVA_LIBRARIES)

# Create package-export.apk, which other packages can use to get
# PRODUCT-agnostic resource data like IDs and type definitions.
LOCAL_EXPORT_PACKAGE_RESOURCES := true

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

#*******build lib***************************************

LOCAL_MODULE := sl4a.QuickAction


LOCAL_STATIC_JAVA_LIBRARIES := guava android-common
#LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4

LOCAL_SRC_FILES := $(call all-java-files-under, src/net/londatiga/android)
LOCAL_SRC_FILES += ../../../../../../$(call intermediates-dir-for,APPS,sl4a.QuickAction-res,,COMMON)/src/net/londatiga/android/R.java


LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_STATIC_JAVA_LIBRARY)

# Make sure that R.java and Manifest.java are built before we build
# the source for this library.
sl4a.QuickAction_res_R_stamp := \
	$(call intermediates-dir-for,APPS,sl4a.QuickAction-res,,COMMON)/src/R.stamp
$(full_classes_compiled_jar): $(sl4a.QuickAction_res_R_stamp)
