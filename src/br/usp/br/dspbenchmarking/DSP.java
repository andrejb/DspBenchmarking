package br.usp.br.dspbenchmarking;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class DSP extends Activity {

	// Time measurement
	private int cpuUsage;
	private long sampleReadTime = 0;
	private long sampleWriteTime = 0;
	private long dspCycleTime = 0;
	private int minBufferSize;

	// Threads
	private SystemWatchThread swt;
	private DSPThread dt;

	// DSP
	private final int SR = 44100; // Sample Rate in Hz.
	private int blockSize = 1; // in samples.
	private long ticks = 0; // how many DSP cycles so far.
	private float blockPeriod;

	// Views
	private CheckBox c;
	private ProgressBar cpuUsageBar;
	private TextView dspBlockSizeView = null;
	private TextView sampleRetrieveTimeView = null;
	private TextView sampleWriteTimeView = null;
	private TextView dspCycleTimeView = null;
	private ProgressBar dspCycleTimeBar = null;
	private TextView dspPeriodView = null;
	private TextView dspCyclesView = null;

	/************************************************************************
	 * onCreate Calles when the activity is first created.
	 ***********************************************************************/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dsp);
		cpuUsageBar = (ProgressBar) findViewById(R.id.cpu_usage);
		dspBlockSizeView = (TextView) findViewById(R.id.dspBlockSizeValue);
		sampleRetrieveTimeView = (TextView) findViewById(R.id.meanSampleRetrieveTimeValue);
		sampleWriteTimeView = (TextView) findViewById(R.id.meanSampleWriteTimeValue);
		dspCycleTimeView = (TextView) findViewById(R.id.meanDspCycleTimeValue);
		dspCycleTimeBar = (ProgressBar) findViewById(R.id.dspCycleBar);
		dspPeriodView = (TextView) findViewById(R.id.dspPeriodValue);
		dspCyclesView = (TextView) findViewById(R.id.dspCyclesValue);

		// radio buttons listener
		RadioButton rb;
		rb = (RadioButton) findViewById(R.id.radioDsp1);
		rb.setOnClickListener(dspRadioListener);
		rb = (RadioButton) findViewById(R.id.radioDsp64);
		rb.setOnClickListener(dspRadioListener);
		rb = (RadioButton) findViewById(R.id.radioDsp128);
		rb.setOnClickListener(dspRadioListener);
		rb = (RadioButton) findViewById(R.id.radioDsp256);
		rb.setOnClickListener(dspRadioListener);
		rb = (RadioButton) findViewById(R.id.radioDsp512);
		rb.setOnClickListener(dspRadioListener);

		// Init vars
		blockPeriod = ((float) blockSize) / SR;

	}

	/************************************************************************
	 * message handler.
	 ***********************************************************************/
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			double dspMean;

			// Text Views
			dspMean = (double) dspCycleTime / ticks;
			dspBlockSizeView.setText(Long.toString(blockSize));
			sampleRetrieveTimeView.setText(String.format("%.6f",
					(double) sampleReadTime / ticks));
			sampleWriteTimeView.setText(String.format("%.6f",
					(double) sampleWriteTime / ticks));
			dspCycleTimeView.setText(String.format("%.6f", dspMean));
			dspPeriodView.setText(String.format("%.6f", blockPeriod));
			dspCyclesView.setText(Long.toString(ticks));

			// Progress Bars
			cpuUsageBar.setProgress(cpuUsage);
			dspCycleTimeBar.setProgress((int) ((dspMean / blockPeriod) * 100));

		}
	};

	/************************************************************************
	 * This turns FFT processing on and off.
	 ***********************************************************************/
	public void toggleDSP(View v) {
		c = (CheckBox) findViewById(R.id.toggle_dsp);
		cpuUsageBar = (ProgressBar) findViewById(R.id.cpu_usage);

		if (c.isChecked()) {
			// Values
			sampleReadTime = 0;
			sampleWriteTime = 0;
			dspCycleTime = 0;
			ticks = 0;
			// Threads
			swt = new SystemWatchThread(mHandler);
			dt = new DSPThread();
			swt.start();
			dt.start();
		//mProgressStatus = (int) readUsage() * 100;
		} else {
			try {
				cpuUsage = 0;
				swt.stopRunning();
				dt.stopRunning();
				swt = null;
				dt = null;
			}
			catch (SecurityException e) {
				e.printStackTrace();
			}
		}
		// Start lengthy operation in a background thread
		//setContentView(R.layout.fft);
		//EditText et = (EditText) findViewById(R.id.texto1);
        //et.setText(mProgress.toString());
	}

	/************************************************************************
	 * Changes block size.
	 ***********************************************************************/
	private OnClickListener dspRadioListener = new OnClickListener() {
		public void onClick(View v) {
			// Perform action on clicks
			RadioButton rb = (RadioButton) v;
			blockSize = Integer.parseInt(rb.getText().toString());
			blockPeriod = ((float) blockSize) / SR * 1000;
			// sampleReadTime = 0;
			// sampleWriteTime = 0;
			// dspCycleTime = 0;
			// ticks = 0;
		}
	};

	/************************************************************************
	 * Gets CPU usage from /proc/stat.
	 ***********************************************************************/
	private float readUsage() {
		try {
			RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
			String load = reader.readLine();

			String[] toks = load.split(" ");

			long idle1 = Long.parseLong(toks[5]);
			long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
					+ Long.parseLong(toks[4]) + Long.parseLong(toks[6])
					+ Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			try {
				Thread.sleep(360);
			} catch (Exception e) {
				e.printStackTrace();
			}

			reader.seek(0);
			load = reader.readLine();
			reader.close();

			toks = load.split(" ");

			long idle2 = Long.parseLong(toks[5]);
			long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
					+ Long.parseLong(toks[4]) + Long.parseLong(toks[6])
					+ Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			return (float) (cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return 0;
	}

	/************************************************************************
	 * SystemWatchThread Monitors system parameters and sends messages to the
	 * main thread.
	 ***********************************************************************/
	private class SystemWatchThread extends Thread {

		// Class constants defining state of the thread
		private boolean isRunning = false;

		Handler mHandler;
		int total;

		// Constructor with an argument that specifies Handler on main thread
		// to which messages will be sent by this thread.
		SystemWatchThread(Handler h) {
			mHandler = h;
		}

		// This is called when the thread starts. It monitors device parameters
		@Override
		public void run() {
			isRunning = true;
			total = 100;
			while (isRunning) {
				// The method Thread.sleep throws an InterruptedException if
				// Thread.interrupt()
				// were to be issued while thread is sleeping; the exception
				// must be caught.
				float usage = readUsage();
				cpuUsage = (int) (usage * 100.0);
				if (cpuUsage >= 100)
					cpuUsage = 99;

				try {
					// Control speed of update (but precision of delay not
					// guaranteed)
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Log.e("ERROR", "Thread was Interrupted");
				}

				// Send message (with current value of total as data) to Handler
				// on UI thread
				// so that it can update the progress bar.

				Message msg = mHandler.obtainMessage();
				Bundle b = new Bundle();
				b.putInt("total", total);
				msg.setData(b);
				mHandler.sendMessage(msg);

				total--; // Count down
			}
		}

		// Set current state of thread (use state=ProgressThread.DONE to stop
		// thread)
		public boolean stopRunning() {
			if (isRunning == false)
				return false;
			isRunning = false;
			return true;
		}
	}

	/************************************************************************
	 * SystemWatchThread Monitors system parameters and sends messages to the
	 * main thread.
	 ***********************************************************************/
	private class DSPThread extends Thread {

		// Class constants defining state of the thread
		private boolean isRunning = false;

		// Handler mHandler;

		AudioRecord recorder = null;
		AudioTrack track = null;
		short[][] buffers = new short[256][160];
		int ix = 0;

		// Constructor with an argument that specifies Handler on main thread
		// to which messages will be sent by this thread.
		DSPThread() {
			// sets higher priority for realtime purposes
			android.os.Process
					.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			// mHandler = h;
		}

		// This is called when the thread starts. It monitors device parameters
		@Override
		public void run() {
			// Exits if we're already running.
			// if (isRunning != false)
			// return;

			isRunning = true;

			long timed1, timed2;
			long times1, times2;

			sampleReadTime = 0;
			sampleWriteTime = 0;
			dspCycleTime = 0;

			try {
				int N = AudioRecord.getMinBufferSize(SR,
						AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT);
				minBufferSize = N;
				recorder = new AudioRecord(AudioSource.MIC, // audio source
															// (could be
															// VOICE_UPLINK).
						SR, // sample rate (Hz) -- 44.100 is the only guaranteed
							// to work on all devices.
						AudioFormat.CHANNEL_IN_MONO, // channel configuration
														// (could be
														// CHANNEL_IN_STEREO,
														// not guaranteed to
														// work).
						AudioFormat.ENCODING_PCM_16BIT, // channel encoding
														// (could be
														// ENCODING_PCM_8BIT,
														// not guaranteed to
														// work).
						N * 10); // buffer size.
				track = new AudioTrack(AudioManager.STREAM_MUSIC, // stream type
																	// (could be
																	// STREAM_VOICECALL).
						SR, // sample rate.
						AudioFormat.CHANNEL_OUT_MONO, // channel configuration.
						AudioFormat.ENCODING_PCM_16BIT, // channel encoding.
						N * 10, // buffer size.
						AudioTrack.MODE_STREAM); // streaming or static buffer.
				recorder.startRecording();
				track.play();

				// -------------------------------------------------------------------
				// ____ ____ ____
				// | _ \/ ___|| _ \
				// | | | \___ \| |_) |
				// | |_| |___) | __/
				// |____/|____/|_|
				// -------------------------------------------------------------------
				int readSize = 160;

				while (isRunning) {
					ticks++;
					timed1 = SystemClock.uptimeMillis();

					short[] buffer = buffers[ix++ % buffers.length];

					times1 = SystemClock.uptimeMillis();
					// Log.i("Map", "Writing new data to buffer");
					N = recorder.read(buffer, 0, readSize);
					times2 = SystemClock.uptimeMillis();
					sampleReadTime += (times2 - times1);

					times1 = SystemClock.uptimeMillis();
					track.write(buffer, 0, readSize);
					times2 = SystemClock.uptimeMillis();
					sampleWriteTime += (times2 - times1);

					timed2 = SystemClock.uptimeMillis();
					dspCycleTime += (timed2 - timed1);
					// sleep until next DSP cycle
				}
				releaseIO();
			} catch (Throwable x) {
				Log.w("Audio", "Error reading voice audio", x);
			} finally {
				releaseIO();
			}

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
		private boolean stopRunning() {
			if (isRunning == false)
				return false;
			isRunning = false;
			return true;
		}
	}

}