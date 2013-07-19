package br.usp.ime.dspbenchmarking.streams;


import br.usp.ime.dspbenchmarking.threads.DspThread.DspPerformCallback;


/**
 * An AudioStream is a stream of input audio signal.
 * 
 * This abstract class may be implemented to provide input audio streams of
 * many types, such as from microphones, audio files and others.
 * 
 * Each AudioStream implementation is responsible for executing a DspCallback
 * periodically, which will in turn execute an algorithm over a block of
 * samples.
 * 
 * @author André J. Bianchi
 *
 */
public abstract class AudioStream {
	
	// timing
	protected long callbackPeriod = 0;
	protected long readTicks = 0;
	protected long sampleReadTime = 0;
	protected boolean isRunning = true;
	
	// DSP
	DspPerformCallback dspCallback;
	protected int blockSize;

	public abstract int getBufferSize();
	public abstract void scheduleDspCallback(long blockPeriodNanoseconds);
	public abstract void readLoop(short[] buffer);
	public abstract int blocks();
	public abstract void stopRunning();
	public abstract int getMinBufferSize();
	
	
	/**
	 * Set the block size for the signal processing of the stream.
	 * @param bSize
	 */
	public void setBlockSize(int bSize) {
		blockSize = bSize;
	}

	/**
	 * @return The sum of the periods of callback calls.
	 */
	public long getCallbackPeriod() {
		return callbackPeriod;
	}
	
	/**
	 * @return The amount of times blocks were read from input.
	 */
	public long getReadTicks() {
		return readTicks;
	}
	
	/**
	 * @return The sum of the time taken to read from input.
	 */
	public long getSampleReadTime() {
		return sampleReadTime;
	}
	
	/**
	 * Reset the audio stream parameters.
	 */
	public void reset() {
		callbackPeriod = 0;
		readTicks = 0;
		sampleReadTime = 0;
	}
	
	/**
	 * Define the callback that works over samples.
	 */
	public void setDspCallback(DspPerformCallback callback) {
		dspCallback = callback;
	}

	/**
	 * Create a new buffer to store input samples. 
	 */
	public short[] createBuffer() {
		return new short[getBufferSize()];
	}
	
}

