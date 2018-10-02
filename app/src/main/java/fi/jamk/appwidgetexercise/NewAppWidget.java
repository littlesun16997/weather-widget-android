package fi.jamk.appwidgetexercise;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;

/**
 * Implementation of App Widget functionality.
 */
public class NewAppWidget extends AppWidgetProvider {

    // Construct the url to fetch weather JSON data from web
    private String mCity = MainActivity.mCity;
    private String mURLRoot = "http://api.openweathermap.org/data/2.5/forecast?id=";
    private String mAppID = "&APPID=a5ad2931a6580668b157d3e5fb076999";

    private String iconURL = "http://openweathermap.org/img/w/";

    private static RemoteViews remoteViews;
    private static AppWidgetManager appWidgetManager;
    private static ComponentName watchWidget;
    private SharedPreferences sharedPref = MainActivity.sharedPref;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ComponentName watchWidget = new ComponentName(context, NewAppWidget.class);
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String name = sharedPref.getString("cityId", "1581130");
        if(!name.equalsIgnoreCase(""))
        {
            mCity = name;
        }

        Intent intent = new Intent(context, NewAppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        appWidgetManager.updateAppWidget(watchWidget, remoteViews);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        // Allow the network operation on main thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        super.onReceive(context, intent);

        appWidgetManager = AppWidgetManager.getInstance(context);
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        watchWidget = new ComponentName(context, NewAppWidget.class);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String name = sharedPref.getString("cityId", "1581130");
        if(!name.equalsIgnoreCase(""))
        {
            mCity = name;
        }

        // Check the internet connection availability
        if (isInternetConnected()) {
            // Update the widget weather data
            // Execute the AsyncTask
            final ProcessJSONData data = new ProcessJSONData();
            final DownloadImageTask task = new DownloadImageTask();

            data.setListener(new ProcessJSONData.LoadJSONTaskListener() {
                @Override
                public void onPostExecuteConcluded(JSONObject result) {

                    if (result != null) {
                        try {
                            JSONArray list = result.getJSONArray("list");
                            JSONObject main = list.getJSONObject(0);
                            JSONObject obj = main.getJSONObject("main");

                            // Get the value of key "temp" under JSONObject "main"
                            String temperature = obj.getString("temp");
                            // Get the value of key "humidity" under JSONObject "main"
                            String humidity = obj.getString("humidity");

                            JSONObject city = result.getJSONObject("city");
                            String city_name = city.getString("name");

                            Double celsius = data.getCelsiusFromKelvin(temperature);
                            temperature = "" + celsius + " " + (char) 0x00B0 + "C";

                            JSONArray weather = main.getJSONArray("weather");
                            JSONObject obj_weather = weather.getJSONObject(0);
                            String icon = obj_weather.getString("icon");
                            task.execute(iconURL + icon + ".png");

                            // Display weather data on widget
                            remoteViews.setTextViewText(R.id.tv_temperature, temperature);
                            remoteViews.setTextViewText(R.id.tv_humidity, "H: " + humidity + "%");
                            remoteViews.setTextViewText(R.id.city, city_name);

                            // Apply the changes
                            appWidgetManager.updateAppWidget(watchWidget, remoteViews);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                }
            });
            data.execute(mURLRoot + mCity + mAppID);

            sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("cityId", mCity);
            editor.apply();
        } else
            Toast.makeText(context, "No Internet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    // Custom method to check internet connection
    public Boolean isInternetConnected() {
        boolean status = false;
        try {
            InetAddress address = InetAddress.getByName("google.com");

            if (address != null) {
                status = true;
            }
        } catch (Exception e) // Catch the exception
        {
            e.printStackTrace();
        }
        return status;
    }

    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        // this is done in UI thread, nothing this time
        @Override
        protected void onPreExecute() {
        }

        // this is background thread, load image and pass it to onPostExecute
        @Override
        protected Bitmap doInBackground(String... urls) {
            URL imageUrl;
            Bitmap bitmap = null;

            try {
                imageUrl = new URL(urls[0]);
                InputStream in = imageUrl.openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        // this is done in UI thread
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            remoteViews.setImageViewBitmap(R.id.img_icon, bitmap);
            appWidgetManager.updateAppWidget(watchWidget, remoteViews);
        }
    }
}

