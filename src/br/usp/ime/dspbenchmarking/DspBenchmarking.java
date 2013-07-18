package br.usp.ime.dspbenchmarking;

import br.usp.ime.dspbenchmarking.activities.AllTestsActivity;
import br.usp.ime.dspbenchmarking.activities.LiveActivity;
import br.usp.ime.dspbenchmarking.activities.StressActivity;
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

public class DspBenchmarking extends Activity {
	
	
	private String results;
	private Button buttonAllTestsActivity;
	private TextView textAirplaneMode;
	
	private static final boolean BYPASS_AIRPLANE_MODE = true; 
	
	
    /**
     * Called when activity is started:
     *   - Set main view.
     *   - Hide unwanted buttons (benchmark, stress).
     * Allows for calling one of two activities:
     *   - LiveActivity.
     *   - BenchmarkActivity.
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
     * Calls subsequent test activities and send results in the end.
     */
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
    	if (requestCode == 1 && resultCode == RESULT_OK) {
    		results = data.getStringExtra("results");
    		Intent i = new Intent(DspBenchmarking.this, StressActivity.class);
    		DspBenchmarking.this.startActivityForResult(i, 2);
    	} else if (requestCode == 2 && resultCode == RESULT_OK) {
    		results += data.getStringExtra("results");
    		sendResults("Test results");
    	}
    }
    
    /**
     * Send test results by email.
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
