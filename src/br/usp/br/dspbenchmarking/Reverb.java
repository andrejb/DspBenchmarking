package br.usp.br.dspbenchmarking;

public class Reverb extends DspAlgorithm {

	public Reverb(int sRate, int bSize) {
		super(sRate, bSize);
		// TODO Auto-generated constructor stub
	}
	
	public void perform(short[] buffer) {
		//int delayMilliseconds = 5;
		//int delaySamples = (int)((float)delayMilliseconds * this.getSampleRate() / 1000);
		
		int delaySamples = (int) (this.getParameter1() * this.getBlockSize());
		
		float decay = 0.5f;
		for (int i = 0; i < this.getBlockSize() - delaySamples; i++)
		//for (int i = 0; i < this.getBlockSize(); i++)
		{
		    // WARNING: overflow potential
		    //buffer[i] = 0;
		    buffer[i+delaySamples] = (short)((float)buffer[i] * decay);
		}
	}
	
}
