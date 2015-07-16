package ru.snoa.celestialbot.heavensabove;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import ru.snoa.celestialbot.R;

public class SiteParser {

    private static final String REFERRER = "http://www.heavens-above.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
    private static final String ISS_URL = "http://www.heavens-above.com/PassSummary.aspx?satid=25544&lat=%f&lng=%f&alt=%f&tz=%s";
    private static final String IRI_URL = "http://www.heavens-above.com/IridiumFlares.aspx?lat=%f&lng=%f&alt=%f&tz=%s";

    private double Lat;
    private double Lng;
    private double Alt;
    private String TZ;
    private Context ctx;

    public SiteParser(Context ctx, double Lat, double Lng, double Alt, String TZ) {
        this.ctx = ctx;
        this.Lat = Lat;
        this.Lng = Lng;
        this.Alt = Alt;
        this.TZ = TZ;
    }

    public ArrayList<Pass> parseISS() throws IOException {

        ArrayList<Pass> passes = new ArrayList<Pass>();
        SimpleDateFormat df = new SimpleDateFormat("dd MMM HH:mm:ss", Locale.ENGLISH);
        String egg;

        Calendar now = GregorianCalendar.getInstance();
        final float offs = getOffset();
        now.add(Calendar.MINUTE, (int) (-offs * 60));
        Calendar tmp = GregorianCalendar.getInstance();

        // http://www.heavens-above.com/PassSummary.aspx?satid=25544&lat=52.631440&lng=39.583500&alt=160.000000&tz=RFTm3
        final String url = String.format(Locale.ENGLISH, ISS_URL, Lat, Lng, Alt, TZ);

        Document doc;
        try {
            doc = Jsoup.connect(url).userAgent(USER_AGENT).referrer(REFERRER).get();

            Elements rows = doc.select("tr.clickableRow");
            Log.d("SiteParser", "iss rows count:" + rows.size());

            for (Element row : rows) {
                final Elements tds = row.select("td");
                Pass pass = new Pass();

                egg = "" + tds.get(0).text() + " " + tds.get(5).text();
                tmp.setTime(df.parse(egg));
                tmp.set(Calendar.YEAR, now.get(Calendar.YEAR));
                if (tmp.before(now)) {
                    tmp.add(Calendar.YEAR, 1);
                }
                pass.date = tmp.getTime();

                egg = tds.get(6).text().replaceAll("\\D", "");
                pass.alt = Float.parseFloat(egg);

                pass.setAzimuth(tds.get(7).text());
                pass.brightness = Float.parseFloat(tds.get(1).text());
                pass.name = "ISS";
                passes.add(pass);
            }

            Log.d("SiteParser", "passes rows count:" + rows.size());
            return passes;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<Pass> parseIridium() throws IOException {

        ArrayList<Pass> passes = new ArrayList<Pass>();
        SimpleDateFormat df = new SimpleDateFormat("MMM dd, HH:mm:ss", Locale.ENGLISH);
        String egg;

        Calendar now = GregorianCalendar.getInstance();
        final float offs = getOffset();
        now.add(Calendar.MINUTE, (int) (-offs * 60));
        Calendar tmp = GregorianCalendar.getInstance();

        // http://www.heavens-above.com/IridiumFlares.aspx?lat=52.631440&lng=39.583500&alt=160.000000&tz=RFTm3
        final String url = String.format(Locale.ENGLISH, IRI_URL, Lat, Lng, Alt, TZ);

        Document doc;
        try {
            doc = Jsoup.connect(url).userAgent(USER_AGENT).referrer(REFERRER).get();

            Elements rows = doc.select("tr.clickableRow");
            Log.d("SiteParser", "iridium rows count:" + rows.size());

            for (Element row : rows) {
                final Elements tds = row.select("td");
                Pass pass = new Pass();

                egg = tds.get(0).text();
                tmp.setTime(df.parse(egg));
                tmp.set(Calendar.YEAR, now.get(Calendar.YEAR));
                if (tmp.before(now)) {
                    tmp.add(Calendar.YEAR, 1);
                }
                pass.date = tmp.getTime();

                pass.brightness = Float.parseFloat(tds.get(1).text());

                egg = tds.get(2).text().replaceAll("\\D", "");
                pass.alt = Float.parseFloat(egg);

                pass.az = Float.parseFloat(tds.get(3).text().replaceAll("\\D", ""));

                pass.name = tds.get(4).text();

                passes.add(pass);
            }

            Log.d("SiteParser", "passes rows count:" + rows.size());
            return passes;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    float getOffset() {
        float rc = 0f;
        final Resources res = ctx.getResources();
        final String[] tz = res.getStringArray(R.array.tz_array);
        final String[] offs = res.getStringArray(R.array.tzValue_array);
        for (int i = 0; i < tz.length; i++) {
            if (tz[i].equals(this.TZ)) {
                final String v = offs[i];
                final Pattern p = Pattern.compile("\\(GMT\\s([\\-+]\\d+)\\).+");
                final Matcher m = p.matcher(v);
                if (m.find()) {
                    Log.d("SiteParser", "match " + m.group(1));
                    rc = Float.parseFloat(m.group(1));
                }
            }
        }
        return rc;
    }
}
