package ru.snoa.celestialbot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import ru.snoa.celestialbot.heavensabove.Pass;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class PassesFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<Pass>> {

	private static final int MENU_SHARING_ID = 100;

	private static final String TAG = "PassesFragment";

	private PassListAdapter adapter;
	private Loader<List<Pass>> loader;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// http://code.google.com/p/android/issues/detail?id=20791
		// setRetainInstance(true);

		setEmptyText(getActivity().getString(R.string.no_data));
		setListShown(false);
		registerForContextMenu(getListView());
		adapter = new PassListAdapter(getActivity());

		loader = getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.add(Menu.NONE, MENU_SHARING_ID, 200, R.string.share);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == MENU_SHARING_ID) {
			final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			final Pass pass = adapter.getItem(info.position);
			if (pass == null) {
				return true;
			}
			final SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd MMM");
			final String text = String.format(getString(R.string.notif_text), pass.name, pass.brightness,
					df.format(pass.date), pass.az, pass.getAzimuth(), pass.alt);
			Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.celestial_bot_event));
			shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
			startActivity(Intent.createChooser(shareIntent, getString(R.string.select_target)));
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// super.onListItemClick(l, v, position, id);
		Bundle bndl = new Bundle();
		Pass pass = adapter.getItem(position);
		bndl.putParcelable("pass", pass);
		CompassFragment cf = new CompassFragment();
		cf.setArguments(bndl);
		cf.show(getActivity().getSupportFragmentManager(), "cf");
	}

	public void reloadPasses() {
		loader.forceLoad();
	}

	public static class PassListAdapter extends ArrayAdapter<Pass> {
		private final LayoutInflater inflater;
		private SimpleDateFormat df;

		public PassListAdapter(Context context) {
			super(context, R.layout.row_layout);
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			df = new SimpleDateFormat("HH:mm:ss dd MMM", Locale.getDefault());
		}

		public void setData(List<Pass> data) {
			clear();
			if (data != null) {
				for (Pass pass : data) {
					add(pass);
				}
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;

			if (convertView == null) {
				view = inflater.inflate(R.layout.row_layout, parent, false);
			} else {
				view = convertView;
			}

			Pass item = getItem(position);
			((TextView) view.findViewById(R.id.name_view)).setText(item.name);
			if (item.name.contentEquals("ISS")) {
				((ImageView) view.findViewById(R.id.icon_view)).setImageResource(R.drawable.iss);
			} else {
				((ImageView) view.findViewById(R.id.icon_view)).setImageResource(R.drawable.satellite);
			}
			((TextView) view.findViewById(R.id.brightness_view)).setText(String.format("%.1f", item.brightness));
			final String az = String.format("%.1f°(%s)", item.az, item.getAzimuth());
			((TextView) view.findViewById(R.id.azimuth_view)).setText(az);
			((TextView) view.findViewById(R.id.altitude_view)).setText(String.format("%.1f°", item.alt));
			((TextView) view.findViewById(R.id.datetime_view)).setText(df.format(item.date));

			return view;
		}
	}

	public static class PassListLoader extends AsyncTaskLoader<List<Pass>> {

		private Context context;

		public PassListLoader(Context context) {
			super(context);
			this.context = context;
		}

		@Override
		protected void onStartLoading() {
			super.onStartLoading();
			Log.d(TAG, "start loading data to fragment");
			forceLoad();
		}

		@Override
		public List<Pass> loadInBackground() {
			return MainApplication.readJson(context);
		}

	}

	@Override
	public Loader<List<Pass>> onCreateLoader(int arg0, Bundle arg1) {
		return new PassListLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<List<Pass>> loader, List<Pass> passes) {
		Log.d(TAG, "set data to adapter");
		adapter.setData(passes);
		adapter.notifyDataSetChanged();
		setListAdapter(adapter);
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<List<Pass>> arg0) {
		adapter.setData(null);
	}

}
