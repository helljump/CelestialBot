package ru.snoa.celestialbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class MainReceiver extends BroadcastReceiver {
	private static final String TAG = "MainReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		long frequency = Long.parseLong(prefs.getString("frequency", "0"));

		if (action.equals("android.intent.action.BOOT_COMPLETED")) {
			if (frequency > 0) {
				Log.d(TAG, "Start internet-updates service and set nearest notification");
				MainApplication.startParserService(context, frequency);
				MainApplication.setSilentAlarm(context);
			}
		} else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
			long last_update = prefs.getLong(MainApplication.LAST_UPDATE, 0);
			if (frequency == 0) {
				return; // autoupdate disabled
			}
			if (System.currentTimeMillis() - last_update > frequency) {
				Log.d(TAG, "Start internet-update on connectivity changed");
				Intent in = new Intent(context, ParserService.class);
				in.setAction(ParserService.ACTION_PARSE);
				context.startService(in);
			}
		}
	}

}
