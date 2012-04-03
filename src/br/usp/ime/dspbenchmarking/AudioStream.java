package br.usp.ime.dspbenchmarking;

import br.usp.ime.dspbenchmarking.DspThread.DspCallback;

public abstract class AudioStream {
	
	// timing
	protected long callbackPeriod = 0;
	protected long readTicks = 0;
	protected long sampleReadTime = 0;
	protected boolean isRunning = true;
	
	// DSP
	DspCallback dspCallback;
	protected int blockSize;

	public abstract short[] createBuffer();
	public abstract void scheduleDspCallback(long blockPeriodNanoseconds);
	public abstract void readLoop(short[] buffer);
	public abstract int blocks();
	public abstract void stopRunning();
	protected abstract int getMinBufferSize();
	
	
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
	 * 
	 */
	public void setDspCallback(DspCallback callback) {
		dspCallback = callback;
	}

}
