package br.usp.ime.dspbenchmarking;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.TextView;

public class StatsActivity extends Activity {

	// Views from stats UI
	private ProgressBar cpuUsageBar;
	private ProgressBar dspCycleTimeBar = null;
	private TextView sampleReadTimeView = null;
	private TextView sampleWriteTimeView = null;
	private TextView dspCycleTimeView = null;
	private TextView dspPeriodView = null;
	private TextView dspCyclesView = null;
	private TextView readCyclesView = null;
	private TextView callbackPeriodView = null;
	private TextView elapsedTimeView = null;

	// Threads
	protected SystemWatchThread swt;
	protected DspThread dt;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Find stats views
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
			//Log.e("mHandler", "received msg="+msg);
			if (dt != null) {
				//Log.e("mHandler", "And dt is not null.");
				sampleReadTimeView.setText(String.format("%.6f",
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
				readCyclesView.setText(Long.toString(dt.getReadTicks())); // #
																			// of
																			// read
																			// ticks

				callbackPeriodView.setText(String.format("%.6f",
						dt.getCallbackPeriodMeanTime())); // callback period
															// mean time
				elapsedTimeView.setText(String.format("%.6f",
						dt.getElapsedTime()));

				// Progress Bars
				if (swt != null)
					cpuUsageBar.setProgress(swt.getCpuUsage());
				dspCycleTimeBar
						.setProgress((int) ((dt.getDspCycleMeanTime() / dt
								.getBlockPeriod()) * 100));
			}

		}
	};

}
