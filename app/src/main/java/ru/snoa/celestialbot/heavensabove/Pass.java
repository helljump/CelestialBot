package ru.snoa.celestialbot.heavensabove;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.os.Parcel;
import android.os.Parcelable;

public class Pass implements Comparable<Pass>, Parcelable {

    private static final double HALFDEG = 11.25;
    public Date date;
    public double brightness;
    public double alt;
    public double az;
    public String name;

    private static final HashMap<String, Double> azimuthmap = new HashMap<String, Double>() {
        /**
         *
         */
        private static final long serialVersionUID = -4138188505153455801L;

        {
            put("N", 0.0);
            put("NNE", 22.5);
            put("NE", 45.0);
            put("ENE", 67.5);
            put("E", 90.0);
            put("ESE", 112.5);
            put("SE", 135.0);
            put("SSE", 157.5);
            put("S", 180.0);
            put("SSW", 202.5);
            put("SW", 225.0);
            put("WSW", 247.5);
            put("W", 270.0);
            put("WNW", 292.5);
            put("NW", 315.0);
            put("NNW", 337.5);
        }
    };

    public Pass() {
    }

    @Override
    public int compareTo(Pass another) {
        return date.compareTo(another.date);
    }

    public void setAzimuth(String az) {
        this.az = azimuthmap.get(az);
    }

    public String getAzimuth() {
        Double deg;
        Double deg_from;
        Double deg_to;
        Double egg = az + HALFDEG;
        egg = egg > 360 ? egg - 360 : egg;
        for (Map.Entry row : azimuthmap.entrySet()) {
            deg = (Double) row.getValue() + HALFDEG;
            deg_to = deg + HALFDEG;
            deg_from = deg - HALFDEG;
            //Log.d("Pass", "az+" + egg + " (" + deg_from + ".." + deg_to + ")");
            if (egg <= deg_to && egg >= deg_from) {
                return (String) row.getKey();
            }
        }
        return null;
    }

    public Pass(Parcel in) {
        date = new Date(in.readLong());
        brightness = in.readDouble();
        alt = in.readDouble();
        az = in.readDouble();
        name = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(date.getTime());
        dest.writeDouble(brightness);
        dest.writeDouble(alt);
        dest.writeDouble(az);
        dest.writeString(name);
    }

    public static final Parcelable.Creator<Pass> CREATOR = new Parcelable.Creator<Pass>() {
        public Pass createFromParcel(Parcel in) {
            return new Pass(in);
        }

        @Override
        public Pass[] newArray(int size) {
            return new Pass[size];
        }
    };

}
