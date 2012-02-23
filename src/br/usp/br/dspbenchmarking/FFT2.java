package br.usp.br.dspbenchmarking;

import java.util.Calendar;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;

public class FFT2 extends Activity {

	private ProgressBar mProgress;
	private int mProgressStatus = 30;
	CheckBox c = (CheckBox) findViewById(R.id.toggle_dsp);


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		c.setOnClickListener(toggleFFT);

		mProgress = (ProgressBar) findViewById(R.id.cpu_usage);
		// Start lengthy operation in a background thread
		mProgressStatus = Calendar.getInstance().get(Calendar.SECOND);
		mProgressStatus = 50;
		mProgress.setProgress(mProgressStatus);
		// Start lengthy operation in a background thread
		new ProgressThread(mHandler).start();
		setContentView(R.layout.dsp);
		EditText et = (EditText) findViewById(R.id.texto1);
        et.setText(mProgress.toString());

	}
	// Create an anonymous implementation of OnClickListener
	private OnClickListener toggleFFT = new OnClickListener() {
		public void onClick(View v) {
			//mProgress.setProgress(30);
			//c.setChecked(false);
			//findViewById(R.layout.fft).invalidate();

		}
	};

	private class ProgressThread extends Thread {	

		// Class constants defining state of the thread
		final static int DONE = 0;
		final static int RUNNING = 1;

		Handler mHandler;
		int mState;
		int total;

		// Constructor with an argument that specifies Handler on main thread
		// to which messages will be sent by this thread.

		ProgressThread(Handler h) {
			mHandler = h;
		}

		// Override the run() method that will be invoked automatically when 
		// the Thread starts.  Do the work required to update the progress bar on this
		// thread but send a message to the Handler on the main UI thread to actually
		// change the visual representation of the progress. In this example we count
		// the index total down to zero, so the horizontal progress bar will start full and
		// count down.

		@Override
		public void run() {
			mState = RUNNING;   
			total = 100;
			while (mState == RUNNING) {
				// The method Thread.sleep throws an InterruptedException if Thread.interrupt() 
				// were to be issued while thread is sleeping; the exception must be caught.
				try {
					// Control speed of update (but precision of delay not guaranteed)
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Log.e("ERROR", "Thread was Interrupted");
				}

				// Send message (with current value of  total as data) to Handler on UI thread
				// so that it can update the progress bar.

				Message msg = mHandler.obtainMessage();
				Bundle b = new Bundle();
				b.putInt("total", total);
				msg.setData(b);
				mHandler.sendMessage(msg);

				total--;    // Count down
			}
		}

		// Set current state of thread (use state=ProgressThread.DONE to stop thread)
		public void setState(int state) {
			mState = state;
		}
	}
	
	
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mProgress.setProgress(mProgressStatus);
			c.setChecked(!c.isChecked());
		}
	};

}