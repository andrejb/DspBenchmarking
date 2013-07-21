package br.usp.ime.dspbenchmarking.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import br.usp.ime.dspbenchmarking.R;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Provides the GUI for running tests. 
 * 
 * @author andrejb
 *
 */
public abstract class TestActivity extends DspActivity {

	// Views
	protected ToggleButton toggleTestsButton = null;
	protected ProgressBar workingBar = null;
	protected ProgressBar progressBar = null;
	protected TextView algorithmName = null;
	protected TextView blockSizeView = null;


	int blockSize;
	int algorithm;
	protected int maxDspCycles;
	
	protected double LOG2 = Math.log(2);

	// Save results for writing on file and sending email
	String results = new String();

	// DSP stuff
	InputStream is;
	OutputStream os;

	// External storage state
	BroadcastReceiver mExternalStorageReceiver;
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;

	// Output file specifications
	final String dirName = "DspBenchmarking";
	protected String fileName = null;
	protected String filePrefix = "/";
	final String dateFormat = "yyyy-MM-dd_HH-mm-ss";

	/**
	 * Executes upon creation of activity, configures screen.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set the view
		super.onCreate(savedInstanceState);
		// Finds toggle button
		toggleTestsButton = (ToggleButton) findViewById(R.id.toggleTests);
		toggleTestsButton.setTextOff("start");
		toggleTestsButton.setTextOn("running tests...");
		// Find working bar
		workingBar = (ProgressBar) findViewById(R.id.workingBar);
		workingBar.setVisibility(ProgressBar.INVISIBLE);
		// Find progress bar
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		// Find algorithm and block info
		algorithmName = (TextView) findViewById(R.id.algorithmName);
		blockSizeView = (TextView) findViewById(R.id.blockSize);
		this.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}


	/**
	 * When the test finishes, send results back to previous activity and finish.
	 */
	protected void turnOff(String title) {
		sendResults(title);
		workingBar.setVisibility(ProgressBar.INVISIBLE);
		toggleTestsButton.toggle();
		toggleTestsButton.setClickable(true);
		Intent i = new Intent();
		i.putExtra("results", results);
		setResult(RESULT_OK, i);
		finish();
	}
	
	/**
	 * Stop DSP thread, mark test as canceled and finish.
	 */
	public void onBackPressed() {
		if (dt != null)
			dt.stopDspThread();
		dt = null;
		setResult(RESULT_CANCELED);
		finish();
	}

	/**
	 * 
	 * @param v
	 */
	public void toggleTests(View v) {
		if (toggleTestsButton.isChecked()) {
			toggleTestsButton.setClickable(false);
			workingBar.setVisibility(ProgressBar.VISIBLE);
			initTests();
		} else {
			turnOff("");
		}
	}


	/**
	 * Create and return a file to write results.
	 * 
	 * @return
	 */
	protected OutputStream getOutputStream() {
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
		results += getFileName() + "\n";
		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		// Writes device info on file
		try {
			outputStream.write(getBuildInfo().getBytes());
			results += getBuildInfo();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return outputStream;
	}


	/**
	 * Get formatted info about Android build.
	 * 
	 * @return
	 */
	private String getBuildInfo() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("# codename: " + android.os.Build.VERSION.CODENAME + "\n");
		sbuf.append("# incremental: " + android.os.Build.VERSION.INCREMENTAL + "\n");
		sbuf.append("# release: " + android.os.Build.VERSION.RELEASE + "\n");
		sbuf.append("# sdk_int: " + android.os.Build.VERSION.SDK_INT + "\n");
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
		sbuf.append("# user: " + Build.USER + "\n\n");
		sbuf.append("# bsize time  cbt readt sampread sampwrit blockper cbperiod perftime calltime\n");
		return sbuf.toString();
	}


	/**
	 * Start a test.
	 */
	protected void launchTest() {
		dt.setBlockSize(blockSize);
		dt.setAlgorithm(algorithm);
		dt.setMaxDspCycles(maxDspCycles);
		if (!dt.isProcessing())
			dt.start();
		else
			dt.resumeDsp();
	}


	/**
	 * Verify if external storage is present and is writable.
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
	 * Get a formatted name for the file with tests results.
	 * 
	 * @return
	 */
	protected String getFileName() {
		// Generate file name
		if (fileName == null) {
			StringBuffer sbuf = new StringBuffer(filePrefix);
			SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.US);
			sdf.format(new Date(), sbuf, new FieldPosition(0));
			sbuf.append(".txt");
			fileName = sbuf.toString();
		}
		return fileName;
	}


	/**
	 * Send results to email.
	 * 
	 * @param title
	 */
	private void sendResults(String title) {
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, results);
		String[] to = { "compmus.ime@gmail.com" };
		sendIntent.putExtra(Intent.EXTRA_EMAIL, to);
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "[dsp-benchmarking] "+title);
		sendIntent.setType("message/rfc822");
		startActivity(Intent.createChooser(sendIntent, "Send results"));	
	}

	/**
	 * Generates a byte array with statistics from the DSP thread
	 * 
	 * @return
	 */
	protected String getDspThreadInfo(int algorithm) {
		String output = "" + algorithm + " "; 								// 1  - alg
		output += String.format("%5d ", dt.getBlockSize()); 				// 2  - bsize
		output += String.format("%5.0f ", dt.getElapsedTime()); 			// 3  - time
		output += String.format("%3d ", dt.getCallbackTicks());				// 4  - cbt
		output += String.format("%5d ", dt.getReadTicks());					// 5  - read tics
		output += String.format("%8.4f ", dt.getSampleReadMeanTime());		// 6  - sample read
		output += String.format("%8.4f ", dt.getSampleWriteMeanTime());		// 7  - sample write
		output += String.format("%8.4f ", dt.getBlockPeriod());				// 8  - block period (calculated)
		output += String.format("%8.4f ", dt.getCallbackPeriodMeanTime());	// 9  - callback period
		output += String.format("%8.4f ", dt.getDspPerformMeanTime());		// 10 - perform time
		output += String.format("%8.4f ", dt.getDspCallbackMeanTime());	// 11 - callback time
		return output;
	}

	private int lastAlg = -1;
	protected void updateScreenInfo() {
		if (algorithm != lastAlg) {
			if (algorithm == 0)
				algorithmName.setText("Loopback:  ");
			else if (algorithm == 1)
				algorithmName.setText("Reverb:  ");
			else if (algorithm == 2)
				algorithmName.setText("FFT:  ");
			else if (algorithm == 3)
				algorithmName.setText("Additive Synthesis:  ");
			else if (algorithm == 4)
				algorithmName.setText("Stress:  ");
			lastAlg = algorithm;
		}
		blockSizeView.setText(String.valueOf(blockSize));
	}

	/**
	 * Initialize tests.
	 */
	protected void initTests() {
		Log.w("StressFlow", "initTests");
		// Opens results file
		os = getOutputStream();
		startControlThread();
	}

	abstract protected void startControlThread();

	/**
	 * Libera uma thread DSP:
	 *   - libera a entrada e a saída.
	 *   - pára a thread.
	 */
	protected void releaseDspThread() {
		Log.w("StressFlow", "releaseDspThread");
		if (dt != null) {
			dt.stopDspThread();
			dt = null;
		}
	}

	/**
	 * Configura um teste:
	 *   - abre um arquivo de áudio como stream de entrada.
	 */
	protected void setupTests() {
		Log.w("StressFlow", "setupTests");
		try {
			is = getResources().openRawResourceFd(
					R.raw.alien_orifice).createInputStream();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
