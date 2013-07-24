package br.usp.ime.dspbenchmarking.threads;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;

import br.usp.ime.dspbenchmarking.algorithms.AdditiveSynthesisLookupTableTruncated;
import br.usp.ime.dspbenchmarking.algorithms.AdditiveSynthesisSine;
import br.usp.ime.dspbenchmarking.algorithms.AdditiveSynthesisLookupTableCubic;
import br.usp.ime.dspbenchmarking.algorithms.AdditiveSynthesisLookupTableLinear;
import br.usp.ime.dspbenchmarking.algorithms.DspAlgorithm;
import br.usp.ime.dspbenchmarking.algorithms.FftAlgorithm;
import br.usp.ime.dspbenchmarking.algorithms.Loopback;
import br.usp.ime.dspbenchmarking.algorithms.Reverb;
import br.usp.ime.dspbenchmarking.algorithms.Convolution;
import br.usp.ime.dspbenchmarking.algorithms.StressAlgorithm;
import br.usp.ime.dspbenchmarking.algorithms.fftw.FFTWAlgorithm;
import br.usp.ime.dspbenchmarking.algorithms.fftw.FFTWMultithreadAlgorithm;
import br.usp.ime.dspbenchmarking.algorithms.jtransforms.fft.DoubleFFT_1D1TAlgorithm;
import br.usp.ime.dspbenchmarking.algorithms.jtransforms.fft.DoubleFFT_1D2TAlgorithm;
import br.usp.ime.dspbenchmarking.streams.AudioStream;
import br.usp.ime.dspbenchmarking.streams.MicStream;
import br.usp.ime.dspbenchmarking.streams.WavStream;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.SystemClock;
import android.util.Log;


/**
 * The thread that controls and performs DSP. It is responsible for:
 * 	- I/O instantiation and release.
 *  - DSP parameters control.
 *  - Processing audio blocks.
 *  
 *  The thread is started in STATE_SUSPENDED state and resumeDsp() has to
 *  be called to start processing.
 *  
 *  The method suspendDsp() may be called to stop processing (and
 *  release I/O stuff).
 *  
 *  To kill the thread, use stopDspThread().
 */
public class DspThread extends Thread {

	// DSP parameters
	private final int sampleRate = 44100; // the system sample rate
	private int blockSize; // the block period in samples
	private DspAlgorithm dspAlgorithm; // the chosen algorithm
	private DspThread.AlgorithmEnum algorithm;
	private double parameter1;
	private int maxDspCycles;  // 0 means infinite
	private final int DEFAULT_BLOCK_SIZE = 64;

	// Audio source variables
	static public final int AUDIO_SOURCE_MIC = 0;
	static public final int AUDIO_SOURCE_WAV = 1;
	private int audioSource;

	// Input stream variables
	private InputStream inputStream = null;  // suppose we will receive from MIC
	AudioTrack track = null;
	short[] buffer = null;
	final int BUFFER_SIZE_IN_BLOCKS = 256;
	int jx = 0;
	double[] performBuffer = null;
	
	// Time tracking variables
	private long sampleWriteTime = 0; // sum of time needed to write samples
	private long dspPerformTime = 0; // sum of dsp perform routine times
	private long dspCallbackTime = 0;  // sum of dsp callback times
	private long callbackTicks = 0; // how many times DSP callback ran
	private long elapsedTime = 0; // total DSP elapsed time
	private long startTime;

	// Stream
	AudioStream audioStream = null;

	//Stressing
	int stressParameter;

	// Estado
	private static final int STATE_STOPPED = 0;
	private static final int STATE_PROCESSING = 1;
	private static final int STATE_SUSPENDED = 2;
	private int state = STATE_STOPPED;

	// DSP algorithms
	private DspAlgorithm[] algorithms;
	
	public enum AlgorithmEnum { LOOPBACK, REVERB, FFT_ALGORITHM, CONVOLUTION, ADD_SYNTH_SINE,
		ADD_SYNTH_LOOKUP_TABLE_LINEAR, ADD_SYNTH_LOOKUP_TABLE_CUBIC, 
		ADD_SYNTH_LOOKUP_TABLE_TRUNCATED,
		FFTW_MONO, FFTW_MULTI, DOUBLE_FFT_1T, DOUBLE_FFT_2T,
		__NUM_ALGORITHMS
	};
	
	private EnumMap<AlgorithmEnum, DspAlgorithm> map_algorithm;


	/************************************************************************
	 * Constructor
	 ***********************************************************************/

	/**
	 * Creates a DSP Thread that runs for a maximum amount of cycles and a
	 * filterSize for stress testing.
	 */
	public DspThread() {
		// set priority for this thread
		android.os.Process
		.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		map_algorithm = new EnumMap<DspThread.AlgorithmEnum, DspAlgorithm>(AlgorithmEnum.class);
		
		map_algorithm.put(AlgorithmEnum.LOOPBACK, new Loopback(sampleRate, DEFAULT_BLOCK_SIZE));
		map_algorithm.put(AlgorithmEnum.REVERB, new Reverb(sampleRate, DEFAULT_BLOCK_SIZE));
		map_algorithm.put(AlgorithmEnum.FFT_ALGORITHM, new FftAlgorithm(sampleRate, DEFAULT_BLOCK_SIZE));
		map_algorithm.put(AlgorithmEnum.CONVOLUTION, new Convolution(sampleRate, DEFAULT_BLOCK_SIZE, 1));
		map_algorithm.put(AlgorithmEnum.ADD_SYNTH_SINE, new AdditiveSynthesisSine(sampleRate, DEFAULT_BLOCK_SIZE, 1));
		map_algorithm.put(AlgorithmEnum.ADD_SYNTH_LOOKUP_TABLE_LINEAR, new AdditiveSynthesisLookupTableLinear(sampleRate, DEFAULT_BLOCK_SIZE, 1));
		map_algorithm.put(AlgorithmEnum.ADD_SYNTH_LOOKUP_TABLE_CUBIC, new AdditiveSynthesisLookupTableCubic(sampleRate, DEFAULT_BLOCK_SIZE, 1));
		map_algorithm.put(AlgorithmEnum.ADD_SYNTH_LOOKUP_TABLE_TRUNCATED, new AdditiveSynthesisLookupTableTruncated(sampleRate, DEFAULT_BLOCK_SIZE, 1));
		map_algorithm.put(AlgorithmEnum.FFTW_MONO, new FFTWAlgorithm(sampleRate, DEFAULT_BLOCK_SIZE));
		map_algorithm.put(AlgorithmEnum.FFTW_MULTI, new FFTWMultithreadAlgorithm(sampleRate, DEFAULT_BLOCK_SIZE));
		map_algorithm.put(AlgorithmEnum.DOUBLE_FFT_1T, new DoubleFFT_1D1TAlgorithm(sampleRate, DEFAULT_BLOCK_SIZE));
		map_algorithm.put(AlgorithmEnum.DOUBLE_FFT_2T, new DoubleFFT_1D2TAlgorithm(sampleRate, DEFAULT_BLOCK_SIZE));
		
		// Instantiate algorithms
		/*algorithms = new DspAlgorithm[10];
		algorithms[0] = new Loopback(sampleRate, DEFAULT_BLOCK_SIZE);
		algorithms[1] = new Reverb(sampleRate, DEFAULT_BLOCK_SIZE);
		algorithms[2] = new FftAlgorithm(sampleRate, DEFAULT_BLOCK_SIZE);
		algorithms[3] = new Convolution(sampleRate, DEFAULT_BLOCK_SIZE, 1);
		algorithms[4] = new AdditiveSynthesisSine(sampleRate, DEFAULT_BLOCK_SIZE, 1);
		algorithms[5] = new AdditiveSynthesisLookupTableLinear(sampleRate, DEFAULT_BLOCK_SIZE, 1);
		algorithms[6] = new AdditiveSynthesisLookupTableCubic(sampleRate, DEFAULT_BLOCK_SIZE, 1);
		algorithms[7] = new AdditiveSynthesisLookupTableTruncated(sampleRate, DEFAULT_BLOCK_SIZE, 1);
		algorithms[8] = new FFTWAlgorithm(sampleRate, DEFAULT_BLOCK_SIZE);
		algorithms[9] = new FFTWMultithreadAlgorithm(sampleRate, DEFAULT_BLOCK_SIZE);*/

		// set default DSP values
		setAlgorithm(AlgorithmEnum.LOOPBACK);  // Loopback
		setBlockSize(64);  // this must be done after we have set an algorithm
		setAudioSource(AUDIO_SOURCE_MIC);
		setMaxDspCycles(0); // run indefinitelly
		state = STATE_SUSPENDED;
	}

	/**
	 * Initiate audio stream either from MIC or WAV file.
	 */
	private void setupAudioStream() {
		if (audioStream == null) {
			if (audioSource == AUDIO_SOURCE_WAV)
				try {
					// 	create the stream and the buffer.
					audioStream = new WavStream(inputStream, blockSize);
				} catch (IOException e) {
					Log.e("startRecorder", "IOException: " + e);
					e.printStackTrace();
				}
			else if (audioSource == AUDIO_SOURCE_MIC)
				// 	create the buffer.
				audioStream = new MicStream(BUFFER_SIZE_IN_BLOCKS * blockSize,
						sampleRate, blockSize);
			// general audioStream options
			buffer = audioStream.createBuffer();
			audioStream.setDspCallback(new DspPerformCallback());
		}
	}


	/**
	 * Schedule the DSP callback.
	 */
	private void scheduleDspCallback() {
		if (audioSource == AUDIO_SOURCE_WAV)
			audioStream.scheduleDspCallback((long) (1000000 * getBlockPeriod()));
		else if (audioSource == AUDIO_SOURCE_MIC)
			audioStream.scheduleDspCallback(0);
	}

	/**
	 * Reset thread statistics
	 */
	public void resetDsp() {
		sampleWriteTime = 0; // sum of time needed to write samples
		dspPerformTime = 0; // sum of dsp perform routine times
		dspCallbackTime = 0;  // sum of dsp callback times
		callbackTicks = 0; // how many times DSP callback ran
		elapsedTime = 0; // total DSP elapsed time
		if (audioStream != null)  // if run too early maybe there's no audioStream yet
			audioStream.reset();
	}

	/**
	 * Instantiate and configure the player.
	 */
	private void setupAudioTrack() {
		//Log.e("startAudioTrack", "audioStream="+audioStream);
		if (track == null) {
			track = new AudioTrack(AudioManager.STREAM_MUSIC, // stream type (could
					// be
					// STREAM_VOICECALL).
					sampleRate, // sample rate.
					AudioFormat.CHANNEL_OUT_MONO, // channel configuration.
					AudioFormat.ENCODING_PCM_16BIT, // channel encoding.
					audioStream.getMinBufferSize() * 10, // buffer size.
					AudioTrack.MODE_STREAM); // streaming or static buffer.
		}
	}

	/************************************************************************
	 * Setters/getters
	 ***********************************************************************/

	/**
	 * Set the input stream.
	 * @param stream
	 */
	public void setInputStream(InputStream stream) {
		inputStream = stream;
	}

	/**
	 * Set the audio source.
	 * @param source
	 */
	public void setAudioSource(int source) {
		audioSource = source;
	}


	/**
	 * Set the maximum number of DSP cycles this thread is allowed to run.
	 * After that, the thread becomes suspended.
	 * 
	 * @param max
	 */
	public void setMaxDspCycles(int max) {
		maxDspCycles = max;
	}

	/**
	 * Configure the algorithm used to transform the stream given a sample rate and a block size.
	 * The algorithm order must be the same as the one in 'res/values/strings.xml':
	 * 
	 *   0. Loopback.
	 *   1. Reverb.
	 *   2. FftAlgorithm.
	 *   3. PhaseVocoder.
	 *   4. StressAlgorithm.
	 *   5. Additive Synthesis.
	 *   8. FFTW monothread.
	 *   9. FFTW multithread.
	 *   
	 * @param alg The number of the algorithm.
	 */
	public void setAlgorithm(DspThread.AlgorithmEnum alg) {
		// set the DSP algorithm
		Log.i("DSP", "Defining new algorithm...");
		algorithm = alg;
		dspAlgorithm = map_algorithm.get(alg);
		dspAlgorithm.setBlockSize(blockSize);
		Log.i("DSP", "Algorithm set: "+String.valueOf(dspAlgorithm)+".");
		if (map_algorithm.get(alg) instanceof StressAlgorithm) {
			StressAlgorithm stressAlg = (StressAlgorithm) dspAlgorithm;
			stressAlg.setStressParameter(stressParameter);
		}
		setParams(parameter1);
	}
	
	/**
	 * @return The name of the current algorithm.
	 */
	public String getAlgorithmName() {
		if (dspAlgorithm == null)
			return "";
		return dspAlgorithm.getAlgorithmName();
	}

	/**
	 * Set the stress parameter used when running stress test algorithms.
	 * Will also set the parameter in the algorithm in case it is of the
	 * correct type.
	 * 
	 * @param param
	 */
	public void setStressParameter(int param) {
		stressParameter = param;
		if (dspAlgorithm instanceof StressAlgorithm) {
			((StressAlgorithm) dspAlgorithm).setStressParameter(stressParameter);
		}
	}

	/**
	 * Set the DSP block size.
	 * 
	 * @param bSize
	 */
	public void setBlockSize(int bSize) {
		Log.i("DSP", "Setting block size to "+bSize+".");
		blockSize = bSize;
		dspAlgorithm.setBlockSize(blockSize);
		if (audioStream != null)
			audioStream.setBlockSize(blockSize);
	}

	/**
	 * Set generic DSP parameters. These parameters are set either by
	 * the user or by automated tests and can be used by DSP algorithms as
	 * parameters such as filter size, gain, feedback param, etc. 
	 * 
	 * For now, there's just one param (which comes from a slider in
	 * 'res/layout/dsp.xml' view), but more can be added.
	 *  
	 * @param param1
	 */
	public void setParams(double param1) {
		Log.i("DSP", "Setting param1 to "+param1+".");
		parameter1 = param1;
		dspAlgorithm.setParams(param1);
	}

	/**
	 * @return The mean time of sample reads from input.
	 */
	public double getSampleReadMeanTime() {
		if (audioStream != null)
			if (audioStream.getReadTicks() != 0)
				return (double) audioStream.getSampleReadTime()
						/ audioStream.getReadTicks();
		return 0;
	}

	/**
	 * @return The mean time of sample write to output.
	 */
	public double getSampleWriteMeanTime() {
		if (callbackTicks > 0)
			return (double) sampleWriteTime / callbackTicks;
		return 0;
	}

	/**
	 * @return The mean DSP perform callback mean time.
	 */
	public double getDspPerformMeanTime() {
		if (callbackTicks > 0)
			return (double) dspPerformTime / callbackTicks;
		return 0;
	}

	/**
	 * @return The DSP callback mean time.
	 */
	public double getDspCallbackMeanTime() {
		if (callbackTicks > 0)
			return (double) dspCallbackTime / callbackTicks;
		return 0;
	}

	/**
	 * @return The DSP sample rate.
	 */
	public int getSampleRate() {
		return sampleRate;
	}

	/**
	 * @return The DSP block size.
	 */
	public int getBlockSize() {
		return blockSize;
	}

	/**
	 * @return The callback period mean time.
	 */
	public double getCallbackPeriodMeanTime() {
		if (audioStream != null)
			if (callbackTicks > 1)
				return (double) audioStream.getCallbackPeriod()
						/ (callbackTicks - 1);
		return 0;
	}

	/**
	 * @return The amount of blocks read from input.
	 */
	public long getReadTicks() {
		if (audioStream != null)
			return audioStream.getReadTicks();
		return 0;
	}

	/**
	 * @return The amount of times the DSP callback was executed.
	 */
	public long getCallbackTicks() {
		return callbackTicks;
	}

	/**
	 * @return The block period in milliseconds.
	 */
	public double getBlockPeriod() {
		return (double) blockSize / sampleRate * 1000;
	}

	/**
	 * @return The elapsed time since DSP was first started.
	 */
	public double getElapsedTime() {
		return (double) elapsedTime;
	}

	/**
	 * @return The current algorithm.
	 */
	public DspThread.AlgorithmEnum getAlgorithm() {
		return algorithm;
	}
	
	public String getAlgorithmNameById(DspThread.AlgorithmEnum id) {
		return map_algorithm.get(id).getAlgorithmName();
	}

	/************************************************************************
	 * DSP perform callback
	 ***********************************************************************/

	/**
	 * The DspPerformCallback is method called periodically at every DSP cycle.
	 *   - convert PCM to doubles for processing and back.
	 *   - Measure time used on computation.
	 */
	public class DspPerformCallback implements Runnable {

		/**
		 * Control the processing of one block.
		 */
		public void run() {
			callbackTicks++;  // count the number of cycles
			long time0 = SystemClock.uptimeMillis();
			// Convert from PCM shorts to doubles
			for (int i = 0; i < blockSize; i++)
				performBuffer[i] = (double) buffer[(jx * blockSize + i) % buffer.length] / 65536;
			long time1;
			//===============================================================
			// PERFORM
			//===============================================================
			// call DSP perform for selected algorithm and account for time
			time1 = SystemClock.uptimeMillis();
			dspAlgorithm.perform(performBuffer); // <------------------------
			if (callbackTicks % 100 == 0)
				Log.i("DSP", "DSP is performing...");
			dspPerformTime += (SystemClock.uptimeMillis() - time1);
			// Convert from doubles to PCM shorts
			for (int i = 0; i < blockSize; i++)
				buffer[(jx * blockSize + i) % buffer.length] = (short) (performBuffer[i] * 65536);
			// Write to the system buffer.
			time1 = SystemClock.uptimeMillis();
			if (track != null) {
				track.write(buffer, (jx++ * blockSize) % buffer.length, blockSize);
			}

			sampleWriteTime += (SystemClock.uptimeMillis() - time1);
			elapsedTime = SystemClock.uptimeMillis() - startTime;
			dspCallbackTime += SystemClock.uptimeMillis() - time0;

			// Stop if reaches maximum of dsp cycles
			if (callbackTicks == maxDspCycles) {
				suspendDsp();
			}
		}
	}

	/************************************************************************
	 * DSP control
	 ***********************************************************************/

	/**
	 * This is called when the thread starts. Runs until stopDspThread() is
	 * called.
	 */
	@Override
	public void run() {
		Log.i("DSP", "DSP thread started.");
		try {
			while (true) {
				if (isSuspended()) {
					// wait for resume
					try {
						Log.w("DSP", "DSP is suspended.");
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Log.e("ERROR", "Thread was Interrupted");
					}
				}
				// read from audio source
				else if (isProcessing()) {
					Log.i("DSP", "DSP thread starts processing with max dsp cycles.");
					// turn on audio input and output.
					setupAudioStream();
					setupAudioTrack();
					resetDsp();
					createPerformBuffer();
					scheduleDspCallback();
					track.play();
					// execute the read loop until DSP toggles.
					startTime = SystemClock.uptimeMillis();
					audioStream.readLoop(buffer);  // <-- blocking until call to audioStream.stopRunning()
					Log.i("DSP", "DSP thread finished processing...");
				}
				// thread is dead.
				else if (isStopped()) {
					Log.i("DSP", "DSP thread is stopped, bye!");
					break;
				}
			}

		} catch (Throwable x) {
			Log.w("Audio", "Error reading voice audio", x);
		} finally {
		}

	}

	/**
	 * Create a buffer to store audio samples.
	 */
	private void createPerformBuffer() {
		if (performBuffer == null || performBuffer.length != blockSize) {
			performBuffer = new double[blockSize]; 
		}
	}

	/**
	 * Stop and release audio input and output.
	 */
	private void releaseIO() {
		Log.i("DSP", "Releasing I/O...");
		// release audio input
		if (audioStream != null) {
			audioStream.stopRunning();
			audioStream = null;
		}
		// release audio output
		if (track != null) {
			track.stop();
			track.release();
			track = null;
		}		
	}

	/**
	 * Stop the thread.
	 * 
	 * @return
	 */
	public void stopDspThread() {
		// mark thread as stopped
		Log.i("DSP", "Stopping DSP...");
		state = STATE_STOPPED;
		releaseIO();
	}

	/**
	 * Suspend DSP:
	 *   - stop audio input.
	 *   - stop audio output.
	 * @return
	 */
	public void suspendDsp() {
		Log.i("DSP", "Suspending DSP...");
		state = STATE_SUSPENDED;
		audioStream.stopRunning();
		//stopPlaying();
	}

	/**
	 * Resume audio processing.
	 */
	public void resumeDsp() {
		Log.i("DSP", "Resuming DSP...");
		state = STATE_PROCESSING;
	}

	/**
	 * @return Whether audio processing is enabled.
	 */
	public boolean isProcessing() {
		return state == STATE_PROCESSING;
	}

	/**
	 * @return Whether DSP is suspended.
	 */
	public boolean isSuspended() {
		return state == STATE_SUSPENDED;
	}

	/**
	 * @return Whether DPS is stopped.
	 */
	public boolean isStopped() {
		return state == STATE_STOPPED;
	}

}
