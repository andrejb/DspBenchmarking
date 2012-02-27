package br.usp.br.dspbenchmarking;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class DSP extends Activity {

	// Variables from the GUI
	private int blockSize = 64;
	private int dspAlgorithm = 1;
	int maxParamValue = 100;
	private double parameter1 = 1.0;

	// Threads
	private SystemWatchThread swt;
	private DSPThread dt;

	// Views
	private CheckBox toggleDSPView;
	private ProgressBar cpuUsageBar;
	private TextView dspBlockSizeView = null;
	private TextView sampleReadTimeView = null;
	private TextView sampleWriteTimeView = null;
	private TextView dspCycleTimeView = null;
	private ProgressBar dspCycleTimeBar = null;
	private TextView dspPeriodView = null;
	private TextView dspCyclesView = null;
	private TextView readCyclesView = null;
	private TextView callbackPeriodView = null;
	private Spinner algorithmView = null;
	private TextView elapsedTimeView = null;
	private SeekBar parameter1View = null;
	

	/************************************************************************
	 * onCreate Calles when the activity is first created.
	 ***********************************************************************/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dsp);
		toggleDSPView = (CheckBox) findViewById(R.id.toggle_dsp);
		cpuUsageBar = (ProgressBar) findViewById(R.id.cpu_usage);
		dspBlockSizeView = (TextView) findViewById(R.id.dspBlockSizeValue);
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
				dspBlockSizeView.setText(Long.toString(dt.getBlockSize())); // block
																			// size
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
			dt = new DSPThread(blockSize, dspAlgorithm);
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
	public class AlgorithmListener implements OnItemSelectedListener {

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
	 * Listens for DSP toggle clicks.
	 ***********************************************************************/
	private OnClickListener dspRadioListener = new OnClickListener() {
		public void onClick(View v) {
			// Perform action on clicks
			RadioButton rb = (RadioButton) v;
			blockSize = Integer.parseInt(rb.getText().toString());
			restartDSP();
			// sampleWriteTime = 0;
			// dspCycleTime = 0;
			// ticks = 0;
		}
	};
	
	
	/************************************************************************
	 * Restarts DSP with new parameters.
	 ***********************************************************************/
	private OnSeekBarChangeListener parameter1Listener = new OnSeekBarChangeListener() {
		public void onProgressChanged(SeekBar sb, int i, boolean b) {
			parameter1 = (float) i / maxParamValue;
			// set algorithm parameters
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