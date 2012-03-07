package br.usp.br.dspbenchmarking;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class DspActivity extends Activity {
	
	//private static final String TAG = "DspActivity";

	// Variables from the GUI
	private int blockSize = 64;
	private int dspAlgorithm = 1;
	int maxParamValue = 100;
	private double parameter1 = 1.0;
	private int audioSource = 0; // Microphone is default.

	// Threads
	private SystemWatchThread swt;
	private DspThread dt;

	// Views
	private CheckBox toggleDSPView;
	
	private Spinner dspBlockSizeView = null;
	private Spinner audioSourceView = null;
	private Spinner algorithmView = null;

	private ProgressBar cpuUsageBar;
	private ProgressBar dspCycleTimeBar = null;
	private SeekBar parameter1View = null;

	private TextView sampleReadTimeView = null;
	private TextView sampleWriteTimeView = null;
	private TextView dspCycleTimeView = null;
	private TextView dspPeriodView = null;
	private TextView dspCyclesView = null;
	private TextView readCyclesView = null;
	private TextView callbackPeriodView = null;
	private TextView elapsedTimeView = null;
	

	/************************************************************************
	 * onCreate Calles when the activity is first created.
	 ***********************************************************************/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Set the view
		setContentView(R.layout.dsp);
		
		// Prevent from locking
	    this.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		// Get views
		toggleDSPView = (CheckBox) findViewById(R.id.toggle_dsp);
		cpuUsageBar = (ProgressBar) findViewById(R.id.cpu_usage);
		sampleReadTimeView = (TextView) findViewById(R.id.meanSampleReadTimeValue);
		sampleWriteTimeView = (TextView) findViewById(R.id.meanSampleWriteTimeValue);
		dspCycleTimeView = (TextView) findViewById(R.id.meanDspCycleTimeValue);
		dspCycleTimeBar = (ProgressBar) findViewById(R.id.dspCycleBar);
		dspPeriodView = (TextView) findViewById(R.id.dspPeriodValue);
		dspCyclesView = (TextView) findViewById(R.id.dspCyclesValue);
		readCyclesView = (TextView) findViewById(R.id.readCyclesValue);
		callbackPeriodView = (TextView) findViewById(R.id.callbackPeriodValue);
		elapsedTimeView = (TextView) findViewById(R.id.elapsedTimeValue);
		parameter1View = (SeekBar) findViewById(R.id.param1);

		// Init algorithms list
		algorithmView = (Spinner) findViewById(R.id.algorithm);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.algorithm_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		algorithmView.setAdapter(adapter);
		algorithmView.setOnItemSelectedListener(new AlgorithmListener());
		algorithmView.setSelection(1);
		
		// Init block size list
		dspBlockSizeView = (Spinner) findViewById(R.id.dspBlockSize);
		ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
				this, R.array.block_size_array,
				android.R.layout.simple_spinner_item);
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dspBlockSizeView.setAdapter(adapter2);
		dspBlockSizeView.setOnItemSelectedListener(new BlockSizeListener());
		dspBlockSizeView.setSelection(6);
		
		// Init audio source list
		audioSourceView = (Spinner) findViewById(R.id.audioSource);
		ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(
				this, R.array.source_array,
				android.R.layout.simple_spinner_item);
		adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		audioSourceView.setAdapter(adapter3);
		audioSourceView.setOnItemSelectedListener(new AudioSourceListener());
		audioSourceView.setSelection(1);

		// Input paramteres listeners
		parameter1View.setMax(maxParamValue);
		parameter1View.setProgress(maxParamValue);
		parameter1View.setOnSeekBarChangeListener(parameter1Listener);
		
		// This thread updates the screen with new info every 1 second.
		swt = new SystemWatchThread(mHandler);
		swt.start();

	}

	/************************************************************************
	 * message handler.
	 ***********************************************************************/
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			// Set values in text Views
			if (dt != null) {
				sampleReadTimeView.setText(String.format("%.6f",
						dt.getSampleReadMeanTime())); // read mean time
				sampleWriteTimeView.setText(String.format("%.6f",
						dt.getSampleWriteMeanTime())); // write mean time
				dspCycleTimeView.setText(String.format("%.6f",
						dt.getDspCycleMeanTime())); // DSP cycle mean time
				dspPeriodView
						.setText(String.format("%.6f", dt.getBlockPeriod())); // Block
																				// period
				dspCyclesView.setText(Long.toString(dt.getCallbackTicks())); // # of DSP cycles
				readCyclesView.setText(Long.toString(dt.getReadTicks())); // # of read ticks

				callbackPeriodView.setText(String.format("%.6f",
						dt.getCallbackPeriodMeanTime())); // callback period
															// mean time
				elapsedTimeView.setText(String.format("%.6f", dt.getElapsedTime()));

				// Progress Bars
				if (swt != null)
					cpuUsageBar.setProgress(swt.getCpuUsage());
				dspCycleTimeBar
						.setProgress((int) ((dt.getDspCycleMeanTime() / dt
								.getBlockPeriod()) * 100));
			}

		}
	};

	
	/************************************************************************
	 * This turns FFT processing on and off.
	 ***********************************************************************/
	public void toggleDSP(View v) {
		if (toggleDSPView.isChecked()) {
			// Threads
			if (audioSource == 0)
				dt = new DspThread(blockSize, dspAlgorithm);
			else if (audioSource == 1)
				dt = new DspThread(blockSize, dspAlgorithm, "/sdcard/DspBenchmarking/arpeggia2.wav");
			dt.setParams(parameter1);
			dt.start();
			// mProgressStatus = (int) readUsage() * 100;
		} else {
			try {
				//swt.stopRunning();
				dt.stopRunning();
				//swt = null;
				dt = null;
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
		// Start lengthy operation in a background thread
		// setContentView(R.layout.fft);
		// EditText et = (EditText) findViewById(R.id.texto1);
		// et.setText(mProgress.toString());
	}

	
	/************************************************************************
	 * Listener for algorithm change.
	 ***********************************************************************/
	private class AlgorithmListener implements OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			dspAlgorithm = pos;
			restartDSP();
		}

		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
		}
	}

	
	/************************************************************************
	 * Listens for DSP block size change.
	 ***********************************************************************/
	private class BlockSizeListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {			// Perform action on clicks
			blockSize = (int) Math.pow(2, pos);
			restartDSP();
			// sampleWriteTime = 0;
			// dspCycleTime = 0;
			// ticks = 0;
		}
		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
		}
	};
	
	
	/************************************************************************
	 * Listens for audio source change
	 ***********************************************************************/
	private class AudioSourceListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {			// Perform action on clicks
			audioSource = pos;
			restartDSP();
			// sampleWriteTime = 0;
			// dspCycleTime = 0;
			// ticks = 0;
		}
		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
		}
	};
	
	
	/************************************************************************
	 * Restarts DSP with new parameters.
	 ***********************************************************************/
	private OnSeekBarChangeListener parameter1Listener = new OnSeekBarChangeListener() {
		public void onProgressChanged(SeekBar sb, int i, boolean b) {
			parameter1 = (float) i / maxParamValue;
			// set algorithm parameters
			if (dt != null)
				 dt.setParams(parameter1);
		}
		public void onStopTrackingTouch(SeekBar sb) {
			
		}
		public void onStartTrackingTouch(SeekBar sb) {
	
		}
	};
	
	
	/************************************************************************
	 * Restarts DSP with new parameters.
	 ***********************************************************************/
	private void restartDSP() {
		if (toggleDSPView != null)
			if (toggleDSPView.isChecked()) {
				toggleDSPView.setChecked(false);
				toggleDSP(null);
				toggleDSPView.setChecked(true);
				try {
					Thread.sleep(100);
				} catch (Exception e) {
				}
				toggleDSP(null);
			}
	}

}