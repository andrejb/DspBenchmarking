package br.usp.ime.dspbenchmarking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ToggleButton;

public class TestActivity extends StatsActivity {

	// Test config
	private static final int MAX_DSP_CYCLES = 100;

	// Views
	private ToggleButton toggleTests = null;
	private ProgressBar workingBar = null;
	private ProgressBar progressBar = null;

	// Output file specifications
	final String dirName = "DspBenchmarking";
	final String fileName = "/dsp-benchmark-results-";
	final String dateFormat = "yyyy-MM-dd_HH-mm-ss";
	OutputStream os;

	// External storage state
	BroadcastReceiver mExternalStorageReceiver;
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;

	/**
	 * 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set the view
		setContentView(R.layout.tests);
		super.onCreate(savedInstanceState);

		// Finds toggle button
		toggleTests = (ToggleButton) findViewById(R.id.toggleTests);
		toggleTests.setTextOff("start");
		toggleTests.setTextOn("running tests...");

		// Find working bar
		workingBar = (ProgressBar) findViewById(R.id.workingBar);
		workingBar.setVisibility(ProgressBar.INVISIBLE);

		// Find progress bar
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
	}

	/**
	 * 
	 * @return
	 */
	private OutputStream getOutputStream() {
		// Get info on external storage
		try {
			updateExternalStorageState();
		} catch (IOException e) {
			Log.e("TestActivity", "No writeable media found.");
			e.printStackTrace();
		}
		File prefix = Environment.getExternalStorageDirectory();
		// Create dir
		File dir = new File(prefix, dirName);
		dir.mkdirs();
		// Create output file
		File outputFile = new File(dir, getFileName());
		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		// Writes device info on file
		try {
			outputStream.write(getBuildInfo().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return outputStream;
	}

	/**
	 * 
	 * @throws IOException
	 */
	void updateExternalStorageState() throws IOException {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		if (!mExternalStorageWriteable)
			throw new IOException();
	}

	/**
	 * 
	 * @return
	 */
	private String getFileName() {
		// Generate file name
		StringBuffer sbuf = new StringBuffer(fileName);
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		sdf.format(new Date(), sbuf, new FieldPosition(0));
		sbuf.append(".txt");
		return sbuf.toString();
	}

	private String getBuildInfo() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("# board: " + Build.BOARD + "\n");
		// sbuf.append("# bootloader: "+Build.BOOTLOADER+"\n");
		sbuf.append("# brand: " + Build.BRAND + "\n");
		sbuf.append("# cpu_abi: " + Build.CPU_ABI + "\n");
		// sbuf.append("# cpu_abi2: "+Build.CPU_ABI2+"\n");
		sbuf.append("# device: " + Build.DEVICE + "\n");
		sbuf.append("# display: " + Build.DISPLAY + "\n");
		sbuf.append("# fingerprint: " + Build.FINGERPRINT + "\n");
		// sbuf.append("# hardware: "+Build.HARDWARE+"\n");
		sbuf.append("# host: " + Build.HOST + "\n");
		sbuf.append("# id: " + Build.ID + "\n");
		sbuf.append("# manufacturer: " + Build.MANUFACTURER + "\n");
		sbuf.append("# model: " + Build.MODEL + "\n");
		sbuf.append("# product: " + Build.PRODUCT + "\n");
		// sbuf.append("# serial: "+Build.SERIAL+"\n");
		sbuf.append("# tags: " + Build.TAGS + "\n");
		sbuf.append("# time: " + Build.TIME + "\n");
		sbuf.append("# type: " + Build.TYPE + "\n");
		sbuf.append("# user: " + Build.USER + "\n");
		return sbuf.toString();
	}

	/**
	 * 
	 * @param v
	 */
	public void toggleTests(View v) {
		if (toggleTests.isChecked()) {
			toggleTests.setClickable(false);
			workingBar.setVisibility(ProgressBar.VISIBLE);
			initTests();
		} else {
			turnOff();
		}
	}

	/**
	 * 
	 */
	private void releaseDspThread() {
		if (dt != null) {
			dt.stopRunning();
			dt = null;
		}
	}

	/**
	 * 
	 */
	private void turnOff() {
		releaseDspThread();
		workingBar.setVisibility(ProgressBar.INVISIBLE);
		toggleTests.toggle();
		toggleTests.setClickable(true);
	}

	/**
	 * 
	 */
	private void initTests() {
		TestThread mt = new TestThread(mHandler);
		mt.start();
	}

	/**
	 * 
	 * @author andre
	 * 
	 */
	private class TestThread extends Thread {

		private Handler mHandler;

		public TestThread(Handler handler) {
			mHandler = handler;
		}

		@Override
		public void run() {
			// Opens results file
			os = getOutputStream();

			// Get input stream
			try {


				// Iterate through power of 2 blocks
				for (int i = 6; i <= 13; i++) {
					// run tests
					Log.e("Tests", "init test with block = "+Math.pow(2,i));
					
					InputStream is = getResources().openRawResourceFd(R.raw.alien_orifice)
							.createInputStream();
					is.mark(2000000);
					
					performTest((int) Math.pow(2, i), 1, is);

					// Wait for tests to end
					while (dt.isRunning())
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							Log.e("ERROR", "Thread was Interrupted");
						}

					// Write results to file
					try {
						os.write(getDspThreadInfo());
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					// Close input stream
					is.close();
					is = null;
					releaseDspThread();
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						Log.e("ERROR", "Thread was Interrupted");
					}
					System.gc();
					// wait for garbage collector
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						Log.e("ERROR", "Thread was Interrupted");
					}
					
					// increase status bar
					progressBar.setProgress((int) ((double) (i-5) * 100.0 / 8));
				}


			} catch (Exception e) {
				e.printStackTrace();
			}

			// Close the output file
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Turn off when done.
			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			msg.setData(b);
			mHandler.sendMessage(msg);
		}

		private void performTest(int bSize, int algorithm, InputStream is) {
			dt = new DspThread(bSize, algorithm, is, MAX_DSP_CYCLES);
			dt.setParams(0.5);
			dt.start();
		}
		
		/**
		 * Generates a byte array with statistics from the DSP thread
		 * @return
		 */
		private byte[] getDspThreadInfo() {
			String output = "";
			output += dt.getBlockSize() + " ";
			output += dt.getElapsedTime() + " ";
			output += dt.getCallbackTicks() + " ";
			output += dt.getReadTicks() + " ";
			output += dt.getSampleReadMeanTime() + " ";
			output += dt.getSampleWriteMeanTime() + " ";
			output += dt.getDspCycleMeanTime() + " ";
			output += dt.getBlockPeriod() + " ";
			output += dt.getCallbackPeriodMeanTime() + "\n";
			return output.getBytes();
		}
	}
	

	/************************************************************************
	 * message handler.
	 ***********************************************************************/
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			turnOff();
		}
	};

}
