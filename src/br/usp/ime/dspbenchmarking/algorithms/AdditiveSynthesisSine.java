package br.usp.ime.dspbenchmarking.algorithms;


/**
 * This algorithm defines an Additive Synthesis using a number of oscillators 
 * equal to the stressParameter. The oscilator is calculated using Java's
 * Math.sin() function.
 *  
 * @author andrejb
 *
 */
public class AdditiveSynthesisSine extends StressAlgorithm {

	private static final double TWOPI = 2.0 * Math.PI;
	private int lastInd;  // Used to preserve phase.
	
	public AdditiveSynthesisSine(int sRate, int bSize, int stressParam) {
		super(sRate, bSize);
		setStressParameter(stressParam);
		lastInd = 0;
	}

	/**
	 * The perform method is 
	 */
	@Override
	public void perform(double[] buffer) {
		for (int n = 0; n < buffer.length; n++) {
			buffer[n] = 0;
			for (int i = 0; i < stressParameter; i++)
				buffer[n] += Math.sin(TWOPI*110*(lastInd+n)*i/44100);
			buffer[n] /= stressParameter;  // normalize
			lastInd += buffer.length;  // preserve phase
		}
	}
	
	/**
	 * When changing the parameter using the GUI, also update the number of
	 * oscillators used in calculation.
	 */
	public void setParams(double param1) {
		super.setParams(param1);
		setStressParameter((int) (10 * param1));
	}
}
