package br.usp.ime.dspbenchmarking.fftw;

public class FFTWAlgorithm extends br.usp.ime.dspbenchmarking.DspAlgorithm {
	
	public FFTWAlgorithm(int sRate, int bSize) {
		super(sRate, bSize);
		// TODO Auto-generated constructor stub
		
	}

	@Override
	public void perform(double[] buffer) {
		// TODO Auto-generated method stub
		FFTW.setMonothread();
		FFTW.execute(buffer);
	}

}
