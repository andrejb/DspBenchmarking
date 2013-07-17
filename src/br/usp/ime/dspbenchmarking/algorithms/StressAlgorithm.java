package br.usp.ime.dspbenchmarking.algorithms;

import java.util.Random;


public class StressAlgorithm extends DspAlgorithm {

	private int filterSize;
	private double[] buf;
	private int[][] index;
	private double[] coef;

	public StressAlgorithm(int sRate, int bSize, int fSize) {
		super(sRate, bSize);
		setFilterSize(fSize);
	}
	
	public void setFilterSize(int fSize) {
		buf = new double[blockSize];
		filterSize = fSize;
		index = new int[blockSize][fSize];
		coef = new double[fSize];
		calcIndex();
		calcCoef();	}

	private void calcIndex() {
		for (int n = 0; n < blockSize; n++) {
			for (int k = 0; k < filterSize; k++) {
				index[n][k]= (n-k) % blockSize;
				if (index[n][k] < 0)
					index[n][k] += blockSize;
			}
		}
	}

	private void calcCoef() {
		Random r = new Random();
		for (int k = 0; k < filterSize; k++)
			coef[k]= r.nextDouble();
	}
	
	@Override
	public void perform(double[] buffer) {
		/*Log.i("perform", "blockSize="+blockSize);
		Log.i("perform", "filterSize="+filterSize);
		Log.i("perform", "indexX="+index.length);
		Log.i("perform", "indexY="+index[0].length);*/
		for (int n = 0; n < buffer.length; n++) {
			buf[n] = wmean(buffer, n);
		}
	}

	private double wmean(double buffer[], int n) {
		double sum = 0;
		for (int k = 0; k < filterSize; k++)
			sum += coef[k]*buffer[index[n][k]];
		return sum / n;
	}


}
