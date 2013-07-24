LOCAL_PATH := $(call my-dir)
LIBFOLDER := fftw

include $(CLEAR_VARS)

LOCAL_MODULE := fftw3
LOCAL_SRC_FILES := $(LIBFOLDER)/android/armv6/lib/libfftw3.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(LIBFOLDER)/android/armv6/include/
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS) 
LOCAL_ALLOW_UNDEFINED_SYMBOLS=false
LOCAL_MODULE := fftw_jni
LOCAL_SRC_FILES := fftw_jni.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(LIBFOLDER)/android/armv6/include
LOCAL_STATIC_LIBRARY := fftw3
LOCAL_LDLIBS    := -llog -lz -lm $(LOCAL_PATH)/$(LIBFOLDER)/android/armv6/lib/libfftw3.a
include $(BUILD_SHARED_LIBRARY) 