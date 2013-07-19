package br.usp.ime.dspbenchmarking.activities;

import java.io.IOException;
import java.io.InputStream;

import br.usp.ime.dspbenchmarking.R;
import br.usp.ime.dspbenchmarking.threads.DspThread;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;

/**
 * The live activity allows for use of the DSP facilities during a live
 * performance. In fact, it can be seen more as an "examples" screen with
 * the implemented algorithms and possibility of switching between distinct
 * block sizes, audio inputs and algorithms. 
 * 
 * @author andrejb
 *
 */
public class LiveActivity extends DspActivity {
	
	//private static final String TAG = "DspActivity";

	// Variables from the GUI
	private int blockSize = 64;
	private int dspAlgorithm = 1;
	int maxParamValue = 100;
	private double parameter1 = 1.0;
	private int audioSource = 0; // Microphone is default.

	// Views
	private CheckBox toggleDSPView;
	
	private Spinner dspBlockSizeView = null;
	private Spinner audioSourceView = null;
	private Spinner algorithmView = null;
	private SeekBar parameter1View = null;



	/**
	 * Called when the activity is first created. Setup the screen with options
	 * for modifying the DSP parameters (block size, algorithm, audio input,
	 * etc).
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set the view
		setContentView(R.layout.dsp);
		super.onCreate(savedInstanceState);
				
		// Get views
		toggleDSPView = (CheckBox) findViewById(R.id.toggle_dsp);
		parameter1View = (SeekBar) findViewById(R.id.param1);

		// Init algorithms list
		algorithmView = (Spinner) findViewById(R.id.algorithm);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.algorithm_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		algorithmView.setAdapter(adapter);
		algorithmView.setOnItemSelectedListener(new AlgorithmListener());
		algorithmView.setSelection(0);
		
		// Init block size list
		dspBlockSizeView = (Spinner) findViewById(R.id.dspBlockSize);
		ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
				this, R.array.block_size_array,
				android.R.layout.simple_spinner_item);
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dspBlockSizeView.setAdapter(adapter2);
		dspBlockSizeView.setOnItemSelectedListener(new BlockSizeListener());
		dspBlockSizeView.setSelection(6);
		
		// Init audio source list
		audioSourceView = (Spinner) findViewById(R.id.audioSource);
		ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(
				this, R.array.source_array,
				android.R.layout.simple_spinner_item);
		adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		audioSourceView.setAdapter(adapter3);
		audioSourceView.setOnItemSelectedListener(new AudioSourceListener());
		audioSourceView.setSelection(0);

		// Input parameters listeners
		parameter1View.setMax(maxParamValue);
		parameter1View.setProgress(maxParamValue);
		parameter1View.setOnSeekBarChangeListener(parameter1Listener);
				
	}
	
	/**
	 * Stop DSP and finish the activity when "back" is pressed.
	 */
	public void onBackPressed() {
		if (toggleDSPView.isChecked()) {
			toggleDSPView.setChecked(false);
			toggleDSP(null);
		}
		setResult(1);
		finish();
	}

	
	/**
	 * Turn processing on and off, according to button in
	 * 'res/layout/main.xml' interface. 
	 * @param v
	 */
	public void toggleDSP(View v) {
		/* Turn DSP ON */
		if (toggleDSPView.isChecked()) {
			Log.i("DSP", "Starting new DSP thread.");
			dt = new DspThread();
			dt.setBlockSize(blockSize);
			dt.setAlgorithm(dspAlgorithm);
			dt.setAudioSource(audioSource);
			dt.setParams(parameter1);
			// if reading WAV, set input stream
			if (audioSource == DspThread.AUDIO_SOURCE_WAV) {
				InputStream is = null;
				try {
					is = getResources().openRawResourceFd(R.raw.alien_orifice).createInputStream();
				} catch (NotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				dt.setInputStream(is);
			}
			dt.start();
			dt.resumeDsp();
			// mProgressStatus = (int) readUsage() * 100;
		}
		/* Turn DSP OFF */
		else {
			Log.i("DSP", "Stopping existing DSP thread.");
			try {
				dt.stopDspThread();
				dt = null;
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
	}

	
	/************************************************************************
	 * Listeners for algorithm change.
	 ***********************************************************************/
	private class AlgorithmListener implements OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			dspAlgorithm = pos;
			restartDSP();
		}

		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
		}
	}

	
	/************************************************************************
	 * Listens for DSP block size change.
	 ***********************************************************************/
	private class BlockSizeListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {			// Perform action on clicks
			blockSize = (int) Math.pow(2, pos);
			restartDSP();
			// sampleWriteTime = 0;
			// dspCycleTime = 0;
			// ticks = 0;
		}
		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
		}
	};
	
	
	/************************************************************************
	 * Listens for audio source change
	 ***********************************************************************/
	private class AudioSourceListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {			// Perform action on clicks
			audioSource = pos;
			restartDSP();
			// sampleWriteTime = 0;
			// dspCycleTime = 0;
			// ticks = 0;
		}
		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
		}
	};
	
	
	/************************************************************************
	 * Restarts DSP with new parameters.
	 ***********************************************************************/
	private OnSeekBarChangeListener parameter1Listener = new OnSeekBarChangeListener() {
		public void onProgressChanged(SeekBar sb, int i, boolean b) {
			parameter1 = (float) i / maxParamValue;
			// set algorithm parameters
			if (dt != null)
				 dt.setParams(parameter1);
		}
		public void onStopTrackingTouch(SeekBar sb) {
			
		}
		public void onStartTrackingTouch(SeekBar sb) {
	
		}
	};
	
	
	/************************************************************************
	 * Restarts DSP with new parameters.
	 ***********************************************************************/
	private void restartDSP() {
		if (toggleDSPView != null)
			if (toggleDSPView.isChecked()) {
				toggleDSPView.setChecked(false);
				toggleDSP(null);
				toggleDSPView.setChecked(true);
				try {
					Thread.sleep(100);
				} catch (Exception e) {
				}
				toggleDSP(null);
			}
	}

}