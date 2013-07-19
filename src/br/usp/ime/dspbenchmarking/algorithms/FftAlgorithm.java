package br.usp.ime.dspbenchmarking.algorithms;



/**
 * A simple algorithm that calculates a one way FFT.
 * @author andrejb
 *
 */
public class FftAlgorithm extends DspAlgorithm {

	private FFT fft;
	
	/*************************************************************************
	 * Constructor.
	 ***********************************************************************/
	
	public FftAlgorithm(int sRate, int bSize) {
		super(sRate, bSize);
		fft = new FFT((int) (Math.log10(getBlockSize()) / Math.log10(2)));
	}
	
	/*************************************************************************
	 * Perform method.
	 ***********************************************************************/

	/**
	 * Perform a one-way FFT.
	 */
	@Override
	public void perform(double[] real) {
		double imag[] = new double[real.length];
		java.util.Arrays.fill(imag, 0);
		fft.doFFT(real, imag, false);
		//fft.doFFT(real, imag, true);
	}
	
	/**
	 * Reconfigure the FFT when block size changes.
	 */
	public void setBlockSize(int bSize) {
		super.setBlockSize(bSize);
		fft.setBits((int) (Math.log10(getBlockSize()) / Math.log10(2)));
	}

}
