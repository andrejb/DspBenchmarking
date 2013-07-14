#include <jni.h>
#include <android/log.h>

#include <unistd.h>
#include <stdio.h>
#include <stdint.h>

#include <fftw3.h>
#include <math.h>

#define LOG_TAG "FFTW_JNI"

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
		*out_res[j]   = out[i][0];
		*out_res[j+1] = out[i][1];
	}

	fftw_free(out);
}

JNIEXPORT jdoubleArray JNICALL Java_br_usp_ime_dspbenchmarking_fftw_FFTW_executeJNI(JNIEnv *pEnv, jobject pObj, jdoubleArray in) {

	jdouble      *elements;
	double       *real;
	jboolean     isCopy;
	jint         n_elemens;
	double       *result;
	jdoubleArray resJNI;
	jdouble      *resArray;
	int i, len, n_elements;

	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Executing FFTW");

	elements   = (*pEnv)->GetDoubleArrayElements(pEnv, in, &isCopy);
	n_elements = (*pEnv)->GetArrayLength(pEnv, in);

	real = (double*) elements;

	execute_fftw(real, n_elements, &result);

	len = (n_elements/2 + 1) * 2;
	resJNI   = (*pEnv)->NewDoubleArray(pEnv, len);
	resArray = (*pEnv)->GetDoubleArrayElements(pEnv, resJNI, &isCopy);

	for (i = 0; i < len; i++) {
		resArray[i] = result[i];
	}

	if (isCopy == JNI_TRUE) {
		(*pEnv)->ReleaseDoubleArrayElements(pEnv, in, elements, JNI_ABORT);
	}

	if (isCopy == JNI_TRUE) {
		(*pEnv)->ReleaseDoubleArrayElements(pEnv, resJNI, resArray, JNI_ABORT);
	}

	fftw_free(result);

	return resJNI;
}
