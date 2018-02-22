/*  This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.example.hippos;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private WebView wv;
    private SharedPreferences prefs;
    private int screenWidth = 1024;
    private int screenHeight = 768;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        wv = findViewById(R.id.myWebView);
        if (wv != null) {
            WebSettings ws = wv.getSettings();
            ws.setJavaScriptEnabled(true);
            updateWebView();
        }
    }

    private void updateWebView() {
        final String script =
                "var metatag = document.createElement('meta');" +
                        "metatag.name=\"viewport\";" +
                        "metatag.setAttribute(\"content\", \"width=" + (prefs.getBoolean("usedevicewidth", false) ? "device-width" : Integer.toString(screenWidth)) + ", initial-scale=0\");" +
                        "document.getElementsByTagName('head')[0].appendChild(metatag);";

        wv.getSettings().setLoadWithOverviewMode(prefs.getBoolean("overviewmode", false));
        wv.getSettings().setUseWideViewPort(prefs.getBoolean("wideviewport", false));
        wv.setWebViewClient(new WebViewClient() {

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    wv.evaluateJavascript(script, null);
                } else {
                    wv.loadUrl(
                            "javascript:(function(){" + script + "})();"
                    );
                }
            }
        });
        wv.loadUrl(prefs.getString("url", getString(R.string.default_url)));
        goFullscreen();
        setRequestedOrientation(prefs.getBoolean("landscape", false)
                ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    private void goFullscreen() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View mDecorView = getWindow().getDecorView();
            if (mDecorView != null) {
                mDecorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        goFullscreen();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        wv = findViewById(R.id.myWebView);
        if ((prefs != null) && (!prefs.getBoolean("showed_drawer", false))) {
            DrawerLayout dl = findViewById(R.id.drawer_layout);
            if (dl != null) {
                dl.openDrawer(GravityCompat.START);
                prefs.edit().putBoolean("showed_drawer", true).apply();
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd KK:mm:ss a zzz", Locale.US);
        String about = BuildConfig.VERSION_NAME.concat("<p/>").concat(getString(R.string.built)).concat(" ").concat(format.format(BuildConfig.buildTime)).concat("<p/>").concat(
                getString(R.string.about_hippos_long));
        if (id == R.id.nav_about) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.app_name))
                    .setMessage(Html.fromHtml(about))
                    .setPositiveButton(R.string.cool, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            AlertDialog d = builder.create();
            d.show();
            TextView tv = d.findViewById(android.R.id.message);
            if (tv != null){
                tv.setMovementMethod(LinkMovementMethod.getInstance());
                tv.setTextSize(12f);
            }
        } else if (id == R.id.nav_settings) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View v = getLayoutInflater().inflate(R.layout.settings_main, null);
            final EditText urlText = v.findViewById(R.id.urlText);
            final EditText widthText = v.findViewById(R.id.screenWidth);
            final EditText heightText = v.findViewById(R.id.screenHeight);
            final Checkable wideViewPortCheckBox = (CheckBox) v.findViewById(R.id.wideviewport);
            final Checkable overviewModeCheckBox = (CheckBox) v.findViewById(R.id.overviewmode);
            final Checkable usedevicewidth = (CheckBox) v.findViewById(R.id.usedevicewidth);
            final Checkable forceLandscape = (CheckBox) v.findViewById(R.id.landscape);


            overviewModeCheckBox.setChecked(prefs.getBoolean("overviewmode", false));
            wideViewPortCheckBox.setChecked(prefs.getBoolean("wideviewport", false));
            usedevicewidth.setChecked(prefs.getBoolean("usedevicewidth", false));
            forceLandscape.setChecked(prefs.getBoolean("landscape", false));

            urlText.setText(prefs.getString("url", getString(R.string.default_url)));
            widthText.setText(prefs.getString("width", getString(R.string.default_width)));
            heightText.setText(prefs.getString("height", getString(R.string.default_height)));

            builder.setTitle(getString(R.string.set_the_URL))
                    .setView(v)
                    .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            wv.loadUrl(urlText.getText().toString());
                            try {
                                screenWidth = Integer.parseInt(widthText.getText().toString());
                                screenHeight = Integer.parseInt(heightText.getText().toString());
                            } catch (java.lang.NumberFormatException nfe) {
                                Log.e(getString(R.string.app_name), "Error!", nfe);
                            }
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("url", urlText.getText().toString());
                            editor.putString("height", heightText.getText().toString());
                            editor.putString("width", widthText.getText().toString());
                            editor.putBoolean("wideviewport", wideViewPortCheckBox.isChecked());
                            editor.putBoolean("overviewmode", overviewModeCheckBox.isChecked());
                            editor.putBoolean("usedevicewidth", usedevicewidth.isChecked());
                            editor.putBoolean("landscape", forceLandscape.isChecked());

                            editor.apply();
                            updateWebView();
                        }
                    });
            AlertDialog d = builder.create();
            d.show();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }
        goFullscreen();
        return true;
    }
}
