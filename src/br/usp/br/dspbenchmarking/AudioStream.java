package br.usp.br.dspbenchmarking;

import br.usp.br.dspbenchmarking.DspThread.DspCallback;

public abstract class AudioStream {
	
	// timing
	protected long callbackPeriod = 0;
	protected long readTicks = 0;
	protected long sampleReadTime = 0;
	protected boolean isRunning = true;
	
	// DSP
	DspCallback dspCallback;

	public abstract short[] createBuffer();
	public abstract void scheduleDspCallback(DspCallback callback, long blockPeriodNanoseconds);
	public abstract void readLoop(short[] buffer);
	public abstract int blocks();
	public abstract void stopRunning();
	protected abstract int getMinBufferSize();
	
	public long getCallbackPeriod() {
		return callbackPeriod;
	}
	
	public long getReadTicks() {
		return callbackPeriod;
	}
	
	public long getSampleReadTime() {
		return sampleReadTime;
	}

}
