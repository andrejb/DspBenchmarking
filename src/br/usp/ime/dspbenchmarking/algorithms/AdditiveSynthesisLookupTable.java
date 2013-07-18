package br.usp.ime.dspbenchmarking.algorithms;

/**
 * This class defines an abstract additive synthesis using lookup table.
 * 
 * @author andrejb
 *
 */
public abstract class AdditiveSynthesisLookupTable extends StressAlgorithm {

	// Sine table variables
	protected float  sine[];	
	protected static final double TWOPI = 2.0 * Math.PI;
	private static final int SINETABLE_SIZE = 1024;

	// Lookup variables
	protected float ind[] = new float[1024];  // the last index read for each oscillator
	protected float dt;  // the time between two adjacent samples
	protected float deltai[] = new float[1024];  // the delta for each oscillator
	
	public AdditiveSynthesisLookupTable(int sRate, int bSize, int stressParam) {
		super(sRate, bSize);
		setStressParameter(stressParam);
		// initialize the sine table
		sine = new float[SINETABLE_SIZE];
		for (int i=0; i<SINETABLE_SIZE; i++)
			sine[i] = (float) Math.sin(TWOPI * i / SINETABLE_SIZE);
		// calculate the time between two adjacent samples
		dt = 1/sampleRate;
		// initialize the initial phases
		for (int i = 0; i < ind.length; i++)
			ind[i] = 0;
	}
	
	
	/**
	 * The perform method executed sums a number of oscillators equal to the
	 * stressParameter.
	 */
	@Override
	public void perform(double[] buffer) {
		for (int n = 0; n < buffer.length; n++) {
			buffer[n] = 0;
			for (int k = 0; k < stressParameter; k++) {
				buffer[n] += interp(ind[k]+n*deltai[k]);
			}
			buffer[n] /= stressParameter;  // normalize
			
			// update indexes
			if (n == buffer.length - 1)
				for (int k = 0; k < stressParameter; k++)
					ind[k] = modS(ind[k]+dt*deltai[k]);
		}
	}
	
	/**
	 * @param y
	 * @return y % S.
	 */
	private float modS(float y)
	{
		return y-((int)y/sine.length)*sine.length;
	}

	
	/**
	 * Set the stress parameter.
	 * 
	 * @param fSize
	 */
	public void setStressParameter(int stressParam) {
		super.setStressParameter(stressParam);
		for (int i = 0; i < stressParameter; i++)
			deltai[i] = (float) 110*(i+1)*sine.length/sampleRate;
	}

	/**
	 * When changing the parameter using the GUI, also update the number of
	 * oscillators used in calculation.
	 */
	public void setParams(double param1) {
		super.setParams(param1);
		setStressParameter((int) (10 * param1));
	}

	/**
	 * Subclasses should define the way the do the interpolation.
	 * @param i
	 * @return
	 */
	protected abstract float interp(float i);
}