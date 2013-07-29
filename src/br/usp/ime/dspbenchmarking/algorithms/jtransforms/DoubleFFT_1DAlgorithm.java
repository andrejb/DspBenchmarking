package br.usp.ime.dspbenchmarking.algorithms.jtransforms;

import br.usp.ime.dspbenchmarking.algorithms.jtransforms.DoubleFFT_1D;

public class DoubleFFT_1DAlgorithm extends br.usp.ime.dspbenchmarking.algorithms.DspAlgorithm {
	
	public DoubleFFT_1DAlgorithm(int sRate, int bSize) {
		super(sRate, bSize);
	}

	@Override
	public void perform(double[] buffer) {
		DoubleFFT_1D fft = new DoubleFFT_1D(buffer.length/2);
		ConcurrencyUtils.setNumberOfThreads(1);
		fft.complexForward(buffer);
	}

	@Override
	public String getAlgorithmName() {
		return "DoubleFFT 1D";
	}

}
