package br.usp.ime.dspbenchmarking.activities;

import br.usp.ime.dspbenchmarking.R;
import br.usp.ime.dspbenchmarking.threads.DspThread;
import br.usp.ime.dspbenchmarking.threads.SystemWatchThread;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.TextView;


/**
 * A DSP activity is one that allows to show and control a DSP process. It can
 * be used to either run a "live" process and use the DSP facilities as an
 * example or to run tests and display tests results on the screen.
 * 
 * A DSP thread is launched to control the process of acquiring, modifying and
 * outputting audio signals, with help from the algorithm classes.
 * 
 * This activity also has a companion thread called the SystemWatchThread which
 * takes care of polling the system for CPU usage and pinging this activity so
 * the screen gets updated periodically.
 * 
 * @author andrejb
 *
 */
public abstract class DspActivity extends Activity {

	// Views from stats UI
	protected ProgressBar cpuUsageBar;
	protected ProgressBar dspCycleTimeBar = null;
	protected TextView sampleReadTimeView = null;
	protected TextView sampleWriteTimeView = null;
	protected TextView dspCallbackTimeView = null;
	protected TextView dspPeriodView = null;
	protected TextView dspCyclesView = null;
	protected TextView readCyclesView = null;
	protected TextView callbackPeriodView = null;
	protected TextView elapsedTimeView = null;
	
	// possibly used on subviews
	protected TextView totalTimeView = null;

	// Threads
	protected SystemWatchThread swt;
	protected DspThread dt;
	
	protected long totalTime;

	/**
	 * Configure the screen and launch the system watch thread.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();
	}
	
	private void initialize() {
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
	 * Message handler.
	 ***********************************************************************/
	
	/**
	 * This message handler receives messages from the SystemWatchThread and
	 * updates the screen with up-to-date information about the DSP thread.
	 */
	@SuppressLint("HandlerLeak")
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// set total time
			//if (dt != null && totalTimeView != null)
			//	totalTimeView.setText(String.format("%f",
			//			(new Float(totalTime/1000))));
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
