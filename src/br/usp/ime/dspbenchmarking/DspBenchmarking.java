package br.usp.ime.dspbenchmarking;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

public class DspBenchmarking extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		// Prevent from locking
	    this.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Set view
	    setContentView(R.layout.main);
        ProgressBar pb = (ProgressBar) findViewById(R.id.pb1);
        EditText et = (EditText) findViewById(R.id.texto1);
        et.setText(pb.toString());
        pb.setProgress(40);
    }
    
    public void startLiveActivity(View v){
    	Intent dsp_intent = new Intent(DspBenchmarking.this, LiveActivity.class);
    	DspBenchmarking.this.startActivity(dsp_intent);
    }
    
    public void startTestActivity(View v) {
    	Intent test_intent = new Intent(DspBenchmarking.this, TestActivity.class);
    	DspBenchmarking.this.startActivity(test_intent);
    }
    
}
