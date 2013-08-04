package br.usp.ime.dspbenchmarking.util;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;


/**
 * Provide useful information about the system.
 * 
 * @author andrejb
 */
public class SystemInformation {

	/**
	 * Get the state of Airplane Mode.
	 *
	 * @param context
	 * @return true if enabled.
	 */
	public static boolean isAirplaneModeOn(Activity dspBenchmarking) {
		//TODO: fix this return to Android API v17 or higher if necessary: Setting.Global.AIRPLANE_MODE_ON
		int status = Settings.System.getInt(
				dspBenchmarking.getApplicationContext().getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, 0);
		Log.i("AIRPLANEMODE", "Airplane mode status is "+status+".");
		return status != 0;

	}

	/**
	 * Loop over connection types and check if any of them is either in
	 * 'connected' or 'connecting' state.
	 * 
	 * @param activity
	 * @return Whether there's any service type in 'connected' or 'connecting' state.
	 */
	public static boolean isConnectedOrConnecting(Activity activity) {
		int connectivityTypes[] = {
				ConnectivityManager.TYPE_MOBILE,
				ConnectivityManager.TYPE_WIFI,
				// The following types are not supported by this API level.
				/*ConnectivityManager.TYPE_BLUETOOTH,
				ConnectivityManager.TYPE_DUMMY,
				ConnectivityManager.TYPE_ETHERNET,
				ConnectivityManager.TYPE_MOBILE_DUN,
				ConnectivityManager.TYPE_MOBILE_HIPRI,
				ConnectivityManager.TYPE_MOBILE_MMS,
				ConnectivityManager.TYPE_MOBILE_SUPL,
				ConnectivityManager.TYPE_WIMAX*/
		};
		ConnectivityManager connMgr = (ConnectivityManager) 
				activity.getSystemService(Context.CONNECTIVITY_SERVICE);
		// Look for any service type that is either in connected or connecting state
		for (int i = 0; i < connectivityTypes.length; i++)
			if (connMgr.getNetworkInfo(connectivityTypes[i]).isConnectedOrConnecting())
				return true;
		return false;
	}

	/**
	 * Return whether wifi is enabled.
	 * 
	 * @param activity
	 * @return Whether wifi is enabled or not.
	 */
	public static boolean isWifiEnabled(Activity activity) {
		WifiManager ws = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		return ws.isWifiEnabled();
	}

}
