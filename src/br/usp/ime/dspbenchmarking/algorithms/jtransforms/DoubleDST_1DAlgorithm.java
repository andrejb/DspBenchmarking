package br.usp.ime.dspbenchmarking.algorithms.jtransforms;

import br.usp.ime.dspbenchmarking.algorithms.jtransforms.DoubleDST_1D;

public class DoubleDST_1DAlgorithm extends br.usp.ime.dspbenchmarking.algorithms.DspAlgorithm {
	
	public DoubleDST_1DAlgorithm(int sRate, int bSize) {
		super(sRate, bSize);
	}

	@Override
	public void perform(double[] buffer) {
		DoubleDST_1D dst = new DoubleDST_1D(buffer.length/2);
		ConcurrencyUtils.setNumberOfThreads(1);
		dst.forward(buffer, false);
	}

	@Override
	public String getAlgorithmName() {
		return "DoubleDST 1D";
	}

}
