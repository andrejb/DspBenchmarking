package br.usp.ime.dspbenchmarking;

import java.io.IOException;
import java.io.InputStream;

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

	// Audio variables
	static final int AUDIO_SOURCE_MIC = 0;
	static final int AUDIO_SOURCE_WAV = 1;
	private int audioSource;
	private InputStream inputStream;
	AudioTrack track = null;
	short[] buffer = null;
	final int BUFFER_SIZE_IN_BLOCKS = 256;
	int jx = 0;

	// Time tracking variables
	private long sampleWriteTime = 0; // sum of time needed to write samples
	private long dspCycleTime = 0; // sum of dsp cycle times
	private final int sampleRate = 44100; // the system sample rate
	private int blockSize = 64; // the block period in samples
	private long callbackTicks = 0; // how many times DSP callback ran
	private long elapsedTime = 0; // total DSP elapsed time
	private long startTime;

	// DSP parameters
	private DspAlgorithm dspAlgorithm; // the chosen algorithm
	private double parameter1;
	private int maxDspCycles = 0;

	// Stream
	AudioStream audioStream = null;

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
	 * Initializes instance variables.
	 * 
	 * @param bSize
	 * @param algorithm
	 */
	private void Init(int bSize, int algorithm, InputStream stream, int source) {
		// sets higher priority for real time purposes
		inputStream = stream;
		audioSource = source;
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		blockSize = bSize;
		setAlgorithm(algorithm);
	}

	/**
	 * Initiates microphone audio stream.
	 */
	private void startRecorder() {
		if (audioSource == AUDIO_SOURCE_WAV)
			try {
				// create the stream and the buffer.
				audioStream = new WavStream(inputStream, blockSize);
				buffer = audioStream.createBuffer();
				audioStream.scheduleDspCallback(
						new DspCallback(audioStream.blocks()),
						(long) (1000000 * getBlockPeriod()));

			} catch (IOException e) {
				Log.e("startRecorder", "IOException: " + e);
				e.printStackTrace();
			}
		else if (audioSource == AUDIO_SOURCE_MIC) {
			// create the buffer.
			audioStream = new MicStream(BUFFER_SIZE_IN_BLOCKS * blockSize,
					sampleRate, blockSize);
			buffer = audioStream.createBuffer();
			audioStream.scheduleDspCallback(new DspCallback(
					BUFFER_SIZE_IN_BLOCKS), 0);
			// buffer = new short[BUFFER_SIZE_IN_BLOCKS * blockSize];

		}
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
			callbackTicks++;
			// Convert from PCM shorts to doubles
			double[] performBuffer = new double[blockSize];
			for (int i = 0; i < blockSize; i++)
				performBuffer[i] = (double) buffer[(jx % wrap) * blockSize + i] / 65536;
			long time1;
			// calls DSP perform for selected algorithm
			time1 = SystemClock.uptimeMillis();
			dspAlgorithm.perform(performBuffer);
			dspCycleTime += (SystemClock.uptimeMillis() - time1);
			// Convert from doubles to PCM shorts
			for (int i = 0; i < blockSize; i++)
				buffer[(jx % wrap) * blockSize + i] = (short) (performBuffer[i] * 65536);
			// Write to the system buffer.
			time1 = SystemClock.uptimeMillis();
			if (track != null) {
				track.write(buffer, (jx++ % wrap) * blockSize, blockSize);
				// track.write(outputBuffer, 0, blockSize);
			}
			sampleWriteTime += (SystemClock.uptimeMillis() - time1);
			elapsedTime = SystemClock.uptimeMillis() - startTime;
			
			// Stop if reaches maximum of dsp cycles
			if (callbackTicks == maxDspCycles)
				stopRunning();
		}
	}

	/**
	 * This is called when the thread starts.
	 */
	@Override
	public void run() {
		isRunning = true;
		try {
			// turn on audio input and output.
			startRecorder();
			startAudioTrack();
			// execute the read loop until DSP toggles.
			startTime = SystemClock.uptimeMillis();
			audioStream.readLoop(buffer);
			// free audio resources.

		} catch (Throwable x) {
			Log.w("Audio", "Error reading voice audio", x);
		} finally {
		}

	}

	/**
	 * Starts the player.
	 */
	private void startAudioTrack() {
		//Log.e("startAudioTrack", "audioStream="+audioStream);
		track = new AudioTrack(AudioManager.STREAM_MUSIC, // stream type (could
															// be
															// STREAM_VOICECALL).
				sampleRate, // sample rate.
				AudioFormat.CHANNEL_OUT_MONO, // channel configuration.
				AudioFormat.ENCODING_PCM_16BIT, // channel encoding.
				audioStream.getMinBufferSize() * 10, // buffer size.
				AudioTrack.MODE_STREAM); // streaming or static buffer.
		track.play();
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
		if (audioStream != null)
			audioStream.stopRunning();
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
		return (double) sampleWriteTime / callbackTicks;
	}

	// getter
	public double getDspCycleMeanTime() {
		return (double) dspCycleTime / callbackTicks;
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
}
