package br.usp.ime.dspbenchmarking.algorithms;

/**
 * This is the representation of a DSP algorithm. It stores the current DSP
 * block size and the sample rate of the signal it is working over.
 * 
 * All DSP algorithms should have a perform() method that works over a buffer
 * containing the signal samples and actually modifies/analyses/synthesises
 * the signal.
 * 
 * @author andrejb
 *
 */
public abstract class DspAlgorithm {

	protected int blockSize;
	protected int sampleRate;
	
	protected double parameter1 = 1;
	
	/*************************************************************************
	 * Constructor
	 ************************************************************************/
	
	/**
	 * The constructor just saves the sample rate and block size of the
	 * current algorithm.
	 * 
	 * @param sRate
	 * @param bSize
	 */
	public DspAlgorithm(int sRate, int bSize) {
		sampleRate = sRate;
		blockSize = bSize;
	}

	/*************************************************************************
	 * Perform method
	 ************************************************************************/
	
	/**
	 * This method should be implemented by all DSP algorithms. It is the
	 * actual perform function that works over a buffer to modify the audio
	 * signal.
	 * 
	 * @param buffer
	 */
	abstract public void perform(double[] buffer);
	
	/*************************************************************************
	 * Setters/getters
	 ************************************************************************/
	
	/**
	 * Set the block size.
	 * 
	 * @param bSize
	 */
	public void setBlockSize(int bSize) {
		blockSize = bSize;
	}
	
	/**
	 * @return The current block size.
	 */
	public int getBlockSize() {
		return blockSize;
	}
	
	/**
	 * @return The current sample rate.
	 */
	public int getSampleRate() {
		return sampleRate;
	}
	
	/**
	 * Set a parameter to control the algorithm.
	 * @param param1
	 */
	public void setParams(double param1) {
		parameter1 = param1;
	}
	
	/**
	 * @return The parameter that controls the algorithm.
	 */
	public double getParameter1() {
		return parameter1;
	}
	
}
