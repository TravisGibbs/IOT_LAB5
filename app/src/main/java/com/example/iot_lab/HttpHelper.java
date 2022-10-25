package com.example.iot_lab;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class HttpHelper<T> {
    public interface Callback<A> {
        A execute(String html);
        void finish(A result);
    }

    private static String read(InputStream input, String encoding) {
        try {
            String TAG = "testingiot";
            InputStreamReader reader = new InputStreamReader(input, encoding);
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[4096];
            int len;
            Log.v(TAG, "reading Started");
            len = reader.read(buffer, 0, buffer.length);
            result.append(buffer, 0, len);
            Log.v(TAG, "reading Finished");

            reader.close();
            return result.toString();
        }
        catch (IOException ignored) {
        }
        return null;
    }

    private static String getEncoding(HttpURLConnection conn) {
        String encoding = "utf-8";

        String contentType = conn.getHeaderField("Content-Type").toLowerCase();
        if (contentType.contains("charset=")) {
            int found = contentType.indexOf("charset=");
            encoding = contentType.substring(found + 8, contentType.length()).trim();
        }
        else if (conn.getContentEncoding() != null) {
            encoding = conn.getContentEncoding();
        }
        return encoding;
    }

    @SuppressLint("StaticFieldLeak")
    public String get(final String url, final String message, TextView textView) {

        new AsyncTask<Void, Void, String>() {
            @SuppressLint("StaticFieldLeak")
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String TAG = "testingiot";
                    Log.v(TAG, "Request Started");
                    URI baseUri = new URI(url);
                    String[] params = new String[]{"message", message};
                    URI uri = applyParameters(baseUri, params);
                    HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                    String html = read(conn.getInputStream(), getEncoding(conn));
                    //conn.disconnect();
                    Log.v(TAG, "Task Done");
                    return html;
                }
                catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                String TAG = "testingiot";
                //Log.v(TAG, result);
                textView.setText(result);
            }
        }.execute();
        return "";
    }

    private URI applyParameters(URI baseUri, String[] urlParameters) {
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < urlParameters.length; i += 2) {
            if (first) {
                first = false;
            } else {
                query.append("&");
            }
            try {
                query.append(urlParameters[i]).append("=")
                        .append(URLEncoder.encode(urlParameters[i + 1], "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
        }
        try {
            return new URI(baseUri.getScheme(), baseUri.getAuthority(),
                    baseUri.getPath(), query.toString(), null);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
}