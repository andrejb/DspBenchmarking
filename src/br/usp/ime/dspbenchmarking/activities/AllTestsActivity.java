package br.usp.ime.dspbenchmarking.activities;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import br.usp.ime.dspbenchmarking.R;
import br.usp.ime.dspbenchmarking.threads.DspThread;

/**
 * This class performs all tests in a device. Tests are divided in 2 phases:
 * 
 * 	 Phase 1: measurement of time taken to perform common tasks (loopback,
 *            FFT, IIR filtering).
 *
 *   Phase 2: stress tests (convolution, additive synthesis). During this
 *            phase, each algorithm is run with increasing complexity until
 *            the device limit is reached (i.e. until the computation period
 *            becomes bigger than the theoretical DSP period). Then, a binary
 *            search is performed to find the maximum parameter feasible for
 *            that algorithm.
 *
 *   All tests use one DSP thread, which is subsequently suspended and resumed
 *   between tests execution. One DSP control thread is started that keeps
 *   watching the DSP thread and controls tests execution and results
 *   gathering.
 * 
 * @author andrejb
 *
 */
public class AllTestsActivity extends Activity {

	// Views
	protected ToggleButton toggleTestsButton = null;
	protected ProgressBar workingBar = null;
	protected ProgressBar progressBar = null;
	protected TextView algorithmName = null;
	protected TextView blockSizeView = null;

	// Variables for setting views
	protected final double LOG2 = Math.log(2);
	protected final int LOG_START_BLOCK_SIZE = (int) Math.log(START_BLOCK_SIZE);
	protected double totalProgress = ((Math.log(END_BLOCK_SIZE) - LOG_START_BLOCK_SIZE) / LOG2 + 1) * 7;

	// Test results are stored in this variable
	String results = getResultsHeader();

	// Variables for tests
	private java.io.InputStream inputStream;
	int blockSize;
	int algorithm;
	protected int maxDspCycles;
	protected int stressParameter;  // in case the test is a stress test.
	private static final int MAX_DSP_CYCLES = 1000;

	// Block size limits
	static final int START_BLOCK_SIZE = (int) Math.pow(2,4);
	static final int END_BLOCK_SIZE = (int) Math.pow(2,13);

	// Time keeping
	private long lastTotalTime = 0;

	// Message constants
	private final String MESSAGE_ACTION = "action";
	private final String MESSAGE_LAUNCH_TEST = "launch-test";
	private final String MESSAGE_RELEASE_TEST = "release-test";
	private final String MESSAGE_FINISH_TESTS = "finish-tests";
	private final String MESSAGE_STORE_RESULTS = "store-results";
	private final String MESSAGE_ALGORITHM = "algorithm";
	private final String MESSAGE_BLOCK_SIZE  = "block-size";
	private final String MESSAGE_MAX_DSP_CYCLES = "max-dsp-cycles";
	private final String MESSAGE_TOTAL_TIME = "total-time";
	private final String MESSAGE_STRESS_PARAM = "stress-param";

	// Message handler lock
	private TestControlThread mt;
	static class Lock extends Object {
	}
	static public Lock messageHandlerLock = new Lock();

	public class Barrier
	{
		public synchronized void block() throws InterruptedException
		{
			Log.i("BARRIER", "Blocking...");
			wait();
			Log.i("BARRIER", "Released.");
		}

		public synchronized void release() throws InterruptedException
		{
			Log.i("BARRIER", "Notifying.");
			notify();
		}

		public synchronized void releaseAll() throws InterruptedException
		{
			Log.i("BARRIER", "Notifying all.");
			notifyAll();
		}

	}

	protected DspThread dt;
	protected long totalTime;

	/*************************************************************************
	 * Constructor
	 ************************************************************************/

	/**
	 * Executes upon creation of activity:
	 *   - configure screen.
	 *   - configure tests.
	 *   - start the test control thread.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set the view
		setContentView(R.layout.tests);
		super.onCreate(savedInstanceState);

		// Hide everything we don't need
		/*cpuUsageBar.setVisibility(View.GONE);
		sampleReadTimeView.setVisibility(View.GONE);
		sampleWriteTimeView.setVisibility(View.GONE);
		dspCallbackTimeView.setVisibility(View.GONE);
		dspCycleTimeBar.setVisibility(View.GONE);
		dspPeriodView.setVisibility(View.GONE);
		dspCyclesView.setVisibility(View.GONE);
		readCyclesView.setVisibility(View.GONE);
		callbackPeriodView.setVisibility(View.GONE);
		elapsedTimeView.setVisibility(View.GONE);*/

		// Find toggle button
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

		// Configure screen
		toggleTestsButton.setTextOn("Executando testes...");
		toggleTestsButton.setChecked(true);
		toggleTestsButton.setClickable(false);
		workingBar.setVisibility(ProgressBar.VISIBLE);

		// Start tests
		Log.i("DSP TESTS", "Starting control thread...");
		setupTests();
		startControlThread();
	}

	/*************************************************************************
	 * Control methods
	 ************************************************************************/

	/**
	 * Sets up everything needed to start tests:
	 *   - open an input stream.
	 *   - configure the DSP algorithm (block size, audio source, input strea,
	 *     max dsp cycles, etc).
	 *   - start the DSP thread. 
	 */
	protected void setupTests() {
		Log.i("DSP TESTS", "Setting up the DSP thread...");
		dt = new DspThread();
		try {
			inputStream = getResources().openRawResourceFd(
					R.raw.alien_orifice).createInputStream();
			dt.setInputStream(inputStream);
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		dt.setBlockSize(blockSize);
		dt.setAlgorithm(algorithm);
		dt.setAudioSource(DspThread.AUDIO_SOURCE_WAV);
		dt.setInputStream(inputStream);
		dt.setMaxDspCycles(MAX_DSP_CYCLES);
		dt.setParams(0.5);
		dt.start();
	}

	/**
	 * Update screen with test information.
	 */
	private int lastAlg = -1;
	protected void updateScreenInfo() {
		if (algorithm != lastAlg) {
			if (algorithm == 0)
				algorithmName.setText("Loopback  ");
			else if (algorithm == 1)
				algorithmName.setText("Reverb  ");
			else if (algorithm == 2)
				algorithmName.setText("FFT  ");
			else if (algorithm == 3)
				algorithmName.setText("Convolution  ");
			else if (algorithm == 4)
				algorithmName.setText("Additive Synthesis (sine)  ");
			else if (algorithm == 5)
				algorithmName.setText("Additive Synthesis (linear)  ");
			else if (algorithm == 6)
				algorithmName.setText("Additive Synthesis (cubic)  ");
			lastAlg = algorithm;
		}
		blockSizeView.setText(String.valueOf(blockSize));
	}

	/**
	 * Launch a new test by configuring and resuming the DSP thread.
	 */
	protected void launchTest() {
		dt.setBlockSize(blockSize);
		dt.setAlgorithm(algorithm);
		dt.setMaxDspCycles(maxDspCycles);
		dt.setStressParameter(stressParameter);
		dt.resumeDsp();
	}

	/**
	 * Releases a test by suspending the DSP thread.
	 */
	protected void releaseTest() {
		dt.suspendDsp();
	}

	/**
	 * Finish tests.
	 */
	private void finishTests() {
		// get total time
		String timeTotal = new String("# total time: " + ((float)totalTime / 1000) + "\n");
		results += timeTotal;
		// close the input stream
		try {
			inputStream.close();
			inputStream = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Stop test control thread.
		mt.stopControlThread();
		// Suspend DSP
		dt.suspendDsp();

		// wait for eventual DSP thread pollings to occur before releasing the DSP thread.
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			Log.e("ERROR", "Thread was Interrupted");
		}
		releaseDspThread();
		
		// Reconfigure screen
		workingBar.setVisibility(ProgressBar.INVISIBLE);
		toggleTestsButton.setTextOff("Testes finalizados.");
		toggleTestsButton.toggle();
		//Intent i = new Intent();
		//i.putExtra("results", results);
	}

	/**
	 * Send tests results to email.
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
	 * Stops and releases the DSP thread.
	 */
	protected void releaseDspThread() {
		Log.i("DSP TESTS", "Releasing DSP thread...");
		dt.stopDspThread();
		dt = null;
	}

	/*************************************************************************
	 * Test control thread
	 ************************************************************************/

	/**
	 * Start the test control thread.
	 */
	protected void startControlThread() {
		mt = new TestControlThread(mHandler);
		mt.start();
	}

	/**
	 * Handle message from main thread. This handler receives messages from
	 * the TestControlThread and control test launch and screen update.
	 */
	@SuppressLint("HandlerLeak")
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String action = msg.getData().getString(MESSAGE_ACTION);

			Log.i("MSG HANDLER", "Received control message, will now act...");
			synchronized (messageHandlerLock) {
				Log.i("MSG HANDLER", "Acquired lock.");
				// launch a new test
				if (action.equals(MESSAGE_LAUNCH_TEST)) {
					Log.i("MSG HANDLER", "Launching new test.");
					algorithm = msg.getData().getInt(MESSAGE_ALGORITHM);
					blockSize = msg.getData().getInt(MESSAGE_BLOCK_SIZE);
					maxDspCycles = msg.getData().getInt(MESSAGE_MAX_DSP_CYCLES);
					stressParameter = msg.getData().getInt(MESSAGE_STRESS_PARAM);
					updateScreenInfo();
					launchTest();
					Log.i("MSG HANDLER", "Finished launching new test.");
				}

				// release a test
				else if (action.equals(MESSAGE_RELEASE_TEST)) {
					Log.i("MSG HANDLER", "Releasing test.");
					releaseTest();
					Log.i("MSG HANDLER", "Finished releasing test.");
				}

				// finish all tests
				else if (action.equals(MESSAGE_FINISH_TESTS)) {
					Log.i("MSG HANDLER", "Finishing tests.");
					finishTests();
					sendResults("Test results");
					Log.i("MSG HANDLER", "Finished finishing tests.");
				}

				// store results
				else if (action.equals(MESSAGE_STORE_RESULTS)) {
					Log.i("MSG HANDLER", "Storing results.");
					storeResults(msg.getData().getInt(MESSAGE_STRESS_PARAM));
					Log.i("MSG HANDLER", "Finishing storing results.");
				}
				// Release the lock.
				Log.i("MSG HANDLER", "Releasing lock.");
				Message reply = mt.cHandler.obtainMessage();
				mt.cHandler.sendMessage(reply);

			}
		}
	};



	/*************************************************************************
	 * Test control thread
	 ************************************************************************/

	/**
	 * Store results from a test.
	 * 
	 * @param maxFiltersize
	 */
	private void storeResults(int stressParam) {
		String info = (new String(getDspThreadInfo(stressParam)));
		info += " # test time: " + ((float)(totalTime - lastTotalTime)/ 1000) + " s\n";
		lastTotalTime = totalTime;
		results += info;
	}

	/**
	 * This is the thread that actually runs and controls tests. It
	 * communicates with the main thread by means of messages to control
	 * the DSP thread configuration, suspension and resuming.
	 */
	protected class TestControlThread extends Thread {

		private boolean controlThreadRunning = false;

		private Handler mHandler;

		private boolean messageHandlerTaskDone;

		/**
		 * Initialize the thread by storing the message handler.
		 * @param handler
		 */
		public TestControlThread(Handler handler) {
			mHandler = handler;
		}

		/**
		 * Set a variable to stop the control thread.
		 */
		public void stopControlThread() {
			controlThreadRunning = false;
		}

		/**
		 * Handles a message to set the message handler task as done.
		 */
		@SuppressLint("HandlerLeak")
		final Handler cHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				Log.i("TEST CONTROL", "Message was handled.");
				messageHandlerTaskDone = true;
			}
		};

		/**
		 * Send a control message to main thread using the message handler.
		 * The action string is mandatory, and all other fields will just be
		 * set in the message if their value is bigger or equal to 0.
		 * 
		 * @param action
		 * @param blockSize
		 * @param algorithm
		 * @param maxDspCycles
		 */
		private void sendMessage(String action, int bSize, int alg,
				int mdCycles, long totalTime, int stressParam) {
			String logString = "  action: "+action+"\n";
			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putString(MESSAGE_ACTION, action);
			if (bSize >= 0) {
				b.putInt(MESSAGE_BLOCK_SIZE, bSize);
				logString += "  block size: "+bSize+"\n";
			}
			if (alg >= 0) {
				b.putInt(MESSAGE_ALGORITHM, alg);
				logString += "  algorithm: "+alg+"\n";
			}
			if (mdCycles>= 0) {
				b.putInt(MESSAGE_MAX_DSP_CYCLES, mdCycles);
				logString += "  max cycles: "+mdCycles+"\n";
			}
			if (totalTime >= 0) {
				b.putLong(MESSAGE_TOTAL_TIME, totalTime);
				logString += "  total time: "+totalTime+"\n";
			}
			if (stressParam >= 0) {
				b.putInt(MESSAGE_STRESS_PARAM, stressParam);
				logString += "  stress param: "+stressParam+"\n";
			}
			msg.setData(b);

			// Send the message and wait for it to be handled.
			Log.i("TEST CONTROL", "Sending control message: \n"+logString);
			messageHandlerTaskDone = false;
			mHandler.sendMessage(msg);
			while (!messageHandlerTaskDone)
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Log.e("ERROR", "Thread was Interrupted");
				}

			// Block until the message has been handled.
			Log.i("TEST CONTROL", "Message was handled.");
		}

		/**
		 * Run all tests, phase 1 and phase 2 as described above in the class
		 * definition. The integers that represent algorithms are given by
		 * the order of the algorithms definition in 'res/values/strings.xml'
		 * (yeah, that is awful and if you have the time to improve it please
		 * do a pull request in the repo :P ). 
		 */
		@Override
		public void run() {

			// Mark thread as running.
			controlThreadRunning = true;

			// keep track of time
			long startTime = SystemClock.uptimeMillis();

			/*************************************************************
			 * Phase 1: Loopback, Reverb and FFT algorithms.
			 ************************************************************/
			int startAlgorithm = 0; // Loopback
			int endAlgorithm = 2; // FFT

			// Iterate through algorithms
			for (algorithm = startAlgorithm; algorithm <= endAlgorithm; algorithm++) {
				Log.i("TESTS PHASE 1", "Starting test with algorithm "+algorithm+".");
				// Iterate through power of 2 blocks
				for (blockSize = START_BLOCK_SIZE; blockSize <= END_BLOCK_SIZE; blockSize *= 2) {	
					Log.i("TESTS PHASE 1", "Starting test with block size " + blockSize+".");

					// limit number of DSP cycles, depending on the block
					// size. This is so that tests do not run for a
					// extremely large period of time.
					/*if (blockSize <= 64)
						maxDspCycles = 5000;
					else if (blockSize <= 512)
						maxDspCycles = 1000;
					else if (blockSize <= 2048)
						maxDspCycles = 500;
					else
						maxDspCycles = 100;*/

					maxDspCycles = 100;

					// Send message for starting a new test
					if (controlThreadRunning)
						sendMessage(MESSAGE_LAUNCH_TEST, blockSize, algorithm, maxDspCycles, -1, -1);

					// wait for test to start
					//try {
					//	Log.i("TESTS PHASE 1", "Waiting for test to start.");
					//	Thread.sleep(2000);
					//} catch (InterruptedException e) {
					//	Log.e("ERROR", "Thread was Interrupted");
					//}

					Log.i("TESTS PHASE 1", "Wait for test to end...");
					// Wait for tests to end
					while (controlThreadRunning && !dt.isSuspended())
						try {
							Log.i("TESTS PHASE 1", "  stil waiting...");
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Log.e("ERROR", "Thread was Interrupted");
						}
					Log.i("TESTS PHASE 1", "Test ended.");

					// Sends message for releasing the test
					if (controlThreadRunning)
						sendMessage(MESSAGE_RELEASE_TEST, -1, -1, -1, -1, -1);

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Log.e("ERROR", "Thread was Interrupted");
					}
					//System.gc();
					// wait for garbage collector
					//try {
					//	Thread.sleep(5000);
					//} catch (InterruptedException e) {
					//	Log.e("ERROR", "Thread was Interrupted");
					//}

					if (controlThreadRunning)
						sendMessage(MESSAGE_STORE_RESULTS, -1, algorithm, -1,
								SystemClock.uptimeMillis() - startTime, -1);

					// increase status bar
					double actualProgress = (((Math.log(blockSize) - LOG_START_BLOCK_SIZE) / LOG2)  + algorithm*totalProgress/7);
					progressBar
					.setProgress((int)((actualProgress / totalProgress) * 100));
					
					// break if thread was interrupted.
					if (!controlThreadRunning)
						break;
				}
				
				// break if thread was interrupted.
				if (!controlThreadRunning)
					break;
			}


			/*************************************************************
			 * Phase 2: Convolution and additive synthesis.
			 ************************************************************/

			startAlgorithm = 3;	// Convolution
			endAlgorithm = 7;	// Additive Synthesis (truncated)

			for (algorithm = startAlgorithm; algorithm <= endAlgorithm; algorithm++) {
				Log.i("TESTS PHASE 2", "Starting with algorithm "+algorithm+".");
				for (blockSize = START_BLOCK_SIZE; blockSize <= END_BLOCK_SIZE; blockSize *= 2) {
					Log.i("TESTS PHASE 2", "Starting with block size "+blockSize+".");

					int stressParam = 1;
					int m = stressParam;			// 2 ** 0
					int M = (int) Math.pow(2,14); 	// 2 ** 14
					boolean reachedPeak = false;	// True if last execution was infeasible

					// invariant: the device performs well on filters with m coefficients
					// and bad with filters with M coefficients
					while(m<M-1) {
						Log.i("TESTS PHASE 2", "Starting new stress-test with m="+m+", M="+M+".");

						// for screen preview
						//eme = m;
						//emao = M;
						// TODO: reactivate view.

						//===============================================
						// calc new filter size
						//===============================================
						if (!reachedPeak)
							stressParam *= 2;
						else
							stressParam = (int) (m + ((double) (M-m) / 2));

						// calc max dsp cycles
						/*if (blockSize <= 64)
							maxDspCycles = 5000;
						else if (blockSize <= 512) {
							int max_size = 1000;
							if (M-m <= 8)
								maxDspCycles = max_size;
							else if (M-m <= 32)
								maxDspCycles = max_size;
							else
								maxDspCycles = max_size;
						}
						else if (blockSize <= 2048) {
							int max_size = 500;
							if (M-m <= 8)
								maxDspCycles = max_size;
							else if (M-m <= 32)
								maxDspCycles = max_size/2;
							else
								maxDspCycles = max_size/5;
						}
						else {
							int max_size = 100;
							if (M-m <= 8)
								maxDspCycles = max_size;
							else if (M-m <= 32)
								maxDspCycles = max_size/2;
							else
								maxDspCycles = max_size/2;
						}*/
						maxDspCycles = 100;
						//if (blockSize >= 2048)
						//	maxDspCycles = 50;


						//===============================================
						// run test
						//===============================================
						// launch test
						if (controlThreadRunning)
							sendMessage(MESSAGE_LAUNCH_TEST, blockSize, algorithm, maxDspCycles, -1, stressParam);

						// wait for test to start
						//try {
						//	Thread.sleep(2000);
						//} catch (InterruptedException e) {
						//	Log.e("ERROR", "Thread was Interrupted");
						//}

						// wait for test to end
						while (controlThreadRunning && !dt.isSuspended()) {
							if (dt.getDspCallbackMeanTime() > dt.getBlockPeriod() && dt.getCallbackTicks() > 100) {
								dt.suspendDsp();
							}
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								Log.e("ERROR", "Thread was Interrupted");
							}
						}

						//===============================================
						// get performance results
						//===============================================
						if (controlThreadRunning && dt.getDspCallbackMeanTime() < dt.getBlockPeriod()) {
							m = stressParam;
							Log.i("TESTS PHASE 2", "Stress parameter "+m+" is feasible!");
						}
						else {
							reachedPeak = true;
							M = stressParam;
							Log.i("TESTS PHASE 2", "Stress parameter "+M+" is *not* feasible!");
						}


						//===============================================
						// clean trash before running next test
						//===============================================
						// release test
						if (controlThreadRunning)
							sendMessage(MESSAGE_RELEASE_TEST, -1, -1, -1, SystemClock.uptimeMillis() - startTime, -1);
						//try {
						//	Thread.sleep(2000);
						//} catch (InterruptedException e) {
						//	Log.e("ERROR", "Thread was Interrupted");
						//}
						//System.gc();
						// wait for garbage collector
						//try {
						//	Thread.sleep(5000);
						//} catch (InterruptedException e) {
						//	Log.e("ERROR", "Thread was Interrupted");
						//}
						
						// break if thread was interrupted.
						if (!controlThreadRunning)
							break;
					}

					// store results
					if (controlThreadRunning)
						sendMessage(MESSAGE_STORE_RESULTS, -1, -1, -1, SystemClock.uptimeMillis() - startTime, m);

					double actualProgress = (((Math.log(blockSize) - LOG_START_BLOCK_SIZE) / LOG2)  + algorithm*totalProgress/7);
					progressBar.setProgress(((int)((actualProgress / totalProgress) * 100)));
					
					// break if thread was interrupted.
					if (!controlThreadRunning)
						break;
				}

				// break if thread was interrupted.
				if (!controlThreadRunning)
					break;
			}

			// Set progress bar to 100%
			progressBar.setProgress(100);

			// Turn off when done.
			if (controlThreadRunning)
				sendMessage(MESSAGE_FINISH_TESTS, -1, -1, -1, -1, -1);
		}
	}


	/*************************************************************************
	 * Test results information acquiring
	 ************************************************************************/

	/**
	 * @return Formatted email header containing Android build info.
	 */
	private String getResultsHeader() {
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
		sbuf.append("# bsize time  cbt   readt sampread sampwrit blockper cbperiod perftime calltime stress\n");
		return sbuf.toString();
	}

	/**
	 * Generates a byte array with statistics from the DSP thread
	 * 
	 * @return
	 */
	protected String getDspThreadInfo(int stressParam) {
		String output = dt.getAlgorithm() + " "; 							// 1  - alg
		output += String.format("%5d ", dt.getBlockSize()); 				// 2  - bsize
		output += String.format("%5.0f ", dt.getElapsedTime()); 			// 3  - time
		output += String.format("%5d ", dt.getCallbackTicks());				// 4  - cbt
		output += String.format("%5d ", dt.getReadTicks());					// 5  - read tics
		output += String.format("%8.4f ", dt.getSampleReadMeanTime());		// 6  - sample read
		output += String.format("%8.4f ", dt.getSampleWriteMeanTime());		// 7  - sample write
		output += String.format("%8.4f ", dt.getBlockPeriod());				// 8  - block period (calculated)
		output += String.format("%8.4f ", dt.getCallbackPeriodMeanTime());	// 9  - callback period
		output += String.format("%8.4f ", dt.getDspPerformMeanTime());		// 10 - perform time
		output += String.format("%8.4f ", dt.getDspCallbackMeanTime());		// 11 - callback time
		output += String.format("%6d ", stressParam);						// 12 - stress param
		return output;
	}

	/*************************************************************************
	 * Popup confirmation dialog if user try to cancel tests
	 ************************************************************************/

	/**
	 * Do that ---^
	 */
	@Override
	public void onBackPressed() {
		if (toggleTestsButton.isChecked()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Tem certeza que deseja interromper os testes?")
			.setCancelable(false)
			.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					finishTests();
					AllTestsActivity.this.finish();
				}
			})
			.setNegativeButton("Não", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
		} else
			AllTestsActivity.this.finish();
	}

}