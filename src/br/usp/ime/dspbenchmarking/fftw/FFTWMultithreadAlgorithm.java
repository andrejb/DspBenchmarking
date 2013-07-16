package br.usp.ime.dspbenchmarking.fftw;

public class FFTWMultithreadAlgorithm extends br.usp.ime.dspbenchmarking.DspAlgorithm {

	public FFTWMultithreadAlgorithm(int sRate, int bSize) {
		super(sRate, bSize);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void perform(double[] buffer) {
		// TODO Auto-generated method stub
		FFTW.setMultithread(2);
		FFTW.execute(buffer);		
	}

}
