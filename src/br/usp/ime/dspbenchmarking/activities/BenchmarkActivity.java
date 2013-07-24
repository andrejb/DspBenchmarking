package br.usp.ime.dspbenchmarking.activities;

import java.io.IOException;
import br.usp.ime.dspbenchmarking.R;
import br.usp.ime.dspbenchmarking.threads.DspThread;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;


/**
 * ATTENTION: This class is currently not used. This is on of the reasons
 * why is is not properly documented.
 * 
 * @author andrejb
 *
 */
public class BenchmarkActivity extends TestActivity {

	// Test config
	private static final int MAX_DSP_CYCLES = 1000;

	// Test limits
	static final int START_BLOCK_SIZE = (int) Math.pow(2,4);
	static final int END_BLOCK_SIZE = (int) Math.pow(2,13);
	//static final int START_ALGORITHM = 0;
	//static final int END_ALGORITHM = 2;
	private DspThread.AlgorithmEnum algorithms_test[] = {
		DspThread.AlgorithmEnum.LOOPBACK,
		DspThread.AlgorithmEnum.REVERB,
		DspThread.AlgorithmEnum.FFT_ALGORITHM
	};
	
	protected final int LOG_START_BLOCK_SIZE = (int) Math.log(START_BLOCK_SIZE);
	protected double totalProgress = ((Math.log(END_BLOCK_SIZE) - LOG_START_BLOCK_SIZE) / LOG2 + 1) * 3;
	private long lastTotalTime = 0;

	/**
	 * - Carrega a tela de testes.
	 * - Define o prefixo do nome do arquivo.
	 * - Define o máximo de ciclos DSP.
	 * - Liga os testes.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set the view
		setContentView(R.layout.tests);
		super.onCreate(savedInstanceState);
		toggleTestsButton.setTextOn("benchmarking algorithms...");
		filePrefix = "/benchmark-";
		maxDspCycles = MAX_DSP_CYCLES;
		//start tests automatically
		toggleTestsButton.setChecked(true);
		toggleTests(null);
	}

	/**
	 * Verifica o estado do dispositivo de armazenamento.
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
	 * Configura os testes:
	 *   - abre um arquivo de áudio de entrada (no super).
	 *   - Inicia uma thread DSP com as seguintes configurações:
	 *     - tamanho de bloco de this.blockSize,
	 *     - algoritmo igual a this.algorithm,
	 *     - stream de entrada igual a this.is,
	 *     - numero máximo de ciclos dsp igual a this.MAX_DSP_CYCLES.
	 */
	protected void setupTests() {
		super.setupTests();
		dt = new DspThread();
		dt.setBlockSize(blockSize);
		dt.setAlgorithm(algorithm);
		dt.setInputStream(is);
		dt.setMaxDspCycles(MAX_DSP_CYCLES);
		dt.setParams(0.5);
	}
	
	/**
	 * 
	 */
	private void releaseTest() {
		// write results
		/*try {
			os.write(getDspThreadInfo(algorithm));
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
		releaseDspThread();*/
	}

	/**
	 * Finaliza os testes realizando as seguintes ações:
	 *   - Escreve o tempo total para a saída.
	 *   - Fecha os stream de entrada.
	 *   - Libera a thread DSP atual.
	 */
	private void finishTests() {
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
		turnOff("Algorithm benchmarking results");
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
		}

		@Override
		public void run() {


			// Get input stream
			try {
				
				// create the DSP thread for tests.
				Message msg = mHandler.obtainMessage();
				Bundle b = new Bundle();
				b.putString("action", "setup-tests");
				msg.setData(b);
				mHandler.sendMessage(msg);
				
				// keep track of time
				long startTime = SystemClock.uptimeMillis();
				

				//for (int i=0; i<3; i++)
				// Iterate through power of 2 blocks
				int algorithm_count = 0;
				for (DspThread.AlgorithmEnum a : algorithms_test) {
					algorithm = a;
					algorithm_count++;
					// Iterate through algorithms					
					for (blockSize = START_BLOCK_SIZE; blockSize <= END_BLOCK_SIZE; blockSize *= 2) {	
						Log.i("DSP TEST", "init algorithm " + algorithm);
						// limit number of DSP cycles
						if (blockSize <= 64)
							maxDspCycles = 5000;
						else if (blockSize <= 512)
							maxDspCycles = 1000;
						else if (blockSize <= 2048)
							maxDspCycles = 500;
						else
							maxDspCycles = 100;
						
						// Send message for starting a new test
						msg = mHandler.obtainMessage();
						b = new Bundle();
						b.putString("action", "launch-test");
						b.putInt("block-size", blockSize);
						b.putInt("algorithm", algorithm.ordinal());
						//b.putInt("max-dsp-cycles", MAX_DSP_CYCLES);
						msg.setData(b);
						mHandler.sendMessage(msg);

						// wait for test to start
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							Log.e("ERROR", "Thread was Interrupted");
						}

						// run tests
						Log.i("DSP TEST", "init block size = " + blockSize);
						// Wait for tests to end
						while (!dt.isSuspended())
							try {
								//Log.w("rodando", "arui");
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								Log.e("ERROR", "Thread was Interrupted");
							}

						// Sends message for starting a new test
						msg = mHandler.obtainMessage();
						b = new Bundle();
						b.putString("action", "release-test");
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
						
						msg = mHandler.obtainMessage();
						b = new Bundle();
						b.putString("action", "write-results");
						b.putLong("total-time", SystemClock.uptimeMillis() - startTime);
						msg.setData(b);
						mHandler.sendMessage(msg);

						// increase status bar
						double actualProgress = ((Math.log(blockSize) - LOG_START_BLOCK_SIZE) / LOG2)  + algorithm_count*totalProgress/3;
						progressBar
						.setProgress((int)((actualProgress / totalProgress) * 100));
					}
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

	/************************************************************************
	 * Recebe uma mensagem e executa uma das seguintes ações:
	 *   - Inicia um teste.
	 *   - Libera um teste.
	 *   - Finaliza os testes.
	 *   - Configura os testes.
	 *   - Escreve os resultados dos testes.
	 ***********************************************************************/
	@SuppressLint("HandlerLeak")
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String action = msg.getData().getString("action");
			if (action.equals("finish-tests"))
				finishTests();
			else if (action.equals("launch-test")) {
				algorithm = DspThread.AlgorithmEnum.values()[msg.getData().getInt("algorithm")];
				blockSize = msg.getData().getInt("block-size");
				updateScreenInfo();
				launchTest();
			} else if (action.equals("release-test")) {
				releaseTest();
			} else if (action.equals("setup-tests"))
				setupTests();
			else if (action.equals("write-results")) {
				totalTime = msg.getData().getLong("total-time");
				writeResults(msg.getData().getInt("filter-size"));
			}
		}
	};
	
	private void writeResults(int maxFiltersize) {
		try {
			String info = (new String(getDspThreadInfo(algorithm))) + " # test time: " + ((float)(totalTime - lastTotalTime)/ 1000) + " s\n";
			lastTotalTime = totalTime;
			os.write(info.getBytes());
			results += info;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	protected void startControlThread() {
		TestControlThread mt = new TestControlThread(mHandler);
		mt.start();
	}
}
