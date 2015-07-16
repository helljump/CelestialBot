package ru.snoa.celestialbot;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TimePicker;

// FIXME: починить сохранение состояния при повороте

public class PeriodPreference extends DialogPreference {
	public static final String MINUTE_TO = ".minute_to";
	public static final String HOUR_TO = ".hour_to";
	public static final String MINUTE_FROM = ".minute_from";
	public static final String HOUR_FROM = ".hour_from";

	/** The widget for picking a time */
	private TimePicker fromTimePicker;
	private TimePicker toTimePicker;

	/** Default hour */
	private static final int DEFAULT_HOUR = 20;
	private static final int DEFAULT_HOURTO = 6;
	private static final int DEFAULT_MINUTE = 0;

	// private Parcelable tmpdata = null;

	/*
	 * protected Parcelable onSSaveInstanceState() { Log.d("PeriodPreference",
	 * "saving"); SavedState ss = new SavedState(); ss.hour_from =
	 * fromTimePicker.getCurrentHour(); ss.minute_from =
	 * fromTimePicker.getCurrentMinute(); ss.hour_to =
	 * toTimePicker.getCurrentHour(); ss.minute_to =
	 * toTimePicker.getCurrentMinute(); return ss; }
	 */

	/**
	 * Creates a preference for choosing a time based on its XML declaration.
	 * 
	 * @param context
	 * @param attributes
	 */
	public PeriodPreference(Context context, AttributeSet attributes) {
		super(context, attributes);
		setPersistent(false);
	}

	/**
	 * Initialize time picker to currently stored time preferences.
	 * 
	 * @param view
	 *            The dialog preference's host view
	 */
	@Override
	public void onBindDialogView(View view) {
		super.onBindDialogView(view);

		Log.d("PerodPreference", "binding");

		fromTimePicker = (TimePicker) view.findViewById(R.id.from_timePicker);

		fromTimePicker.setIs24HourView(DateFormat.is24HourFormat(fromTimePicker.getContext()));
		fromTimePicker.setCurrentHour(getSharedPreferences().getInt(getKey() + HOUR_FROM, DEFAULT_HOUR));
		fromTimePicker.setCurrentMinute(getSharedPreferences().getInt(getKey() + MINUTE_FROM, DEFAULT_MINUTE));

		toTimePicker = (TimePicker) view.findViewById(R.id.to_timePicker);

		toTimePicker.setIs24HourView(DateFormat.is24HourFormat(toTimePicker.getContext()));
		toTimePicker.setCurrentHour(getSharedPreferences().getInt(getKey() + HOUR_TO, DEFAULT_HOURTO));
		toTimePicker.setCurrentMinute(getSharedPreferences().getInt(getKey() + MINUTE_TO, DEFAULT_MINUTE));
	}

	/**
	 * Handles closing of dialog. If user intended to save the settings,
	 * selected hour and minute are stored in the preferences with keys KEY.hour
	 * and KEY.minute, where KEY is the preference's KEY.
	 * 
	 * @param okToSave
	 *            True if user wanted to save settings, false otherwise
	 */
	@Override
	protected void onDialogClosed(boolean okToSave) {
		super.onDialogClosed(okToSave);
		if (okToSave) {
			fromTimePicker.clearFocus();
			SharedPreferences.Editor editor = getEditor();
			editor.putInt(getKey() + HOUR_FROM, fromTimePicker.getCurrentHour());
			editor.putInt(getKey() + MINUTE_FROM, fromTimePicker.getCurrentMinute());
			editor.putInt(getKey() + HOUR_TO, toTimePicker.getCurrentHour());
			editor.putInt(getKey() + MINUTE_TO, toTimePicker.getCurrentMinute());
			editor.commit();
		}
	}

	/*
	 * private static class SavedState implements Parcelable { int hour_from;
	 * int minute_from; int hour_to; int minute_to;
	 * 
	 * public SavedState() { }
	 * 
	 * public SavedState(Parcel source) { hour_from = source.readInt();
	 * minute_from = source.readInt(); hour_to = source.readInt(); minute_to =
	 * source.readInt(); }
	 * 
	 * @Override public void writeToParcel(Parcel dest, int flags) {
	 * dest.writeInt(hour_from); dest.writeInt(minute_from);
	 * dest.writeInt(hour_to); dest.writeInt(minute_to); }
	 * 
	 * public static final Parcelable.Creator<SavedState> CREATOR = new
	 * Parcelable.Creator<SavedState>() { public SavedState
	 * createFromParcel(Parcel in) { return new SavedState(in); }
	 * 
	 * public SavedState[] newArray(int size) { return new SavedState[size]; }
	 * };
	 * 
	 * @Override public int describeContents() { return 0; }
	 * 
	 * }
	 */
}
