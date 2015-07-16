package ru.snoa.celestialbot;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import ru.snoa.celestialbot.heavensabove.Pass;
import ru.snoa.celestialbot.heavensabove.SiteParser;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class ParserService extends Service {

	public static final String ACTION_ALARM = "ru.snoa.celestialbot.action.ALARM";
	public static final String ACTION_PARSE = "ru.snoa.celestialbot.action.PARSE";
	public static final String ACTION_GET_STATUS = "ru.snoa.celestialbot.action.GET_STATUS";
	public static final String ACTION_STATUS = "ru.snoa.celestialbot.action.STATUS";
	public static final String ACTION_REFRESH = "ru.snoa.celestialbot.action.REFRESH";
	public static final String ACTION_MESSAGE = "ru.snoa.celestialbot.action.MESSAGE";

	private static final String TAG = "ParserService";
	public static final int SERVICE_ID = 345536275;
	private static final int SATELLITE_NOTIF = 1;
	private static final int PARSE_NOTIF = 2;

	private PowerManager pm;
	private SimpleDateFormat df;
	private PowerManager.WakeLock activelock;
	private SharedPreferences prefs;
	private NotificationManager nman;

	@Override
	public void onCreate() {
		super.onCreate();
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		df = new SimpleDateFormat("HH:mm:ss");
		activelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		nman = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) {
			return START_STICKY;
		}
		String action = intent.getAction();
		if (action != null) {
			if (action.equals(ACTION_GET_STATUS)) {
				Intent in = new Intent(ACTION_STATUS);
				in.putExtra("active", activelock.isHeld());
				sendBroadcast(in);
			} else if (action.equals(ACTION_PARSE)) {
				parseSite(startId);
			} else if (action.equals(ACTION_ALARM)) {
				if (MainApplication.isAlarmOn(this)) {
					setNearAlarm();
				}
				boolean alarm = intent.getBooleanExtra("alarm", false);
				if (alarm) {
					showPassNotify(intent.getStringExtra("text"));
				}
			}
		}
		return START_STICKY;
	}

	public synchronized void parseSite(int startId) {
		if (activelock.isHeld()) {
			Log.d(TAG, "parseSite: already active");
			return;
		}
		if (!MainApplication.isInternetAvailable(this)) {
			Intent in = new Intent(ACTION_MESSAGE);
			in.putExtra("text", getString(R.string.no_internet_connection));
			sendBroadcast(in);
			return;
		}

		Intent in = new Intent(ACTION_STATUS);
		in.putExtra("active", true);
		sendBroadcast(in);

		Thread t = new Thread(new Parser(this, startId));
		t.start();
	}

	private void showPassNotify(String nottext) {
		String ringtone = prefs.getString("ringtone", "DEFAULT_NOTIFICATION_URI");
		Uri sound = Uri.parse(ringtone);
		Intent in = new Intent(this, MainActivity.class);
		PendingIntent pin = PendingIntent.getActivity(this, 0, in, 0);
		Notification not = new Notification(R.mipmap.ic_launcher, getString(R.string.satellite_alarm), 0);
		not.flags |= Notification.FLAG_AUTO_CANCEL;
		not.sound = sound;
		not.setLatestEventInfo(this, getString(R.string.satellite_alarm), nottext, pin);
		nman.notify(SATELLITE_NOTIF, not);
	}

	private void showParseNotify() {
		Intent in = new Intent(this, MainActivity.class);
		PendingIntent pin = PendingIntent.getActivity(this, 0, in, 0);
		Notification not = new Notification(R.mipmap.ic_launcher, getString(R.string.getting_passes), 0);
		not.flags |= Notification.FLAG_AUTO_CANCEL;
		not.setLatestEventInfo(this, getString(R.string.getting_passes), getString(R.string.download_passes), pin);
		nman.notify(PARSE_NOTIF, not);
	}

	private void cancelParseNotify() {
		nman.cancel(PARSE_NOTIF);
	}

	private class Parser implements Runnable {

		private double elv = 0.0;
		private double lng = 0.0;
		private double lat = 0.0;
		private String tz = "GMT";
		private Service service;
		private int startId;

		public Parser(Service service, int startId) {
			this.service = service;
			this.startId = startId;
			try {
				elv = Double.parseDouble(prefs.getString("elv", "0.0"));
				lng = Double.parseDouble(prefs.getString("lng", "0.0"));
				lat = Double.parseDouble(prefs.getString("lat", "0.0"));
				tz = prefs.getString("tz", "GMT");
			} catch (NumberFormatException e) {
				Log.e(TAG, "prefs parse error");
			}
		}

		@Override
		public void run() {
			try {
				showParseNotify();
				activelock.acquire();
				SiteParser sp = new SiteParser(ParserService.this, lat, lng, elv, tz);
				ArrayList<Pass> passes = new ArrayList<Pass>();
				Log.d(TAG, "start parse heavens");
				ArrayList<Pass> egg = sp.parseIridium();
				if (egg != null) {
					Log.d(TAG, "parsed iridium:" + egg.size());
					passes.addAll(egg);
				} else {
					Log.e(TAG, "iridium error");
				}
				egg = sp.parseISS();
				if (egg != null) {
					Log.d(TAG, "parsed iss:" + egg.size());
					passes.addAll(egg);
				} else {
					Log.e(TAG, "iss error");
				}
								
				Collections.sort(passes);
				MainApplication.writePasses(service, passes);

				Intent in = new Intent(ACTION_REFRESH);
				sendBroadcast(in);

				//fixme
				//in = new Intent(DarkWidget.WIDGET_UPDATE);
				//sendBroadcast(in);

				if (MainApplication.isAlarmOn(ParserService.this)) {
					Log.d(TAG, "Parser.run");
					MainApplication.setSilentAlarm(ParserService.this);
				}

			} catch (IOException e) {
				Log.e(TAG, "siteparse error:" + e);
				Intent in = new Intent(ACTION_MESSAGE);
				in.putExtra("text", getString(R.string.connection_error));
				sendBroadcast(in);
			} finally {
				activelock.release();
				cancelParseNotify();
			}
			stopSelf(startId);
		}
	}

	public void setNearAlarm() {
		double minalt = Double.parseDouble(prefs.getString("altitude_alarm", "30.0"));
		double minbright = Double.parseDouble(prefs.getString("brightness_alarm", "8.0"));
		long odds = Long.parseLong(prefs.getString("odds_alarm", "300000"));

		final String pb = "period_alarm";
		int mm = prefs.getInt(pb + PeriodPreference.MINUTE_FROM, 0);
		int hh = prefs.getInt(pb + PeriodPreference.HOUR_FROM, 0);
		int from = hh * 60 + mm;
		mm = prefs.getInt(pb + PeriodPreference.MINUTE_TO, 0);
		hh = prefs.getInt(pb + PeriodPreference.HOUR_TO, 0);
		int to = hh * 60 + mm;

		Pass pass = getPassTime(from, to, minbright, minalt, odds);
		if (pass == null) {
			Log.d(TAG, "no passes");
			return;
		}

		long passtime = pass.date.getTime() - odds;

		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent in = new Intent(this, ParserService.class);
		in.setAction(ACTION_ALARM);
		in.putExtra("alarm", true);
		String text = String.format(getString(R.string.notif_text), pass.name, pass.brightness, df.format(pass.date),
				pass.az, pass.getAzimuth(), pass.alt);
		in.putExtra("text", text);
		PendingIntent pin = PendingIntent.getService(this, SERVICE_ID, in, PendingIntent.FLAG_UPDATE_CURRENT);

		alarmManager.cancel(pin);

		alarmManager.set(AlarmManager.RTC_WAKEUP, passtime, pin);

		Log.d(TAG, "reset alarm on: " + passtime + " name: " + pass.name);
	}

	/**
	 * Возвращает первый пролёт для заданных условий. Нужно для виджета и
	 * напоминалок.
	 * 
	 * @param from
	 *            Начало диапазона времени
	 * @param to
	 *            Конец диапазона
	 * @param minbright
	 *            минимальная яркость(чем меньше, тем ярче)
	 * @param minalt
	 *            минимальная высота
	 * @param odds
	 *            упреждение
	 * 
	 * @return Пролёт или null если нет таковых
	 */
	public Pass getPassTime(int from, int to, double minbright, double minalt, long odds) {
		Log.d(TAG, "getPassTime");
		List<Pass> passes = MainApplication.readJson(this);
		Calendar c = Calendar.getInstance();
		long ct = System.currentTimeMillis();
		for (Pass pass : passes) {
			c.setTime(pass.date);
			c.add(Calendar.MILLISECOND, (int) -odds);

			if (c.getTimeInMillis() < ct) {
				continue;
			}
			int passtime = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
			int midnight = 1440;
			if (minbright >= pass.brightness && minalt <= pass.alt) {
				if (from < to) {
					if (passtime >= from && passtime < to) {
						return pass;
					}
				} else {
					if ((passtime >= from && passtime < midnight) || (passtime >= 0 && passtime < to)) {
						return pass;
					}
				}
			}
		}
		return null;
	}
}
