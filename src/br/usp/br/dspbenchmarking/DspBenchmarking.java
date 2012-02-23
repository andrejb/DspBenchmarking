package br.usp.br.dspbenchmarking;

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
        setContentView(R.layout.main);
        ProgressBar pb = (ProgressBar) findViewById(R.id.pb1);
        EditText et = (EditText) findViewById(R.id.texto1);
        et.setText(pb.toString());
        pb.setProgress(40);
    }
    
    public void startDSP(View v){
      Intent dsp_intent = new Intent(DspBenchmarking.this, DSP.class);
      DspBenchmarking.this.startActivity(dsp_intent);
    }
    
}
