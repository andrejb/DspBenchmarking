package br.usp.ime.dspbenchmarking;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DspActivity extends Activity {

	// Views from stats UI
	private ProgressBar cpuUsageBar;
	private ProgressBar dspCycleTimeBar = null;
	private TextView sampleReadTimeView = null;
	private TextView sampleWriteTimeView = null;
	private TextView dspCallbackTimeView = null;
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
		dspCallbackTimeView = (TextView) findViewById(R.id.meanDspCycleTimeValue);
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
			// set sample read mean time
			if (dt != null)
				sampleReadTimeView.setText(String.format("%.6f",
						dt.getSampleReadMeanTime()));
			// set sample write mean time
			if (dt != null)
				sampleWriteTimeView.setText(String.format("%.6f",
						dt.getSampleWriteMeanTime()));
			// set DSP cycle mean time
			if (dt != null)
				dspCallbackTimeView.setText(String.format("%.6f",
						dt.getDspCallbackMeanTime()));
			// set block period
			if (dt != null)
				dspPeriodView
						.setText(String.format("%.6f", dt.getBlockPeriod()));
			// set callback ticks
			if (dt != null)
				dspCyclesView.setText(Long.toString(dt.getCallbackTicks()));
			// set read ticks
			if (dt != null)
				readCyclesView.setText(Long.toString(dt.getReadTicks()));
			// set callback period mean time
			if (dt != null)
				callbackPeriodView.setText(String.format("%.6f",
						dt.getCallbackPeriodMeanTime()));
			// set elapsed time
			if (dt != null)
				elapsedTimeView.setText(String.format("%.6f",
						dt.getElapsedTime()));
			// set cpu usage
			if (swt != null)
				cpuUsageBar.setProgress(swt.getCpuUsage());
			// set amount of DSP cycle used with processing
			if (dt != null)
				dspCycleTimeBar
					.setProgress((int) ((dt.getDspCallbackMeanTime() / dt
							.getBlockPeriod()) * 100));

		}
	};

}
