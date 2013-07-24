package br.usp.ime.dspbenchmarking;

import br.usp.ime.dspbenchmarking.activities.AllTestsActivity;
import br.usp.ime.dspbenchmarking.activities.LiveActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


/**
 * The main activity, called when the program starts. It basically
 * lets the user to choose from using the DSP facilities in an example
 * activity (which I called "LiveActivity") or running automated tests that
 * will send a report through email when they are finished.
 * 
 * @author andrejb
 *
 */
public class DspBenchmarking extends Activity {
	
	
	// Views
	private TextView textAirplaneMode;
	
	
    /**
     * Called when activity is started:
     *   - Set main view.
     *   - Hide unwanted buttons (benchmark, stress).
     * Allows for calling one of two activities:
     *   - LiveActivity.
     *   - AllTestsActivity.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		// Prevent from locking
	    this.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	    // Set view
	    setContentView(R.layout.main);
	    // Hide airplane mode warning.
	    textAirplaneMode = (TextView) findViewById(R.id.textAirplaneMode);
	    textAirplaneMode.setVisibility(View.GONE);
    }
    
    /**
     * Start activity that allows use of DSP system for live performance.
     * @param v
     */
    public void startLiveActivity(View v){
    	Log.i("Main", "Starting live tool...");
    	Intent dsp_intent = new Intent(DspBenchmarking.this, LiveActivity.class);
    	DspBenchmarking.this.startActivity(dsp_intent);
    }

    /**
     * Start activity that runs all tests.
     * @param v
     */
    public void startAllTestsActivity(View v) {
    	Log.i("Main", "Starting all tests...");
    	Intent tests_intent = new Intent(DspBenchmarking.this, AllTestsActivity.class);
    	DspBenchmarking.this.startActivity(tests_intent);
    }
 }
