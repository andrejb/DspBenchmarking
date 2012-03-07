package br.usp.br.dspbenchmarking;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ToggleButton;

public class TestActivity extends Activity {
	
	private ToggleButton toggleTests = null;
	private ProgressBar workingBar = null;
	private DspThread dt;
	
	private static final int MAX_DSP_CYCLES = 100;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		// Set the view
		setContentView(R.layout.tests);
		
		toggleTests = (ToggleButton) findViewById(R.id.toggleTests);
		toggleTests.setTextOff("start");
		toggleTests.setTextOn("running tests...");
		
		workingBar = (ProgressBar) findViewById(R.id.workingBar);
		workingBar.setVisibility(ProgressBar.INVISIBLE);
	}
	
	
	public void toggleTests(View v) {
		if (toggleTests.isChecked()) {
			toggleTests.setClickable(false);
			workingBar.setVisibility(ProgressBar.VISIBLE);
			initTests();
		} else {
			workingBar.setVisibility(ProgressBar.INVISIBLE);
		}
	}
	
	private void initTests() {
		AssetFileDescriptor alienOrifice = getResources().openRawResourceFd(R.raw.alien_orifice);
		BufferedInputStream wavStream = new BufferedInputStream(new FileInputStream(alienOrifice.getFileDescriptor()));
		try {
			wavStream.skip(44);
		} catch (IOException e) {
			
		}
		wavStream.mark(2000000);
		performTest(64, 0, wavStream);
	}
	
	private void performTest(int blockSize, int algorithm, InputStream musicStream) {
		dt = new DspThread(blockSize, algorithm, musicStream, MAX_DSP_CYCLES);
		dt.setParams(0.5);
		dt.start();
	}
	
	
	
}
