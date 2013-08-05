package br.usp.ime.dspbenchmarking;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import br.usp.ime.dspbenchmarking.activities.AllTestsActivity;
import br.usp.ime.dspbenchmarking.activities.LiveActivity;
import br.usp.ime.dspbenchmarking.util.SystemInformation;


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
	private Button buttonAllTestsActivity;


	/**
	 * Called when activity is started:
	 *   - Set main view.
	 *   - Lock/unlock tests button depending on conectivity state.
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
		// Block test button if airplane mode is off
		buttonAllTestsActivity = (Button) findViewById(R.id.buttonStartAllTestsActivity);

		// Lock tests button if not ready for running tests.
		if (isReadyForRunningTests(this))
			textAirplaneMode.setVisibility(View.GONE);
		else
			buttonAllTestsActivity.setEnabled(false);			

		// Save this instance so we can later use to find out about airplane mode
		final DspBenchmarking dspBenchmarking = this;

		// Register to switch button state if flight mode, connectivity or wifi state changes.
		BroadcastReceiver receiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				TextView textAirplaneMode = (TextView) findViewById(R.id.textAirplaneMode);
				Log.d("AirplaneMode", "Service state changed");
				if (DspBenchmarking.isReadyForRunningTests(dspBenchmarking)) {
					buttonAllTestsActivity.setEnabled(true);
					textAirplaneMode.setVisibility(View.GONE);
				} else {
					buttonAllTestsActivity.setEnabled(false);
					textAirplaneMode.setVisibility(View.VISIBLE);
				}
			}
		};
		String[] connectivityIntentList = {
				"android.intent.action.SERVICE_STATE",
				"android.net.conn.CONNECTIVITY_CHANGE",
				"android.net.wifi.WIFI_STATE_CHANGED_ACTION",
		};
		for (int i = 0; i < connectivityIntentList.length; i++) {
			IntentFilter intentFilter = new IntentFilter(connectivityIntentList[i]);
			this.getApplicationContext().registerReceiver(receiver, intentFilter);
		}
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
		tests_intent.putExtra(AllTestsActivity.MESSAGE_RUN_TESTS, true);
		DspBenchmarking.this.startActivity(tests_intent);
	}

	/**
	 * This method will return 'true' only if:
	 *   - Wifi is disabled.
	 *   - There is no network connection.
	 *   - Airplane mode is on (i.e. phone is not connected to a cell).
	 * @param activity
	 * @return
	 */
	private static boolean isReadyForRunningTests(Activity activity) {
		return !SystemInformation.isWifiEnabled(activity)
				&& !SystemInformation.isConnectedOrConnecting(activity)
				&& SystemInformation.isAirplaneModeOn(activity);
	}
}
