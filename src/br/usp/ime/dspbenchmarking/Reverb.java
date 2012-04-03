package br.usp.ime.dspbenchmarking;

import android.util.Log;

public class Reverb extends DspAlgorithm {
	
	private double oldOutput[];
	private double oldInput[];

	public Reverb(int sRate, int bSize) {
		super(sRate, bSize);
		oldOutput = new double[bSize];
		oldInput = new double[bSize];
		java.util.Arrays.fill(oldOutput, 0);
	}
	
	public void perform(double[] buffer) {
		//int delayMilliseconds = 5;
		//int delaySamples = (int)((float)delayMilliseconds * this.getSampleRate() / 1000);
		Log.e("marca222", "1");
		// holds old input values
		double[] tmpOldInput = new double[this.getBlockSize()];
		Log.e("marca222", "1.5length:"+buffer.length);
		System.arraycopy(buffer, 0, tmpOldInput, 0, this.getBlockSize());
		Log.e("marca222", "2");
		// number of delay samples
		int m = (int) (this.getParameter1() * this.getBlockSize());
		if (m < 1)
			m=1;
		//m=40;
		Log.e("marca", "3");
		// feedback gain
		double g = 0.7;
		//double g = this.getParameter1();
		
		// perform the reverb
		for (int i = 0; i < m; i++)
			buffer[i] = -g * buffer[i] + oldInput[this.getBlockSize()-m+i] + g * oldOutput[this.getBlockSize()-m+i];
		for (int i = m; i < this.getBlockSize(); i++)
			buffer[i] = -g * buffer[i] + tmpOldInput[i-m] + g * buffer[i-m];
		Log.e("marca", "4");
		// finish
		oldInput = tmpOldInput;
		System.arraycopy(buffer, 0, oldOutput, 0, this.getBlockSize());
		Log.e("marca", "5");

	}
	
}
