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
	
	
	public void setBlockSize(int bSize) {
		blockSize = bSize;
	}

	public long getCallbackPeriod() {
		return callbackPeriod;
	}
	
	public long getReadTicks() {
		return readTicks;
	}
	
	public long getSampleReadTime() {
		return sampleReadTime;
	}
	
	public void reset() {
		callbackPeriod = 0;
		readTicks = 0;
		sampleReadTime = 0;
	}
	
	/**
	 * Set the callback to work over samples.
	 */
	public void setDspCallback(DspPerformCallback callback) {
		dspCallback = callback;
	}

	/**
	 * 
	 */
	public short[] createBuffer() {
		return new short[getBufferSize()];
	}
	
}

