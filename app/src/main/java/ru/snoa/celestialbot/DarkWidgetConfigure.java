package ru.snoa.celestialbot;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class DarkWidgetConfigure extends Activity {

	static final String TAG = "DarkWidgetConfigure";
	private static final String PREFS_NAME = "darkwidget";
	private static final String PREF_BG = "widgetbg_";
	int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	Intent resultValue;
	Spinner spinnerWidgetBg;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		Log.d(TAG, "configure");

		setContentView(R.layout.darkwidget_configure);
		spinnerWidgetBg = (Spinner) findViewById(R.id.spinner_widgetbg);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}

		resultValue = new Intent();
	    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
	    setResult(RESULT_CANCELED, resultValue);
	}

	@Override
	protected void onResume() {
		super.onResume();
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.widgetbg_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerWidgetBg.setAdapter(adapter);
	}

	public void saveSettings(View view) {
		SharedPreferences.Editor prefs = this.getSharedPreferences(PREFS_NAME, 0).edit();
		int bg = spinnerWidgetBg.getSelectedItemPosition();
		prefs.putInt(PREF_BG + widgetId, bg);
		prefs.commit();

		setResult(RESULT_OK, resultValue);
		
		Intent in = new Intent();
		in.setAction(DarkWidget.WIDGET_UPDATE);
		sendBroadcast(in);

		finish();
	}

	public static int getBackground(Context context, int id) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		int bg = prefs.getInt(PREF_BG + id, 0);
		return bg;
	}

	public static void deletePref(Context context, int id) {
		Log.d(TAG, "delete prefs for " + id);
		SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
		prefs.remove(PREF_BG + id);
		prefs.commit();
	}

}
