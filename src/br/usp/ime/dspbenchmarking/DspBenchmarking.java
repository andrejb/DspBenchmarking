package br.usp.ime.dspbenchmarking;

import br.usp.ime.dspbenchmarking.activities.BenchmarkActivity;
import br.usp.ime.dspbenchmarking.activities.LiveActivity;
import br.usp.ime.dspbenchmarking.activities.StressActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class DspBenchmarking extends Activity {
	
	
	private String results;
	private Button buttonBenchmarkActivity;
	private Button buttonStressActivity;
	
	
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
	    buttonBenchmarkActivity = (Button) findViewById(R.id.buttonBenchmarkActivity);
	    buttonStressActivity = (Button) findViewById(R.id.buttonStressActivity);
	    //Log.e("coisa", "coisa1="+buttonBenchmarkActivity);
	    //Log.e("coisa", "coisa2="+buttonStressActivity);
	    buttonBenchmarkActivity.setVisibility(View.GONE);
	    buttonStressActivity.setVisibility(View.GONE);
    }
    
    /**
     * Start activity that allows use of DSP system for live performance.
     * @param v
     */
    public void startLiveActivity(View v){
    	Intent dsp_intent = new Intent(DspBenchmarking.this, LiveActivity.class);
    	DspBenchmarking.this.startActivity(dsp_intent);
    }

    /**
     * Start activity that runs all tests.
     * @param v
     */
    public void startAllTestsActivity(View v) {
    	Intent i = new Intent(DspBenchmarking.this, BenchmarkActivity.class);
    	DspBenchmarking.this.startActivityForResult(i, 1);
    }
    
    /**
     * This method would be called by a specific button in main view, but the
     * button is hidden so it is not called at all.
     * @param v
     */
    public void startTestActivity(View v) {
    	Intent test_intent = new Intent(DspBenchmarking.this, BenchmarkActivity.class);
    	DspBenchmarking.this.startActivity(test_intent);
    }
    
    /**
     * This method would be called by a specific button in main view, but the
     * button is hidden so it is not called at all.
     * @param v
     */
    public void startStressActivity(View v) {
    	Intent stress_intent = new Intent(DspBenchmarking.this, StressActivity.class);
    	DspBenchmarking.this.startActivity(stress_intent);
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
}
