package br.usp.ime.dspbenchmarking;

public abstract class DspAlgorithm {

	protected int blockSize;
	private int sampleRate;
	
	private double parameter1 = 1;
	
	public DspAlgorithm(int sRate, int bSize) {
		sampleRate = sRate;
		blockSize = bSize;
	}
	
	abstract public void perform(double[] buffer);
	
	public int getBlockSize() {
		return blockSize;
	}
	
	public int getSampleRate() {
		return sampleRate;
	}
	
	public void setParams(double param1) {
		parameter1 = param1;
	}
	
	public double getParameter1() {
		return parameter1;
	}

}
