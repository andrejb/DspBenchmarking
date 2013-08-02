package br.usp.ime.dspbenchmarking.algorithms.jtransforms;

import br.usp.ime.dspbenchmarking.algorithms.jtransforms.DoubleDCT_1D;

public class DoubleDCT_1DAlgorithm extends br.usp.ime.dspbenchmarking.algorithms.DspAlgorithm {
	
	public DoubleDCT_1DAlgorithm(int sRate, int bSize) {
		super(sRate, bSize);
	}

	@Override
	public void perform(double[] buffer) {
		DoubleDCT_1D dct = new DoubleDCT_1D(buffer.length/2);
		ConcurrencyUtils.setNumberOfThreads(1);
		dct.forward(buffer, false);
	}

	@Override
	public String getAlgorithmName() {
		return "DoubleDCT 1D";
	}

}
