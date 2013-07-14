package br.usp.ime.dspbenchmarking;

import java.io.IOException;
import java.io.InputStream;

import br.usp.ime.dspbenchmarking.fftw.FFTWAlgorithm;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.SystemClock;
import android.util.Log;

/************************************************************************
 * This carries on all DSP related stuff.
 ***********************************************************************/
public class DspThread extends Thread {

	// Class variable defining state of the thread
	private boolean isRunning = false;
	private boolean isSuspended = false;

	// Audio variables
	static final int AUDIO_SOURCE_MIC = 0;
	static final int AUDIO_SOURCE_WAV = 1;
	private int audioSource;
	private InputStream inputStream;
	AudioTrack track = null;
	short[] buffer = null;
	final int BUFFER_SIZE_IN_BLOCKS = 256;
	int jx = 0;
	double[] performBuffer = null;

	// Time tracking variables
	private long sampleWriteTime = 0; // sum of time needed to write samples
	private long dspPerformTime = 0; // sum of dsp perform routine times
	private long dspCallbackTime = 0;  // sum of dsp callback times
	private final int sampleRate = 44100; // the system sample rate
	private int blockSize; // the block period in samples
	private long callbackTicks = 0; // how many times DSP callback ran
	private long elapsedTime = 0; // total DSP elapsed time
	private long startTime;

	// DSP parameters
	private DspAlgorithm dspAlgorithm; // the chosen algorithm
	private double parameter1;
	private int maxDspCycles = 0;

	// Stream
	AudioStream audioStream = null;

	//Stressing
	int filterSize;

	/**
	 * Crates a DSP thread with input from MIC
	 * 
	 * @param bSize
	 * @param algorithm
	 */
	DspThread(int bSize, int algorithm) {
		Init(bSize, algorithm, null, AUDIO_SOURCE_MIC);
	}

	DspThread(int bSize, int algorithm, int cycles) {
		maxDspCycles = cycles;
		Init(bSize, algorithm, null, AUDIO_SOURCE_MIC);
	}

	/**
	 * Creates a DSP Thread with file input
	 * 
	 * @param bSize
	 * @param algorithm
	 * @param filePath
	 */
	DspThread(int bSize, int algorithm, InputStream stream) {
		// Sets audio source
		Init(bSize, algorithm, stream, AUDIO_SOURCE_WAV);
	}

	/**
	 * Creates a DSP Thread that runs for a maximum amount of cycles.
	 * 
	 * @param bSize
	 * @param algorithm
	 * @param stream
	 * @param cycles
	 */
	DspThread(int bSize, int algorithm, InputStream stream, int cycles) {
		maxDspCycles = cycles;
		Init(bSize, algorithm, stream, AUDIO_SOURCE_WAV);
	}

	/**
	 * Creates a DSP Thread that runs for a maximum amount of cycles for stress testing
	 * 
	 * @param bSize
	 * @param algorithm
	 * @param stream
	 * @param cycles
	 */
	DspThread(int bSize, int algorithm, InputStream stream, int cycles, int fSize) {
		maxDspCycles = cycles;
		filterSize = fSize;
		Init(bSize, algorithm, stream, AUDIO_SOURCE_WAV);
	}

	/**
	 * Initializes instance variables.
	 * 
	 * @param bSize
	 * @param algorithm
	 */
	private void Init(int bSize, int algorithm, InputStream stream, int source) {
		Log.i("DspThread init", "bSize="+bSize);
		Log.i("DspThread init", "algorithm="+algorithm);

		// sets higher priority for real time purposes
		inputStream = stream;
		audioSource = source;
		android.os.Process
		.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		blockSize = bSize;
		setAlgorithm(algorithm);
		setup();
	}

	/**
	 * Initiates microphone audio stream.
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
			createBuffers();
			setDspCallback();
		}
	}

	private void setDspCallback() {
		if (audioSource == AUDIO_SOURCE_WAV)
			audioStream.setDspCallback(new DspCallback(audioStream.blocks()));
		else if (audioSource == AUDIO_SOURCE_MIC)
			audioStream.setDspCallback(new DspCallback(BUFFER_SIZE_IN_BLOCKS));
	}

	private void scheduleDspCallback() {
		if (audioSource == AUDIO_SOURCE_WAV)
			audioStream.scheduleDspCallback((long) (1000000 * getBlockPeriod()));
		else if (audioSource == AUDIO_SOURCE_MIC)
			audioStream.scheduleDspCallback(0);
	}

	/**
	 * Changes the algorithm used to transform the stream.
	 * 
	 * @param algorithm
	 */
	private void setAlgorithm(int algorithm) {
		// set the DSP algorithm
		if (algorithm == 0)
			dspAlgorithm = new Loopback(sampleRate, blockSize);
		else if (algorithm == 1)
			dspAlgorithm = new Reverb(sampleRate, blockSize);
		else if (algorithm == 2)
			dspAlgorithm = new FftAlgorithm(sampleRate, blockSize);
		else if (algorithm == 3)
			dspAlgorithm = new PhaseVocoder(sampleRate, blockSize);
		else if (algorithm == 4)
			dspAlgorithm = new StressAlgorithm(sampleRate, blockSize, filterSize);
		else if (algorithm == 5)
			dspAlgorithm = new FFTWAlgorithm(sampleRate, blockSize);		
		setParams(parameter1);
	}

	/**
	 * DSP callback. Converts PCM to doubles for processing and back. Measures
	 * time used on computation.
	 */
	class DspCallback implements Runnable {
		private int wrap;

		DspCallback(int w) {
			wrap = w;
			// BUFFER_SIZE_IN_BLOCKS
		}

		public void run() {
			// private void dspCallback(int wrap) {
			//Log.e("DspThread.run()", "1");
			callbackTicks++;
			//Log.e("DspThread.run()", "2");
			long time0 = SystemClock.uptimeMillis();
			//Log.e("DspThread.run()", "3");
			// Convert from PCM shorts to doubles
			//Log.e("DspThread.run()", "blockSize="+blockSize);
			//Log.e("DspThread.run()", "jx="+jx);
			//Log.e("DspThread.run()", "buffer.length="+buffer.length);
			//Log.e("DspThread.run()", "wrap="+wrap);
			performBuffer = new double[blockSize];
			for (int i = 0; i < blockSize; i++)
				performBuffer[i] = (double) buffer[(jx * blockSize + i) % buffer.length] / 65536;
			//Log.e("DspThread.run()", "4");
			long time1;
			//Log.e("DspThread.run()", "5");
			//===============================================================
			// PERFORM
			//===============================================================
			// calls DSP perform for selected algorithm
			time1 = SystemClock.uptimeMillis();
			//Log.e("vou performar", "vou");
			dspAlgorithm.perform(performBuffer); // <------------------------
			//Log.e("vou performar", "fui");
			dspPerformTime += (SystemClock.uptimeMillis() - time1);
			// Convert from doubles to PCM shorts
			for (int i = 0; i < blockSize; i++)
				buffer[(jx * blockSize + i) % buffer.length] = (short) (performBuffer[i] * 65536);
			//Log.e("vou performar", "1");
			// Write to the system buffer.
			time1 = SystemClock.uptimeMillis();
			if (track != null) {
				track.write(buffer, (jx++ * blockSize) % buffer.length, blockSize);
				// track.write(outputBuffer, 0, blockSize);
			}
			//Log.e("vou performar", "2");
			sampleWriteTime += (SystemClock.uptimeMillis() - time1);
			elapsedTime = SystemClock.uptimeMillis() - startTime;

			dspCallbackTime += SystemClock.uptimeMillis() - time0;
			// Stop if reaches maximum of dsp cycles
			//Log.e("aqui", "terminando....");
			//Log.e("vou performar", "callbackTicks="+callbackTicks);
			//Log.e("vou performar", "maxDspCycles="+maxDspCycles);
			if (callbackTicks == maxDspCycles) {
				Log.e("DSPThread", "suspended");
				suspendDsp();
			}
			//Log.e("vou performar", "passei");
		}
	}

	private void setup() {
		// turn on audio input and output.
		setupAudioStream();
		setupAudioTrack();
	}

	public void setFilterSize(int fSize) {
		filterSize = fSize;
		StressAlgorithm alg = (StressAlgorithm) dspAlgorithm;
		alg.setFilterSize(filterSize);
	}

	public void setBlockSize(int bSize) {
		blockSize = bSize;
		dspAlgorithm.setBlockSize(blockSize);
		if (audioStream != null)
			audioStream.setBlockSize(blockSize);
	}

	private void reset() {
		sampleWriteTime = 0; // sum of time needed to write samples
		dspPerformTime = 0; // sum of dsp perform routine times
		dspCallbackTime = 0;  // sum of dsp callback times
		callbackTicks = 0; // how many times DSP callback ran
		elapsedTime = 0; // total DSP elapsed time
		audioStream.reset();
	}


	/**
	 * This is called when the thread starts. Runs until isRunning is false.
	 */
	@Override
	public void run() {
		isRunning = true;
		try {
			while (true) {
				if (isRunning)
					// NO DSP for now...
					if (isSuspended) {
						// wait for resume
						try {
							//Log.w("DSPThread", "sleeping...");
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Log.e("ERROR", "Thread was Interrupted");
						}
					}
					// read from audio source
					else {
						//Log.e("run", "1");
						reset();
						//Log.e("run", "2");
						createPerformBuffer();
						scheduleDspCallback();
						//Log.e("run", "3");
						track.play();
						//Log.e("run", "4");
						// execute the read loop until DSP toggles.
						startTime = SystemClock.uptimeMillis();
						//Log.e("run", "5");
						audioStream.readLoop(buffer);
						//Log.e("run", "6");
					}
				// thread is dead.
				else
					break;
			}

		} catch (Throwable x) {
			Log.w("Audio", "Error reading voice audio", x);
		} finally {
		}

	}
	
	private void createBuffers() {
		if (buffer == null) {
			buffer = audioStream.createBuffer();
			createPerformBuffer();
		}
	}
	
	private void createPerformBuffer() {
		 if (performBuffer == null || performBuffer.length != blockSize)
			 performBuffer = new double[buffer.length]; 
	}

	/**
	 * Starts the player.
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

	/**
	 * Releases Input and Output stuff.
	 * 
	 * @return
	 */
	public boolean releaseIO() {
		if (isRunning == true)
			return false;
		if (audioStream != null) {
			audioStream = null;
		}
		if (track != null) {
			track.stop();
			track.release();
			track = null;
		}
		return true;
	}

	/**
	 * Stops the thread.
	 * 
	 * @return
	 */
	public boolean stopRunning() {
		if (isRunning == false)
			return false;
		isRunning = false;
		isSuspended = false;
		if (audioStream != null)
			audioStream.stopRunning();
		return true;
	}

	public boolean suspendDsp() {
		if (isRunning == false)
			return false;
		isSuspended = true;
		if (audioStream != null)
			audioStream.stopRunning();
		if (track != null)
			track.stop();
		return true;
	}

	public boolean resumeDsp() {
		if (isSuspended == false)
			return false;
		isSuspended = false;
		return true;
	}

	// setter
	public void setParams(double param1) {
		parameter1 = param1;
		dspAlgorithm.setParams(param1);
	}

	// getter
	public double getSampleReadMeanTime() {
		if (audioStream != null)
			return (double) audioStream.getSampleReadTime()
					/ audioStream.getReadTicks();
		return 0;
	}

	// getter
	public double getSampleWriteMeanTime() {
		if (callbackTicks != 0)
			return (double) sampleWriteTime / callbackTicks;
		return 0;
	}

	// getter
	public double getDspPerformMeanTime() {
		if (callbackTicks != 0)
			return (double) dspPerformTime / callbackTicks;
		return 0;
	}

	// getter
	public double getDspCallbackMeanTime() {
		if (callbackTicks != 0)
			return (double) dspCallbackTime / callbackTicks;
		return 0;
	}

	// getter
	public int getSampleRate() {
		return sampleRate;
	}

	// getter
	public int getBlockSize() {
		return blockSize;
	}

	// getter
	public double getCallbackPeriodMeanTime() {
		if (audioStream != null)
			if (callbackTicks != 1)
				return (double) audioStream.getCallbackPeriod()
						/ (callbackTicks - 1);
		return 0;
	}

	// getter
	public long getReadTicks() {
		if (audioStream != null)
			return audioStream.getReadTicks();
		return 0;
	}

	// getter
	public long getCallbackTicks() {
		return callbackTicks;
	}

	// getter
	public double getBlockPeriod() {
		return (double) blockSize / sampleRate * 1000;
	}

	// getter
	public double getElapsedTime() {
		return (double) elapsedTime;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public boolean isSuspended() {
		return isSuspended;
	}

}
