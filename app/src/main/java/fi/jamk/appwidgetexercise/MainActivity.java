package fi.jamk.appwidgetexercise;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static String mCity = "";
    private String mURLRoot = "http://api.openweathermap.org/data/2.5/forecast?id=";
    private String mAppID = "&APPID=a5ad2931a6580668b157d3e5fb076999";
    private String iconURL = "http://openweathermap.org/img/w/";

    // AutoCompleteTextView
    private AutoCompleteTextView actv;
    private TextView txt;
    private ImageView img;

    private RemoteViews remoteViews;
    private AppWidgetManager appWidgetManager;
    private ComponentName thisWidget;
    public static SharedPreferences sharedPref;

    // async task to load a new image
    private DownloadImageTask task;

    private ArrayList<City> cities = new ArrayList<>();

    ProcessJSONData countries = new ProcessJSONData();
    String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txt = findViewById(R.id.txt);
        actv = findViewById(R.id.autoComplete);
        img = findViewById(R.id.img);

        loadData();
        countries.execute("http://www.cc.puv.fi/~e1500941/json/country_list.json");
    }

    public void buttonClicked(View view) {
        city = actv.getText().toString();
        getCityId(city);

        Context context = getApplicationContext();
        appWidgetManager = AppWidgetManager.getInstance(context);
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        thisWidget = new ComponentName(context, NewAppWidget.class);

        task = new DownloadImageTask();
        final ProcessJSONData data = new ProcessJSONData();

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
                        // Get weather description and icon
                        JSONArray weather = main.getJSONArray("weather");
                        JSONObject obj_weather = weather.getJSONObject(0);
                        String desc = obj_weather.getString("description");

                        String icon = obj_weather.getString("icon");
                        task.execute(iconURL + icon + ".png");

                        // Get the speed of wind
                        JSONObject wind = main.getJSONObject("wind");
                        String speed = wind.getString("speed");

                        JSONObject city_obj = result.getJSONObject("city");
                        String city_name = city_obj.getString("name");
                        String country_code = city_obj.getString("country");
                        String country = countries.getCountryName(country_code);

                        Double celsius = data.getCelsiusFromKelvin(temperature);
                        temperature = "" + celsius + " " + (char) 0x00B0 + "C";

                        StringBuilder text = new StringBuilder();
                        text.append("Weather of ").append(city_name);

                        if (!country.equals(""))
                            text.append(", ").append(country).append(":\n");

                        text.append("Temperature: ").append(temperature).append("\n");
                        text.append("Humidity: ").append(humidity).append("%\n");
                        text.append("Wind speed: ").append(speed).append("m/s\n");
                        text.append("Description: ").append(desc);
                        txt.setText(text.toString());

                        remoteViews.setTextViewText(R.id.tv_temperature, temperature);
                        remoteViews.setTextViewText(R.id.tv_humidity, "H: " + humidity + "%");
                        remoteViews.setTextViewText(R.id.city, city_name);

                        appWidgetManager.updateAppWidget(thisWidget, remoteViews);

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
    }

    public void loadData() {
        JsonReader reader = new JsonReader(new StringReader(loadJSONFromAsset()));
        Gson gson = new GsonBuilder().create();
        try {
            // Read file in stream mode
            reader.beginArray();
            while (reader.hasNext()) {
                // Read data into object model
                City city = gson.fromJson(reader, City.class);
                cities.add(city);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<String> cityArr = new ArrayList<>();

        for (int i = 0; i < cities.size(); i++) {
            cityArr.add(cities.get(i).getName());
        }

        String[] city_list = cityArr.toArray(new String[cities.size()]);

        // add stings to control
        ArrayAdapter<String> aa = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, city_list);
        actv.setAdapter(aa);

    }

    public String loadJSONFromAsset() {
        String json;
        try {
            InputStream is = getAssets().open("city_list.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private static void handleJsonObject(JsonReader reader) {
        try {
            reader.beginObject();

            while (reader.hasNext()) {
                JsonToken token = reader.peek();

                if (token.equals(JsonToken.BEGIN_ARRAY)) {
                    handleJsonArray(reader);
                } else if (token.equals(JsonToken.END_OBJECT)) {
                    reader.endObject();
                    return;
                } else {
                    String city = "";
                    int id = 0;

                    if (token.equals(JsonToken.NAME)) {
                        token = reader.peek();
                        if (reader.nextName().equals("name")) {
                            city = reader.nextString();
                        }
                    } else if (token.equals(JsonToken.NUMBER)) {
                        token = reader.peek();
                        id = reader.nextInt();
                    } else {
                        reader.skipValue();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleJsonArray(JsonReader reader) throws IOException {
        reader.beginArray();

        while (true) {
            JsonToken token = reader.peek();

            if (token.equals(JsonToken.END_ARRAY)) {
                reader.endArray();
                break;
            } else if (token.equals(JsonToken.BEGIN_OBJECT)) {
                handleJsonObject(reader);
            } else if (token.equals(JsonToken.END_OBJECT)) {
                reader.endObject();
            } else {
                reader.skipValue();
            }
        }
    }

    public void getCityId(String name) {
        for (int i = 0; i < cities.size(); i++) {
            if (cities.get(i).getName().equals(name)) {
                mCity = cities.get(i).getId() + "";
                break;
            }
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

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
            img.setImageBitmap(bitmap);
            remoteViews.setImageViewBitmap(R.id.img_icon, bitmap);
            appWidgetManager.updateAppWidget(thisWidget, remoteViews);
        }
    }

}
