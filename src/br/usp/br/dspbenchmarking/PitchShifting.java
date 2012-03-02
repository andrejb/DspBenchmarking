package br.usp.br.dspbenchmarking;

public class PitchShifting extends DspAlgorithm {

	public PitchShifting(int sRate, int bSize) {
		super(sRate, bSize);
		// TODO Auto-generated constructor stub
	}

	public void perform(short[] buffer) {
		// Pitch Shifting parameters
		int N = this.getBlockSize();						// block length
		int Sa = N / 2;										// analysis hop size
		double alpha = this.getParameter1() * 1.75 + 0.25;	// pitch scaling factor
		int L = (int) (256 * alpha / 2);						// overlap interval
		int M = (int) Math.ceil(this.getBlockSize() / Sa);
		int Ss = (int) Math.round(Sa*alpha);
		
		buffer[M*Sa+N] = 0;
		
		// Time Stretching using alpha2 = 1/alpha
		double alpha2 = 1.0/alpha;
		for (int ni = 0; ni < M-1; ni++) {
			int grainStart = ni*Sa+1;
			int grainEnd = N+ni*Sa;
			
		}
	}

}
