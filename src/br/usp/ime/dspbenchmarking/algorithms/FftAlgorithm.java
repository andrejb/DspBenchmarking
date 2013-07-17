package br.usp.ime.dspbenchmarking.algorithms;




public class FftAlgorithm extends DspAlgorithm {

	private FFT fft;
	
	public FftAlgorithm(int sRate, int bSize) {
		super(sRate, bSize);
		fft = new FFT((int) (Math.log10(getBlockSize()) / Math.log10(2)));
	}
	
	@Override
	public void perform(double[] real) {
		double imag[] = new double[real.length];
		fft.doFFT(real, imag, false);
		//fft.doFFT(real, imag, true);
	}

}
