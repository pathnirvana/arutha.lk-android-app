package lk.arutha.dictionary;

import android.annotation.SuppressLint;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private WebView myWebView;
    private LinearLayout loadingLayout;

    // Preference Keys
    private static final String PREFS_NAME = "AppConfiguration";
    private static final String KEY_LAST_VERSION_CODE = "last_run_version_code";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Views
        myWebView = findViewById(R.id.webview);
        loadingLayout = findViewById(R.id.loading_layout);

        // 2. Configure WebView (Standard setup)
        setupWebView();

        // 3. Logic: Check App Version vs Last Run Version
        checkVersionAndLoad();

        // 4. Handle Back Button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (myWebView.canGoBack()) {
                    myWebView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // Inject the Java Bridge
        myWebView.addJavascriptInterface(new WebAppInterface(this), "AndroidBackend");
    }

    private void checkVersionAndLoad() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Get the version of the app currently running
        int currentAppVersion = BuildConfig.VERSION_CODE;

        // Get the version code stored from the last time we successfully copied DBs
        // Returns -1 if the app has never run before
        int lastSavedVersion = prefs.getInt(KEY_LAST_VERSION_CODE, -1);

        // LOGIC:
        // If current version != last saved version, it means either:
        // A) It's a fresh install (-1 != 1)
        // B) It's an update (e.g., 1 != 2)
        if (currentAppVersion != lastSavedVersion) {
            Log.d("MainActivity", "App Update or Fresh Install Detected. Copying DBs...");
            startDatabaseCopy(prefs, currentAppVersion);
        } else {
            Log.d("MainActivity", "Version match. Skipping DB copy.");
            launchVueApp();
        }
    }

    private void startDatabaseCopy(SharedPreferences prefs, int currentVersion) {
        // Show Loading Spinner, Hide WebView
        loadingLayout.setVisibility(View.VISIBLE);
        myWebView.setVisibility(View.GONE);

        // Run file operations in background thread
        new Thread(() -> {
            try {
                // 1. Perform the heavy lifting (Copy files from assets to internal storage)
                // This will overwrite existing files with the new ones from the updated APK
                DatabaseAssetHelper.copyAllDatabases(this);

                // 2. Save the new version code so we don't do this again until next update
                prefs.edit().putInt(KEY_LAST_VERSION_CODE, currentVersion).apply();

                // 3. Switch back to UI Thread to load the App
                runOnUiThread(this::launchVueApp);

            } catch (Exception e) {
                Log.e("MainActivity", "Error copying database", e);
                // Optional: You could show a Dialog here if copy fails
            }
        }).start();
    }

    private void launchVueApp() {
        // Ensure the loading screen is gone
        if (loadingLayout.getVisibility() == View.VISIBLE) {
            loadingLayout.setVisibility(View.GONE);
            myWebView.setVisibility(View.VISIBLE);
        }

        // Load the index.html
        if (myWebView.getUrl() == null) {
            myWebView.loadUrl("file:///android_asset/dist/index.html");
        }
    }


}
