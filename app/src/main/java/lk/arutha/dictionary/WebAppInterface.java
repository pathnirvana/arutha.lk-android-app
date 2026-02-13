package lk.arutha.dictionary;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.webkit.JavascriptInterface;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

public class WebAppInterface {

    private Context mContext;
    private static final String TAG = "WebAppInterface";

    public WebAppInterface(Context c) {
        mContext = c;
    }

    /**
     * This method is exposed to JavaScript as:
     * window.AndroidBackend.executeQuery(dbName, query)
     *
     * @param dbName The filename of the database (e.g., "mydata.db")
     * @param query  The raw SQL query (e.g., "SELECT * FROM users")
     * @return A JSON String containing the results or an error object.
     */
    @JavascriptInterface
    public String executeQuery(String dbName, String query) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        JSONArray resultSet = new JSONArray();

        try {
            // 1. Locate the DB file
            File dbFile = mContext.getDatabasePath(dbName);

            if (!dbFile.exists()) {
                return errorJson("Database file not found: " + dbName);
            }

            // 2. Open Database
            // OPEN_READONLY prevents accidental writes from the WebView
            // OPEN_READWRITE allows INSERT/UPDATE if needed
            db = SQLiteDatabase.openDatabase(
                    dbFile.getPath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY
            );

            // 3. Execute Query
            cursor = db.rawQuery(query, null);

            // 4. Convert Cursor to JSON
            if (cursor.moveToFirst()) {
                do {
                    JSONObject row = new JSONObject();
                    int columnCount = cursor.getColumnCount();

                    for (int i = 0; i < columnCount; i++) {
                        String colName = cursor.getColumnName(i);
                        int type = cursor.getType(i);

                        // Handle SQLite data types dynamically
                        switch (type) {
                            case Cursor.FIELD_TYPE_INTEGER:
                                row.put(colName, cursor.getLong(i));
                                break;
                            case Cursor.FIELD_TYPE_FLOAT:
                                row.put(colName, cursor.getDouble(i));
                                break;
                            case Cursor.FIELD_TYPE_NULL:
                                row.put(colName, JSONObject.NULL);
                                break;
                            case Cursor.FIELD_TYPE_BLOB:
                                // JSON cannot natively hold BLOBs.
                                // We skip them or convert to Base64 if absolutely necessary.
                                // row.put(colName, "[BLOB]");
                                break;
                            case Cursor.FIELD_TYPE_STRING:
                            default:
                                row.put(colName, cursor.getString(i));
                                break;
                        }
                    }
                    resultSet.put(row);
                } while (cursor.moveToNext());
            }

            // Return successful JSON array
            return resultSet.toString();

        } catch (Exception e) {
            Log.e(TAG, "SQL Error", e);
            return errorJson(e.getMessage());
        } finally {
            // 5. Clean up resources
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Helper to return a consistent error JSON format
     */
    private String errorJson(String message) {
        try {
            JSONObject err = new JSONObject();
            err.put("error", message);
            return err.toString();
        } catch (Exception e) {
            return "{\"error\": \"Unknown Error construction failed\"}";
        }
    }

    /**
     * Reads a text file (JSON) from the assets folder.
     * Call from JS: window.AndroidBackend.readAssetFile("path/to/file.json");
     */
    @JavascriptInterface
    public String readAssetFile(String filePath) {
        String json = null;
        try {
            // filePath should be relative to assets, e.g., "sanketha/sanketha.json"
            InputStream is = mContext.getAssets().open(filePath);

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.e("WebAppInterface", "Error reading asset: " + filePath, ex);
            // Return null or specific error JSON if needed
            return null;
        }
        return json;
    }

    @JavascriptInterface
    public void setStatusBarColor(String hexColor, boolean isDarkBackground) {
        // We must run UI updates on the main thread
        if (mContext instanceof Activity) {
            ((Activity) mContext).runOnUiThread(() -> {
                Activity activity = (Activity) mContext;

                // Change the Background Color
                activity.getWindow().setStatusBarColor(android.graphics.Color.parseColor(hexColor));

                // Change the Icon Color (White text for dark background, Black text for light)
                android.view.View decorView = activity.getWindow().getDecorView();
                if (isDarkBackground) {
                    // Clear the flag -> White Icons
                    decorView.setSystemUiVisibility(0);
                } else {
                    // Set the flag -> Black Icons
                    decorView.setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            });
        }
    }
}
