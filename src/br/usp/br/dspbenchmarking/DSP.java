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
import android.widget.Spinner;
import android.widget.TextView;

public class DSP extends Activity {

	// Variables from the GUI
	private int blockSize = 64;
	private int algorithm = 1;

	// Threads
	private SystemWatchThread swt;
	private DSPThread dt;

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
	private TextView callbackPeriodView = null;
	private Spinner algorithmView = null;

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
		callbackPeriodView = (TextView) findViewById(R.id.callbackPeriodValue);

		// Init algorithms list
		/*algorithmView = (Spinner) findViewById(R.id.algorithm);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.algorithm_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		algorithmView.setAdapter(adapter);
		algorithmView.setOnItemSelectedListener(new AlgorithmListener());*/

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
				sampleRetrieveTimeView.setText(String.format("%.6f",
						dt.getSampleReadMeanTime())); // read mean time
				sampleWriteTimeView.setText(String.format("%.6f",
						dt.getSampleWriteMeanTime())); // write mean time
				dspCycleTimeView.setText(String.format("%.6f",
						dt.getDspCycleMeanTime())); // DSP cycle mean time
				dspPeriodView
						.setText(String.format("%.6f", dt.getBlockPeriod())); // Block
																				// period
				dspCyclesView.setText(Long.toString(dt.getCallbackTicks())); // #
																				// of
																				// DSP
																				// cycles
				callbackPeriodView.setText(String.format("%.6f",
						dt.getCallbackPeriodMeanTime())); // callback period
															// mean time

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
		c = (CheckBox) findViewById(R.id.toggle_dsp);
		cpuUsageBar = (ProgressBar) findViewById(R.id.cpu_usage);

		if (c.isChecked()) {
			// Threads
			swt = new SystemWatchThread(mHandler);
			dt = new DSPThread(blockSize);
			swt.start();
			dt.start();
			// mProgressStatus = (int) readUsage() * 100;
		} else {
			try {
				swt.stopRunning();
				dt.stopRunning();
				swt = null;
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
	 * Listens for algorithm change.
	 ***********************************************************************/
	public class AlgorithmListener implements OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			algorithm = pos;
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
	private void restartDSP() {
		if (c != null)
			if (c.isChecked()) {
				toggleDSP(null);
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
				toggleDSP(null);
			}
	}

}