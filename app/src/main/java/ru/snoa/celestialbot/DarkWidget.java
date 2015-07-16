package ru.snoa.celestialbot;

import java.text.SimpleDateFormat;
import java.util.List;

import ru.snoa.celestialbot.heavensabove.Pass;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class DarkWidget extends AppWidgetProvider {

	private static final String TAG = "DarkWidget";
	public static final String WIDGET_UPDATE = "ru.snoa.celestialbot.action.WIDGET_UPDATE";

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		if (intent.getAction().equals(WIDGET_UPDATE)) {
			ComponentName appWidget = new ComponentName(context.getPackageName(), getClass().getName());
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			int ids[] = appWidgetManager.getAppWidgetIds(appWidget);
			onUpdate(context, appWidgetManager, ids);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Log.d(TAG, "onUpdate");
		List<Pass> passes = MainApplication.readJson(context);
		Pass pass = null;
		if (passes != null && passes.size() > 0) {
			pass = passes.get(0);
		}

		final String empty = "";
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd MMM");

		for (int i = 0; i < appWidgetIds.length; i++) {
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.darkwidget_layout);

			Intent clickIntent = new Intent(context, MainActivity.class);
			PendingIntent clickpin = PendingIntent.getActivity(context, 0, clickIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.darkwidget_layout, clickpin);

			final int bg = DarkWidgetConfigure.getBackground(context, appWidgetIds[i]);
			if (bg == 1) { // transparent
				views.setInt(R.id.darkwidget_layout, "setBackgroundResource", 0);
			}

			if (pass != null) {
				views.setTextViewText(R.id.name_view, pass.name);
				views.setTextViewText(R.id.brightness_view, String.format("%.1f", pass.brightness));
				views.setTextViewText(R.id.azimuth_view, String.format("%.1f°(%s)", pass.az, pass.getAzimuth()));
				views.setTextViewText(R.id.altitude_view, String.format("%.1f°", pass.alt));
				views.setTextViewText(R.id.datetime_view, df.format(pass.date));
				if (pass.name.contentEquals("ISS")) {
					views.setImageViewResource(R.id.icon_view, R.drawable.iss);
				} else {
					views.setImageViewResource(R.id.icon_view, R.drawable.satellite);
				}
			} else {
				views.setTextViewText(R.id.name_view, context.getString(R.string.no_data));
				views.setTextViewText(R.id.brightness_view, empty);
				views.setTextViewText(R.id.azimuth_view, empty);
				views.setTextViewText(R.id.altitude_view, empty);
				views.setTextViewText(R.id.datetime_view, empty);
			}

			appWidgetManager.updateAppWidget(appWidgetIds[i], views);
		}

		if (pass != null) {
			Intent in = new Intent(WIDGET_UPDATE);
			PendingIntent pin = PendingIntent.getBroadcast(context, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			manager.set(AlarmManager.RTC, pass.date.getTime(), pin);
		}

	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		Intent in = new Intent(WIDGET_UPDATE);
		PendingIntent pin = PendingIntent.getBroadcast(context, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		manager.cancel(pin);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		Log.d(TAG, "delete");
		final int N = appWidgetIds.length;
		for (int i = 0; i < N; i++) {
			DarkWidgetConfigure.deletePref(context, appWidgetIds[i]);
		}
	}

}
