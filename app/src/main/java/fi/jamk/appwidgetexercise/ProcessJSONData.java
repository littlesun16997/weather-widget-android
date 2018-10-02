package fi.jamk.appwidgetexercise;

import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// AsyncTask to fetch, process and display weather data
public class ProcessJSONData extends AsyncTask<String, Void, JSONObject> {
    private JSONObject json = null;
    private static String country;

    // define callback listener
    private LoadJSONTaskListener mListener;

    public interface LoadJSONTaskListener {
        void onPostExecuteConcluded(JSONObject result);
    }

    final public void setListener(LoadJSONTaskListener listener) {
        mListener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    ProcessJSONData() {
    }

    // asynctask background thread, load data
    @Override
    final protected JSONObject doInBackground(String... urls) {
        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL(urls[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            bufferedReader.close();
            json = new JSONObject(stringBuilder.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        return json;
    }

    // data is loaded, send it back to caller
    final protected void onPostExecute(JSONObject json) {
        if (mListener != null) mListener.onPostExecuteConcluded(json);
    }

    // Method to get celsius value from kelvin
    public Double getCelsiusFromKelvin(String kelvinString) {
        Double kelvin = Double.parseDouble(kelvinString);
        Double numberToMinus = 273.15;
        Double celsius = kelvin - numberToMinus;
        // Rounding up the double value
        // Each zero (0) return 1 more precision
        // Precision means number of digits after dot
        celsius = (double) Math.round(celsius * 10) / 10;
        return celsius;
    }

    public String getCountryName(String country_code) {
        if (json != null) {
            try {
                JSONArray list = json.getJSONArray("list");

                for (int i = 0; i < list.length(); i++) {
                    JSONObject country_obj = list.getJSONObject(i);
                    if (country_obj.getString("Code").equals(country_code)) {
                        country = country_obj.getString("Name");
                        break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return country;
    }
} // ProcessJSON class end

