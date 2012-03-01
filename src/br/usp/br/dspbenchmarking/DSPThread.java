package br.usp.br.dspbenchmarking;

import java.io.IOException;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.os.SystemClock;
import android.util.Log;

/************************************************************************
 * This carries on all DSP related stuff.
 ***********************************************************************/
public class DSPThread extends Thread {

	// Class variable defining state of the thread
	private boolean isRunning = false;

	// Audio variables
	static final int AUDIO_SOURCE_MIC = 0;
	static final int AUDIO_SOURCE_FILE = 1;
	private int audioSource;
	AudioRecord recorder = null;
	AudioTrack track = null;
	short[][] buffers = new short[256][512];
	int ix = 0;
	int jx = 0;

	// Time tracking variables
	private long sampleReadTime = 0; // sum of time needed to read samples
	private long sampleWriteTime = 0; // sum of time needed to write samples
	private long dspCycleTime = 0; // sum of dsp cycle times
	private int sampleRate = 44100; // the system sample rate
	private int blockSize = 64; // the block period in samples
	private long callbackPeriod = 0; // sum of periods of callback calling
	private long readTicks = 0; // how many times we read from the buffer
	private long callbackTicks = 0; // how many times DSP callback ran
	private long elapsedTime = 0; // total DSP elapsed time
	private long startTime;

	// DSP parameters
	private DSPAlgorithm dspAlgorithm; // the chosen algorithm
	private double parameter1;

	// Stream
	WavStream wavStream = null;

	/**
	 * Crates a DSP thread with input from MIC
	 * @param bSize
	 * @param algorithm
	 */
	DSPThread(int bSize, int algorithm) {
		// sets higher priority for real time purposes
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		blockSize = bSize;
		setAlgorithm(algorithm);
		// Sets audio source
		audioSource = AUDIO_SOURCE_MIC;
		recorder = new AudioRecord(AudioSource.MIC, // Audio source
													// (could be
													// VOICE_UPLINK).
				sampleRate, // Sample rate (Hz) -- 44.100 is the only
							// guaranteed
				// to work on all devices.
				AudioFormat.CHANNEL_IN_MONO, // Channel configuration
												// (could be
												// CHANNEL_IN_STEREO,
												// not guaranteed to
												// work).
				AudioFormat.ENCODING_PCM_16BIT, // Channel encoding
												// (could be
												// ENCODING_PCM_8BIT,
												// not guaranteed to
												// work).
				getMinBufferSize() * 10); // buffer size.
		recorder.setPositionNotificationPeriod(blockSize);
		recorder.setRecordPositionUpdateListener(dspCallback);
		recorder.startRecording();
	}

	/**
	 * Creates a DSP Thread with file input
	 * @param bSize
	 * @param algorithm
	 * @param filePath
	 */
	DSPThread(int bSize, int algorithm, String filePath) {
		// sets higher priority for real time purposes
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		blockSize = bSize;
		setAlgorithm(algorithm);
		audioSource = AUDIO_SOURCE_FILE;
		try {
			wavStream = new WavStream(filePath);

		} catch (IOException e) {

		}
	}

	private void setAlgorithm(int algorithm) {
		// set the DSP algorithm
		if (algorithm == 0)
			dspAlgorithm = new Loopback(sampleRate, blockSize);
		else if (algorithm == 1)
			dspAlgorithm = new Reverb(sampleRate, blockSize);
		this.setParams(parameter1);
	}

	// -------------------------------------------------------------------
	// ____ ____ ____
	// | _ \/ ___|| _ \
	// | | | \___ \| |_) |
	// | |_| |___) | __/
	// |____/|____/|_|
	// -------------------------------------------------------------------
	AudioRecord.OnRecordPositionUpdateListener dspCallback = new AudioRecord.OnRecordPositionUpdateListener() {
		private long lastListenerStartTime = 0;

		public void onPeriodicNotification(AudioRecord recorder) {
			long time1;

			// Takes note of time between listeners
			long startTime = SystemClock.uptimeMillis();
			if (lastListenerStartTime != 0)
				callbackPeriod += (startTime - lastListenerStartTime);
			lastListenerStartTime = startTime;
			callbackTicks++;

			// calls DSP perform for selected algorithm
			time1 = SystemClock.uptimeMillis();
			dspAlgorithm.perform(buffers[jx % buffers.length]);
			dspCycleTime += (SystemClock.uptimeMillis() - time1);

			// Write to the system buffer.
			time1 = SystemClock.uptimeMillis();
			if (track != null)
				track.write(buffers[jx++ % buffers.length], 0, blockSize);
			sampleWriteTime += (SystemClock.uptimeMillis() - time1);

			// if (callbackTicks == 1000)
			// stopRunning();
		}

		public void onMarkerReached(AudioRecord recorder) {
		}
	};

	
	// This is called when the thread starts. It monitors device parameters
	@Override
	public void run() {
		isRunning = true;

		// long timed1, timed2;
		long times1, times2;

		try {

			startAudioTrack();

			startTime = SystemClock.uptimeMillis();
			while (isRunning) {
				readTicks++;
				short[] buffer = buffers[ix++ % buffers.length];

				times1 = SystemClock.uptimeMillis();
				// Log.i("Map", "Writing new data to buffer");
				if (audioSource == AUDIO_SOURCE_MIC)
					if (recorder != null)
						recorder.read(buffer, 0, blockSize);
				if (audioSource == AUDIO_SOURCE_FILE)
					wavStream.getFromBuffer(buffer, 0, blockSize);
				times2 = SystemClock.uptimeMillis();
				sampleReadTime += (times2 - times1);

				elapsedTime = SystemClock.uptimeMillis() - startTime;
				// if (elapsedTime > (100000.0 * (float) blockSize / sampleRate))
				//     stopRunning();
			}
			releaseIO();
		} catch (Throwable x) {
			Log.w("Audio", "Error reading voice audio", x);
		} finally {
			releaseIO();
		}

	}
	
	private void startAudioTrack() {
		track = new AudioTrack(AudioManager.STREAM_MUSIC, // stream type (could be STREAM_VOICECALL).
				sampleRate, // sample rate.
				AudioFormat.CHANNEL_OUT_MONO, // channel configuration.
				AudioFormat.ENCODING_PCM_16BIT, // channel encoding.
				getMinBufferSize() * 10, // buffer size.
				AudioTrack.MODE_STREAM); // streaming or static buffer.
		track.play();
	}

	private int getMinBufferSize() {
		return AudioRecord.getMinBufferSize(sampleRate,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
	}

	// Releases Input and Output stuff.
	private boolean releaseIO() {
		if (isRunning == true)
			return false;

		if (recorder != null) {
			recorder.stop();
			recorder.release();
			recorder = null;
		}
		if (track != null) {
			track.stop();
			track.release();
			track = null;
		}

		return true;
	}

	// Stops thread.
	public boolean stopRunning() {
		if (isRunning == false)
			return false;
		isRunning = false;
		return true;
	}

	// setter
	public void setParams(double param1) {
		parameter1 = param1;
		dspAlgorithm.setParams(param1);
	}

	// accessor
	public float getSampleReadMeanTime() {
		return (float) sampleReadTime / readTicks;
	}

	// accessor
	public float getSampleWriteMeanTime() {
		return (float) sampleWriteTime / callbackTicks;
	}

	// accessor
	public float getDspCycleMeanTime() {
		return (float) dspCycleTime / callbackTicks;
	}

	// accessor
	public int getSampleRate() {
		return sampleRate;
	}

	// accessor
	public int getBlockSize() {
		return blockSize;
	}

	// accessor
	public float getCallbackPeriodMeanTime() {
		return (float) callbackPeriod / (callbackTicks - 1);
	}

	// accessor
	public long getReadTicks() {
		return readTicks;
	}

	// accessor
	public long getCallbackTicks() {
		return callbackTicks;
	}

	// accessor
	public float getBlockPeriod() {
		return ((float) blockSize) / sampleRate * 1000;
	}

	// accessor
	public float getElapsedTime() {
		return (float) elapsedTime;
	}
}
