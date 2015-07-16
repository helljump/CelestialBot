package ru.snoa.celestialbot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

public class SimpleEula {

	private String EULA_PREFIX = "eula_";
	private Activity activity;

	public SimpleEula(Activity context) {
		activity = context;
	}

	private PackageInfo getPackageInfo() {
		PackageInfo pi = null;
		try {
			pi = activity.getPackageManager()
					.getPackageInfo(activity.getPackageName(), PackageManager.GET_ACTIVITIES);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return pi;
	}

	public void show() {
		PackageInfo versionInfo = getPackageInfo();

		// the eulaKey changes every time you increment the version number in
		// the AndroidManifest.xml
		final String eulaKey = EULA_PREFIX + versionInfo.versionCode;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		boolean hasBeenShown = prefs.getBoolean(eulaKey, false);
		if (hasBeenShown == false) {

			// Show the Eula
			String title = activity.getString(R.string.app_name) + " v" + versionInfo.versionName;

			// Includes the updates as well so users know what changed.
			String message = activity.getString(R.string.updatestext) + "\n\n" + activity.getString(R.string.eulatext);

			AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(title).setMessage(message)
					.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							// Mark this version as read.
							SharedPreferences.Editor editor = prefs.edit();
							editor.putBoolean(eulaKey, true);
							editor.commit();
							dialogInterface.dismiss();
						}
					}).setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Close the activity as they have declined the EULA
							activity.finish();
						}

					});
			builder.create().show();
		}
	}
}
