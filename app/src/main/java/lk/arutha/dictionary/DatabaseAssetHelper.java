package lk.arutha.dictionary;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseAssetHelper {

    // The folder name inside 'src/main/assets/' where you put your .db files
    private static final String ASSET_DB_FOLDER = "server-data";
    private static final String TAG = "DatabaseAssetHelper";

    /**
     * Iterates through the 'databases' folder in assets and copies
     * ONLY files ending in .db to the device's internal database directory.
     */
    public static void copyAllDatabases(Context context) {
        try {
            // 1. Get list of all files in assets/databases
            String[] files = context.getAssets().list(ASSET_DB_FOLDER);

            if (files == null || files.length == 0) {
                Log.w(TAG, "No files found in assets/" + ASSET_DB_FOLDER);
                return;
            }

            // 2. Ensure the internal databases directory exists
            File internalDbDir = new File(context.getApplicationInfo().dataDir + "/databases");
            if (!internalDbDir.exists()) {
                internalDbDir.mkdirs();
            }

            // 3. Loop through files and copy ONLY .db files
            for (String filename : files) {

                // --- FILTER ADDED HERE ---
                if (!filename.endsWith(".db")) {
                    Log.d(TAG, "Skipping non-db file: " + filename);
                    continue; // Skip to the next file
                }

                File outFile = new File(internalDbDir, filename);

                Log.d(TAG, "Copying database: " + filename);
                copyFile(context, filename, outFile);
            }

        } catch (IOException e) {
            Log.e(TAG, "Failed to list or copy databases", e);
            throw new RuntimeException("Database copy failed", e);
        }
    }

    private static void copyFile(Context context, String filename, File outFile) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = context.getAssets().open(ASSET_DB_FOLDER + "/" + filename);
            out = new FileOutputStream(outFile);

            byte[] buffer = new byte[4096]; // 4KB buffer
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }
}
