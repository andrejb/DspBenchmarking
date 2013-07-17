package br.usp.ime.dspbenchmarking.algorithms;



public class AdditiveSynthesis extends DspAlgorithm {

	private int k;
	private double sine[];	
	private static final double TWOPI = 2.0 * Math.PI;
	private static final int SINETABLE_SIZE = 1024;
	private int ind;
	
	public AdditiveSynthesis(int sRate, int bSize) {
		super(sRate, bSize);
		k = 1;
		double[] sine = new double[1024];
		for (int i=0; i<SINETABLE_SIZE; i++)
			sine[i] = Math.sin(TWOPI * i / SINETABLE_SIZE);
		ind = 0;
	}

	@Override
	public void perform(double[] buffer) {
		k = (int) (this.getParameter1()*10);
		for (int n = 0; n < buffer.length; n++) {
			for (int i = 0; i < k; i++) {
				//buffer[n] = sine[];
				buffer[n] = Math.sin(TWOPI*110*ind*k/44100);
				ind++;
			}
		}
	}
}
