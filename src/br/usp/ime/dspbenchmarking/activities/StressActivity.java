package br.usp.ime.dspbenchmarking.activities;

import java.io.IOException;

import br.usp.ime.dspbenchmarking.R;
import br.usp.ime.dspbenchmarking.threads.DspThread;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

/**
 * ATTENTION: This class is currently not used. This is on of the reasons
 * why is is not properly documented.
 * 
 * @author andrejb
 *
 */
public class StressActivity extends TestActivity {


	// Test config
	protected static final int MAX_DSP_CYCLES = 500;
	protected static final int NUMBER_OF_TESTS = 1;
	protected int START_BLOCK_SIZE = (int) Math.pow(2,4);
	protected int END_BLOCK_SIZE = (int) Math.pow(2,13);
	protected int START_FILTER_SIZE = 4;
	
	protected final int LOG_START_BLOCK_SIZE = (int) Math.log(START_BLOCK_SIZE);
	protected double totalProgress = (Math.log(END_BLOCK_SIZE) - LOG_START_BLOCK_SIZE) / LOG2 + 1;
	

	// Views
	protected TextView filterSizeView = null;
	protected TextView emeView = null;
	protected TextView emaoView = null;

	// Output file specifications
	String fileName = null;
	final String dateFormat = "yyyy-MM-dd_HH-mm-ss";

	// Test limits
	//static int startAlgorithm = 4;
	//static int endAlgorithm = 4;
	private final DspThread.AlgorithmEnum algorithms_list[] = {
			DspThread.AlgorithmEnum.ADD_SYNTH_SINE
	};

	// local stuff
	private int filterSize;
	private int eme;
	private int emao;
	private long lastTotalTime = 0;

	/**
	 * 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set the view
		setContentView(R.layout.stress);
		super.onCreate(savedInstanceState);
		toggleTestsButton.setTextOn("stressing device...");
		filterSizeView = (TextView) findViewById(R.id.filterSize);
		emeView = (TextView) findViewById(R.id.eme);
		emaoView = (TextView) findViewById(R.id.emao);
		totalTimeView = (TextView) findViewById(R.id.totalTime);
		// File prefix
		filePrefix = "/benchmark-";
		maxDspCycles = MAX_DSP_CYCLES;
		//start tests automatically
		toggleTestsButton.setChecked(true);
		toggleTests(null);
	}



	/**
	 * Generates a byte array with statistics from the DSP thread
	 * 
	 * @return
	 */
	private String getDspThreadInfo(DspThread.AlgorithmEnum algorithm, int maxFiltersize) {
		return super.getDspThreadInfo(algorithm)
				+ String.format("%d ", maxFiltersize);
	}



	/**
	 *
	 */
	protected void updateScreenInfo() {
		super.updateScreenInfo();
		filterSizeView.setText(String.valueOf(filterSize));
		emeView.setText(String.valueOf(eme));
		emaoView.setText(String.valueOf(emao));
		totalTimeView.setText(String.valueOf((float) totalTime / 1000));
	}



	/**
	 * 
	 */
	private void releaseTest() {
		Log.w("StressFlow", "releaseTest");
		// write results
		//dt.suspendDsp();
	}

	private void writeResults(int maxFiltersize) {
		try {
			String info = getDspThreadInfo(algorithm, maxFiltersize) + " # test time: " + ((float)(totalTime - lastTotalTime)/ 1000) + " s\n";
			lastTotalTime = totalTime;
			os.write(info.getBytes());
			results += info;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	private void finishTests() {
		Log.w("StressFlow", "finishTests");
		// Close the output file
		try {
			String timeTotal = new String("# total time: " + ((float)totalTime / 1000) + "\n"); 
			os.write(timeTotal.getBytes());
			os.close();
			results += timeTotal;
		} catch (IOException e) {
			e.printStackTrace();
		}
		// close the input stream
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		is = null;
		releaseDspThread();
		turnOff("Stress results");
	}




	/**
	 * 
	 * @author andre
	 * 
	 */
	protected class TestControlThread extends Thread {

		private Handler mHandler;

		public TestControlThread(Handler handler) {
			mHandler = handler;
			algorithm = DspThread.AlgorithmEnum.ADD_SYNTH_SINE;
		}

		@Override
		public void run() {

			try {
				blockSize = START_BLOCK_SIZE;
				algorithm = DspThread.AlgorithmEnum.ADD_SYNTH_SINE;
				
				// create the DSP thread for tests.
				Message msg = mHandler.obtainMessage();
				Bundle b = new Bundle();
				b.putString("action", "setup-tests");
				msg.setData(b);
				mHandler.sendMessage(msg);
				
				// Keeps track of time
				long startTime = SystemClock.uptimeMillis();

				// repeat tests N times
				for (int i = 0; i < NUMBER_OF_TESTS; i++)
					// iterate through block sizes
					for (blockSize = START_BLOCK_SIZE; blockSize <= END_BLOCK_SIZE; blockSize *= 2) {

						int filterSize = START_FILTER_SIZE;
						int m = filterSize; // 2 ** 5
						int M = (int) Math.pow(2,14); 			// 2 ** 14
						boolean reachedPeak = false;

						// invariant: the device performs well on filters with m coefficients
						// and bad with filters with M coefficients
						while(m<M-1) {
							Log.i("StressThread", "Starting new stress-test.");
							Log.i("StressThread", "m="+m);
							Log.i("StressThread", "filterSize="+filterSize);
							Log.i("StressThread", "M="+M);
							Log.i("StressThread", "blockSize="+blockSize);
							
							// for screen preview
							eme = m;
							emao = M;
							
							//===============================================
							// calc new filter size
							//===============================================
							if (!reachedPeak)
								filterSize *= 2;
							else
									filterSize = (int) (m + ((double) (M-m) / 2));
							
							// calc max dsp cycles
							if (blockSize <= 64)
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
							}


							//===============================================
							// run test
							//===============================================
							// launch test
							msg = mHandler.obtainMessage();
							b = new Bundle();
							b.putString("action", "launch-test");
							b.putInt("block-size", blockSize);
							b.putInt("algorithm", 4);
							b.putInt("filter-size", filterSize);
							b.putInt("max-dsp-cycles", maxDspCycles);
							msg.setData(b);
							mHandler.sendMessage(msg);

							// wait for test to start
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								Log.e("ERROR", "Thread was Interrupted");
							}

							// wait for test to end
							while (!dt.isSuspended()) {
								// update clock
								msg = mHandler.obtainMessage();
								b = new Bundle();
								b.putString("action", "update-clock");
								b.putLong("total-time", SystemClock.uptimeMillis() - startTime);
								msg.setData(b);
								mHandler.sendMessage(msg);
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
							Log.i("StressThread", "performMeanTime="+dt.getDspPerformMeanTime());
							Log.i("StressThread", "callbackMeanTime="+dt.getDspCallbackMeanTime());
							Log.i("StressThread", "blockPeriod="+dt.getBlockPeriod());
							Log.i("StressThread", "blockSize="+blockSize);
							if (dt.getDspCallbackMeanTime() < dt.getBlockPeriod()) {
								m = filterSize;
								Log.i("***INFO***", "com filterSize="+m+" rola!");
							}
							else {
								reachedPeak = true;
								M = filterSize;
								Log.i("***INFO***", "com filterSize="+M+" ***NAO*** rola!");
							}


							//===============================================
							// clean trash before running next test
							//===============================================
							// release test
							msg = mHandler.obtainMessage();
							b = new Bundle();
							b.putString("action", "release-test");
							b.putLong("total-time", SystemClock.uptimeMillis() - startTime);
							msg.setData(b);
							mHandler.sendMessage(msg);
							try {
								Thread.sleep(2000);
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
						}
						
						// write results
						msg = mHandler.obtainMessage();
						b = new Bundle();
						b.putString("action", "write-results");
						b.putInt("filter-size", m);
						b.putLong("total-time", SystemClock.uptimeMillis() - startTime);
						msg.setData(b);
						mHandler.sendMessage(msg);

						// increase status bar
						double actualProgress = (Math.log(blockSize) - LOG_START_BLOCK_SIZE) / LOG2;
						Log.e("aqui", "actual="+actualProgress);
						progressBar
						.setProgress((int) ((actualProgress / totalProgress) * 100));
					}

			} catch (Exception e) {
				e.printStackTrace();
			}

			// Turn off when done.
			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putString("action", "finish-tests");
			msg.setData(b);
			mHandler.sendMessage(msg);
		}



	}
	
	protected void setupTests() {
		super.setupTests();
		dt = new DspThread();
		dt.setBlockSize(blockSize);
		dt.setAlgorithm(algorithm);
		dt.setInputStream(is);
		dt.setMaxDspCycles(MAX_DSP_CYCLES);
		dt.setStressParameter(filterSize);
		dt.setParams(0.5);
	}

	/**
	 * 
	 * @param bSize
	 * @param alg
	 */
	protected void launchTest() {
		Log.w("StressFlow", "launchTest");
		// set test-specific variables
		dt.setBlockSize(blockSize);
		dt.setStressParameter(filterSize);
		// launch
		super.launchTest();
	}

	/************************************************************************
	 * message handler.
	 ***********************************************************************/
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String action = msg.getData().getString("action");
			if (action.equals("finish-tests"))
				finishTests();
			else if (action.equals("setup-tests"))
				setupTests();
			else if (action.equals("update-clock")) {
				totalTime = msg.getData().getLong("total-time");
				updateScreenInfo();
			}
			else if (action.equals("launch-test")) {
				algorithm = DspThread.AlgorithmEnum.values()[msg.getData().getInt("algorithm")];
				blockSize = msg.getData().getInt("block-size");
				filterSize = msg.getData().getInt("filter-size");
				maxDspCycles = msg.getData().getInt("max-dsp-cycles");
				updateScreenInfo();
				Log.i("launchTest", "launching with filterSize="+filterSize);
				launchTest();
			} else if (action.equals("release-test")) {
				totalTime = msg.getData().getLong("total-time");
				releaseTest();
			} else if (action.equals("write-results")) {
				totalTime = msg.getData().getLong("total-time");
				writeResults(msg.getData().getInt("filter-size"));
			}
		}
	};
	
	protected void startControlThread() {
		TestControlThread mt = new TestControlThread(mHandler);
		mt.start();
	}
}
