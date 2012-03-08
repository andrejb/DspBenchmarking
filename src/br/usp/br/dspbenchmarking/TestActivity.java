package br.usp.br.dspbenchmarking;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ToggleButton;

public class TestActivity extends StatsActivity {

	private ToggleButton toggleTests = null;
	private ProgressBar workingBar = null;
	private DspThread dt;

	private static final int MAX_DSP_CYCLES = 10000;

	/**
	 * 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.tests);
		super.onCreate(savedInstanceState); // Set the view

		toggleTests = (ToggleButton) findViewById(R.id.toggleTests);
		toggleTests.setTextOff("start");
		toggleTests.setTextOn("running tests...");

		workingBar = (ProgressBar) findViewById(R.id.workingBar);
		workingBar.setVisibility(ProgressBar.INVISIBLE);

		Log.e("TestActivity", "swt=" + swt);
	}

	/**
	 * 
	 * @param v
	 */
	public void toggleTests(View v) {
		if (toggleTests.isChecked()) {
			toggleTests.setClickable(false);
			workingBar.setVisibility(ProgressBar.VISIBLE);
			initTests();
		} else {
			turnOff();
		}
	}
	
	private void turnOff() {
		toggleTests.setClickable(true);
		workingBar.setVisibility(ProgressBar.INVISIBLE);
	}

	/**
	 * 
	 */
	private void initTests() {
		try {
			InputStream is = getResources().openRawResourceFd(
					R.raw.alien_orifice).createInputStream();
			dt = new DspThread(64, 0, is, MAX_DSP_CYCLES);
			dt.setParams(0.5);
			dt.start();
			MonitorThread mt = new MonitorThread();
			mt.start();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @author andre
	 *
	 */
	private class MonitorThread extends Thread {

		@Override
		public void run() {

			while (dt.isRunning())
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Log.e("ERROR", "Thread was Interrupted");
				}
			turnOff();
		}
	}
	

}
