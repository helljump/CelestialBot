package ru.snoa.celestialbot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class PrefsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener,
		OnPreferenceChangeListener {

	private static final String TAG = "PrefsActivity";

	private boolean mNeedReload = false;
	private SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);

		getPreferenceScreen().findPreference("lat").setOnPreferenceChangeListener(this);
		getPreferenceScreen().findPreference("lng").setOnPreferenceChangeListener(this);
		getPreferenceScreen().findPreference("elv").setOnPreferenceChangeListener(this);
	}

	@Override
	protected void onDestroy() {
		prefs.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();

	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mNeedReload) {
			Log.d(TAG, "Need reload");
			Intent in = new Intent(this, ParserService.class);
			in.setAction(ParserService.ACTION_PARSE);
			startService(in);
			return;
		}

		boolean val = prefs.getBoolean("alarms", false);
		if (val) {
			MainApplication.stopPassAlarm(this);
			MainApplication.setSilentAlarm(this);
		} else {
			MainApplication.stopPassAlarm(this);
		}

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key.contentEquals("frequency")) {
			String val = prefs.getString(key, "0");
			if (val.contentEquals("0")) {
				Log.d(TAG, "stopParserService");
				MainApplication.stopParserService(this);
			} else {
				Log.d(TAG, "startParserService");
				MainApplication.stopParserService(this);
				MainApplication.startParserService(this, Long.parseLong(val));
			}
		} else if (key.contentEquals("lat") || key.contentEquals("lng") || key.contentEquals("elv")
				|| key.contentEquals("tz")) {
			mNeedReload = true;
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (newValue != null && newValue.toString().matches("-?\\d{1,3}\\.?\\d{0,10}")) {
			float v = Float.parseFloat((String) newValue);
			if (v > -360 && v < 360) {
				return true;
			}
		}
		Toast.makeText(this, R.string.wrong_value, Toast.LENGTH_SHORT).show();
		return false;
	}

}
