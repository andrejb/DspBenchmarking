package br.usp.ime.dspbenchmarking.fftw;

public class FFTW {

    static {
        System.loadLibrary("fftw_jni");
    }

	private static native double[] executeJNI(double in[]);
	public static double[] execute(double in[]) {
		return FFTW.executeJNI(in);
	}
	
}
