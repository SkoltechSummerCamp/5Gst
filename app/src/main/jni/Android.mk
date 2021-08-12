LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := iperf2
LOCAL_MODULE_TAGS := dev
LOCAL_CFLAGS := -DHAVE_CONFIG_H -UAF_INET6 -w -Wno-error=format-security
LOCAL_LDFLAGS := -fPIE -pie
LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_SRC_FILES := \
        ../../../../iPerf/compat/delay.c \
        ../../../../iPerf/compat/error.c \
        ../../../../iPerf/compat/gettimeofday.c \
        ../../../../iPerf/compat/inet_ntop.c \
        ../../../../iPerf/compat/inet_pton.c \
        ../../../../iPerf/compat/signal.c \
        ../../../../iPerf/compat/snprintf.c \
        ../../../../iPerf/compat/string.c \
        ../../../../iPerf/compat/Thread.c \
        ../../../../iPerf/src/Extractor.c \
        ../../../../iPerf/src/gnu_getopt_long.c \
        ../../../../iPerf/src/gnu_getopt.c \
        ../../../../iPerf/src/histogram.c \
        ../../../../iPerf/src/Locale.c \
        ../../../../iPerf/src/pdfs.c \
        ../../../../iPerf/src/ReportCSV.c \
        ../../../../iPerf/src/ReportDefault.c \
        ../../../../iPerf/src/Reporter.c \
        ../../../../iPerf/src/service.c \
        ../../../../iPerf/src/SocketAddr.c \
        ../../../../iPerf/src/sockets.c \
        ../../../../iPerf/src/stdio.c \
        ../../../../iPerf/src/tcp_window_size.c \
        ../../../../iPerf/src/Client.cpp \
        ../../../../iPerf/src/isochronous.cpp \
        ../../../../iPerf/src/Launch.cpp \
        ../../../../iPerf/src/List.cpp \
        ../../../../iPerf/src/PerfSocket.cpp \
        ../../../../iPerf/src/Settings.cpp \
        ../../../../iPerf/src/main.cpp \
        ../../../../iPerf/src/Listener.cpp \
        ../../../../iPerf/src/Server.cpp \
        ../cpp/src/mainJni.cpp \

LOCAL_C_INCLUDES += \
        $(LOCAL_PATH)/../cpp \
        $(LOCAL_PATH)/../../../../iPerf/include

LOCAL_DISABLE_FORMAT_STRING_CHECKS := true
include $(BUILD_SHARED_LIBRARY)
