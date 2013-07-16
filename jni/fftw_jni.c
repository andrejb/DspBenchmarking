#include <jni.h>
#include <android/log.h>

#include <unistd.h>
#include <stdio.h>
#include <stdint.h>

#include <fftw3.h>
#include <math.h>

#define LOG_TAG "FFTW_JNI"

static int count_exec          = 0;
static int threads_enabled     = 0;
static int threads_initialized = 0;

static void log_callback(void* ptr, int level, const char* fmt, va_list vl) {
	 __android_log_vprint(ANDROID_LOG_INFO, LOG_TAG, fmt, vl);
}

inline static void execute_fftw(double *in, int num, double **out_res) {
	fftw_plan plan;
	fftw_complex *out;
	int i, j;

	out = fftw_malloc(sizeof(fftw_complex) * (num/2 + 1));
	*out_res = fftw_malloc(sizeof(double) * (num/2 + 1) * 2);

	plan = fftw_plan_dft_r2c_1d(num, in, out, FFTW_ESTIMATE);
	fftw_execute(plan);

	fftw_destroy_plan(plan);

	for (i = 0, j = 0; i < (num/2); i++, j+= 2) {
		(*out_res)[j]   = out[i][0];
		(*out_res)[j+1] = out[i][1];
	}

	fftw_free(out);
}

JNIEXPORT jboolean JNICALL Java_br_usp_ime_dspbenchmarking_fftw_FFTW_areThreadsEnabled(JNIEnv *pEnv, jobject pObj) {
	return ((threads_enabled == 1) ? JNI_TRUE : JNI_FALSE);
}

JNIEXPORT void JNICALL Java_br_usp_ime_dspbenchmarking_fftw_FFTW_removeThreadsJNI(JNIEnv *pEnv, jobject pObj) {
	if (!threads_initialized) {
		char buff[150];
		sprintf(buff, "Threads weren't initialized");
		(*pEnv)->ThrowNew(pEnv, (*pEnv)->FindClass(pEnv, "java/lang/Exception"), buff);
	} else {
		fftw_plan_with_nthreads(1);
		threads_enabled = 0;
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Threads disabled");
	}
}

JNIEXPORT void JNICALL Java_br_usp_ime_dspbenchmarking_fftw_FFTW_initThreadsJNI(JNIEnv *pEnv, jobject pObj, jint num_of_threads) {
	if (!threads_initialized && !fftw_init_threads()) {
		char buff[150];
		sprintf(buff, "Failed to initialize thread");
		(*pEnv)->ThrowNew(pEnv, (*pEnv)->FindClass(pEnv, "java/lang/Exception"), buff);
		threads_initialized = 1;
	} else {
		fftw_plan_with_nthreads(num_of_threads);
		threads_enabled = 1;
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Threads enabled");
	}
}

JNIEXPORT jdoubleArray JNICALL Java_br_usp_ime_dspbenchmarking_fftw_FFTW_executeJNI(JNIEnv *pEnv, jobject pObj, jdoubleArray in) {

	jdouble      *elements;
	double       *real;
	jboolean     isCopy1, isCopy2;
	jint         n_elemens;
	double       *result;
	jdoubleArray resJNI;
	jdouble      *resArray;
	int i, len, n_elements;

	//__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Executing FFTW [%d] %d", n_elements, ++count_exec);

	elements   = (*pEnv)->GetDoubleArrayElements(pEnv, in, &isCopy1);
	n_elements = (*pEnv)->GetArrayLength(pEnv, in);

	real = (double*) elements;

	execute_fftw(real, n_elements, &result);

	len = (n_elements/2 + 1) * 2;
	resJNI   = (*pEnv)->NewDoubleArray(pEnv, len);
	resArray = (*pEnv)->GetDoubleArrayElements(pEnv, resJNI, &isCopy2);

	for (i = 0; i < len; i++) {
		resArray[i] = result[i];
	}

	(*pEnv)->ReleaseDoubleArrayElements(pEnv, in, elements, JNI_FALSE);
	(*pEnv)->ReleaseDoubleArrayElements(pEnv, resJNI, resArray, JNI_FALSE);

	fftw_free(result);

	return resJNI;
}
