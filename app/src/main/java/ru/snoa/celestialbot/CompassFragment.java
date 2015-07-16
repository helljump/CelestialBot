package ru.snoa.celestialbot;

import ru.snoa.celestialbot.heavensabove.Pass;

import android.content.Context;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class CompassFragment extends DialogFragment implements SensorEventListener {

    private static final String TAG = "CompassFragment";

    private ImageView compassView;
    private ImageView arrowView;
    private Pass pass;
    private TextView timeleftView;

    private Handler timerHandler = new Handler();
    private SensorManager sensorService;

    private Sensor sensor;

    private int mOrientation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pass = getArguments().getParcelable("pass");
        sensorService = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.compass_layout, null);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        compassView = (ImageView) getView().findViewById(R.id.compass_iv);
        arrowView = (ImageView) getView().findViewById(R.id.arrow_iv);
        TextView brightnessView = (TextView) getView().findViewById(R.id.brightness_tv);
        timeleftView = (TextView) getView().findViewById(R.id.timeleft_tv);
        if (sensor != null) {
            sensorService.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        }
        getDialog().setTitle(pass.name);
        brightnessView.setText(String.format("%.1f", pass.brightness));
        timerTask.run();
        mOrientation = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getOrientation();
    }

    Runnable timerTask = new Runnable() {
        public void run() {
            long ct = pass.date.getTime() - System.currentTimeMillis();
            boolean passed = ct < 0;
            ct = ct / 1000;
            long h = ct / 3600;
            ct = ct - h * 3600;
            long m = ct / 60;
            ct = ct - m * 60;
            long s = ct;
            if (timeleftView != null) {
                if (!passed) {
                    timeleftView.setText(String.format(getActivity().getString(R.string.compass_time), h, m, s));
                } else {
                    timeleftView.setText(R.string.old_event);
                }
                timerHandler.postDelayed(timerTask, 1000);
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if (sensor != null) {
            sensorService.unregisterListener(this);
        }
        timerHandler.removeCallbacks(timerTask);
    }

    public float[] lowPass(float[] input, float[] output) {
        if (output == null)
            return input;

        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + 0.2f * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onAccuracyChanged(Sensor event, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        // Log.d(TAG, "screen orient:" + mOrientation);
        float azimuth = -event.values[0] - mOrientation * 90;
        final Matrix matrix = new Matrix();
        final float mW = (float) compassView.getDrawable().getMinimumWidth() / 2;
        final float mH = (float) compassView.getDrawable().getMinimumHeight() / 2;
        matrix.setRotate(azimuth, mW, mH);
        compassView.setImageMatrix(matrix);
        matrix.setRotate((float) (azimuth + pass.az), mW, mH);
        arrowView.setImageMatrix(matrix);
    }

}
