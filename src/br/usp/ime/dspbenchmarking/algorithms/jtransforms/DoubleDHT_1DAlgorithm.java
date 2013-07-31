package br.usp.ime.dspbenchmarking.algorithms.jtransforms;

import br.usp.ime.dspbenchmarking.algorithms.jtransforms.DoubleDHT_1D;

public class DoubleDHT_1DAlgorithm extends br.usp.ime.dspbenchmarking.algorithms.DspAlgorithm {
	
	public DoubleDHT_1DAlgorithm(int sRate, int bSize) {
		super(sRate, bSize);
	}

	@Override
	public void perform(double[] buffer) {
		DoubleDHT_1D dht = new DoubleDHT_1D(buffer.length/2);
		ConcurrencyUtils.setNumberOfThreads(1);
		dht.forward(buffer);
	}

	@Override
	public String getAlgorithmName() {
		return "DoubleDHT 1D";
	}

}
