package ru.snoa.celestialbot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import ru.snoa.celestialbot.heavensabove.Pass;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class MainApplication extends Application {

    public static final String LAST_UPDATE = "last_update";
    private static final String TAG = "MainApplication";
    public static final String PASSES_JSON = "passes.json";
    private static WeakReference<ArrayList<Pass>> passesRef;
    static final String CELESTIALBOT_DONATE = "ru.snoa.celestialbot.donate";
    protected boolean mRegistered = false;

    @Override
    public void onCreate() {
        super.onCreate();
        passesRef = new WeakReference<ArrayList<Pass>>(null);
        mRegistered = isRegistered(this);
    }

    protected static boolean isRegistered(Context context) {
        final PackageManager pm = context.getPackageManager();
        List<PackageInfo> list = pm.getInstalledPackages(PackageManager.GET_DISABLED_COMPONENTS);
        Iterator<PackageInfo> i = list.iterator();
        while (i.hasNext()) {
            PackageInfo p = i.next();
            if ((p.packageName.equals(MainApplication.CELESTIALBOT_DONATE))
                    && (pm.checkSignatures(context.getPackageName(), p.packageName) == PackageManager.SIGNATURE_MATCH))
                return true;
        }
        return false;
    }

    public static boolean isAlarmOn(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("alarms", false);
    }

    /**
     * Тормознуть создание алармов на нотификацию пролетов
     *
     * @param context
     */
    public static void stopPassAlarm(Context context) {
        Log.d(TAG, "stopPassAlarm");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent in = new Intent(context, ParserService.class);
        in.setAction(ParserService.ACTION_ALARM);
        PendingIntent pin = PendingIntent.getService(context, ParserService.SERVICE_ID, in,
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pin);
    }

    /**
     * Установить Беззвучный аларм на ближайшее пасс.
     *
     * @param context
     */
    public static void setSilentAlarm(Context context) {
        Log.d(TAG, "setSilentAlarm");
        Intent in = new Intent(context, ParserService.class);
        in.setAction(ParserService.ACTION_ALARM);
        in.putExtra("alarm", false); // т.к. аларм совмещен с нотифаем
        context.startService(in);
    }

    /**
     * Останов сервиса тырнет обновлений
     *
     * @param context
     */
    public static void stopParserService(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent in = new Intent(context, ParserService.class);
        in.setAction(ParserService.ACTION_PARSE);
        PendingIntent service = PendingIntent.getService(context, ParserService.SERVICE_ID, in,
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(service);
    }

    /**
     * Запуск сервиса интернет обновлений
     *
     * @param context
     * @param frequency
     */
    public static void startParserService(Context context, long frequency) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int in_minutes = (int) (frequency / 1000 / 60);
        calendar.add(Calendar.MINUTE, in_minutes);

        Intent in = new Intent(context, ParserService.class);
        in.setAction(ParserService.ACTION_PARSE);
        PendingIntent pendingIntent = PendingIntent.getService(context, ParserService.SERVICE_ID, in,
                PendingIntent.FLAG_UPDATE_CURRENT);
        int mode = AlarmManager.RTC;
        alarmManager.setRepeating(mode, calendar.getTimeInMillis(), frequency, pendingIntent);
    }

    public static List<Pass> readJson(Context context) {
        return readJson(context, true);
    }

    public static List<Pass> readJson(Context context, boolean force) {
        if (force) {
            passesRef.clear();
        }
        ArrayList<Pass> filtered = passesRef.get();
        if (filtered != null) {
            Log.d(TAG, "readJson, size:" + filtered.size());
            return filtered;
        }
        filtered = new ArrayList<Pass>();
        ArrayList<Pass> passes = new ArrayList<Pass>();
        Type collectionType = new TypeToken<ArrayList<Pass>>() {
        }.getType();
        FileInputStream finp;
        long ct = System.currentTimeMillis();
        try {
            finp = context.openFileInput(PASSES_JSON);
            InputStreamReader osw = new InputStreamReader(finp);
            Gson gs = new Gson();
            passes = gs.fromJson(osw, collectionType);
            Log.d(TAG, "readJson, size:" + passes.size());
            osw.close();
            for (Pass pass : passes) {
                if (ct < pass.date.getTime())
                    filtered.add(pass);
            }
        } catch (IOException e) {
            Log.e(TAG, "readJson:" + e);
        }
        passesRef = new WeakReference<ArrayList<Pass>>(filtered);
        return filtered;
    }

    public static void writePasses(Context context, ArrayList<Pass> passes) {
        FileOutputStream fout;
        try {
            fout = context.openFileOutput(MainApplication.PASSES_JSON, MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fout);
            Gson gs = new GsonBuilder().setPrettyPrinting().create();
            gs.toJson(passes, osw);
            osw.close();

            SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
            Calendar c = Calendar.getInstance();
            prefs.putLong(LAST_UPDATE, c.getTimeInMillis());
            prefs.commit();

        } catch (FileNotFoundException e) {
            Log.e(TAG, "writePasses:" + e);
        } catch (IOException e) {
            Log.e(TAG, "writePasses:" + e);
        }
    }

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMgr.getActiveNetworkInfo() != null && conMgr.getActiveNetworkInfo().isAvailable()
                && conMgr.getActiveNetworkInfo().isConnected()) {
            return true;
        }
        return false;
    }

}
