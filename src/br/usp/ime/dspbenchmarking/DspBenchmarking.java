package br.usp.ime.dspbenchmarking;

import br.usp.ime.dspbenchmarking.activities.AllTestsActivity;
import br.usp.ime.dspbenchmarking.activities.LiveActivity;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
	private Button buttonAllTestsActivity;
	private TextView textAirplaneMode;
	
	// Airplane mode is interesting so tests are not interrupted by a call.
	// The downside is that it may become more difficult to users to run
	// the test until the end and to send the results (because of lack of
	// connection -- i'm not sure about this argument).
	private static final boolean BYPASS_AIRPLANE_MODE = true; 
	
	
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

	    // Block test button if airplane mode is off
	    buttonAllTestsActivity = (Button) findViewById(R.id.buttonStartAllTestsActivity);
	    textAirplaneMode = (TextView) findViewById(R.id.textAirplaneMode);
	    if (!isAirplaneModeOn()) {
	    	buttonAllTestsActivity.setEnabled(false);
	    } else {
	    	textAirplaneMode.setVisibility(View.GONE);
	    }
	    
	    // Register to switch button state if flight mode is set
	    IntentFilter intentFilter = new IntentFilter("android.intent.action.SERVICE_STATE");
	    BroadcastReceiver receiver = new BroadcastReceiver() {
	          @Override
	          public void onReceive(Context context, Intent intent) {
	                Log.d("AirplaneMode", "Service state changed");
	        	    if (!isAirplaneModeOn()) {
	        	    	buttonAllTestsActivity.setEnabled(false);
	        	    	textAirplaneMode.setVisibility(View.VISIBLE);
	        	    } else {
	        	    	buttonAllTestsActivity.setEnabled(true);
	        	    	textAirplaneMode.setVisibility(View.GONE);
	        	    }
	          }
	    };
	    this.getApplicationContext().registerReceiver(receiver, intentFilter);	    
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
    
	/**
	* Gets the state of Airplane Mode.
	* 
	* @param context
	* @return true if enabled.
	*/
	private boolean isAirplaneModeOn() {
		if (BYPASS_AIRPLANE_MODE)
			return true;
	   return Settings.System.getInt(
			   this.getApplicationContext().getContentResolver(),
	           Settings.System.AIRPLANE_MODE_ON, 0) != 0;

	}
}
