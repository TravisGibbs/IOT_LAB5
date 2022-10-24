package com.example.iot_lab;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
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

/**
 * Helper class that retrieves HTML or text from an HTTP address and lets you specify two event handlers, a method
 * that is called on a separate thread to parse the HTML/text and a method that is called on the UI thread to
 * update the user interface
 * @param <T> The type of data that is the result of the HTML parsing is returned from the parser method to the
 *           UI updated method.
 */
public class HttpHelper<T> {
    /**
     * Specify an implementation for this callback in order to hande code in the background to parse the text
     * in the execute method and then code to handle updating the UI on the UI thread in the finish method.
     * @param <A> The type of data that execute returns and finish uses to update the UI
     */
    public interface Callback<A> {
        A execute(String html);
        void finish(A result);
    }

    /**
     * Read all HTML or text from the input stream using the specified text encoding
     * @param input The stream to read text from
     * @param encoding The encoding of the stream
     * @return All text read from the stream
     */
    private static String readAll(InputStream input, String encoding) {
        try {
            InputStreamReader reader = new InputStreamReader(input, encoding);
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[4096];
            int len;
            while ((len = reader.read(buffer, 0, buffer.length)) > 0) {
                result.append(buffer, 0, len);
            }
            reader.close();
            return result.toString();
        }
        catch (IOException ignored) {
        }
        return null;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    /**
     * Find out and return what type of text encoding is specified by the server
     * @param conn The opened HTTP connection to fetch the encoding for
     * @return The string name of the encoding. utf-8 is the default.
     */
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

    public String get(final String url, final String message, TextView textView) {
        new AsyncTask<Void, Void, String>() {
            @SuppressLint("StaticFieldLeak")
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    URI baseUri = new URI(url);
                    String[] params = new String[]{"message", message};
                    URI uri = applyParameters(baseUri, params);
                    HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                    String html = readAll(conn.getInputStream(), getEncoding(conn));
                    conn.disconnect();
                    return html;
                }
                catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
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
                /* As URLEncoder are always correct, this exception
                 * should never be thrown. */
                throw new RuntimeException(ex);
            }
        }
        try {
            return new URI(baseUri.getScheme(), baseUri.getAuthority(),
                    baseUri.getPath(), query.toString(), null);
        } catch (URISyntaxException ex) {
            /* As baseUri and query are correct, this exception
             * should never be thrown. */
            throw new RuntimeException(ex);
        }
    }
}