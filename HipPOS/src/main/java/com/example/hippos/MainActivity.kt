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
package com.example.hippos

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Checkable
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var wv: WebView? = null
    private var prefs: SharedPreferences? = null
    private var screenWidth = 1024
    private var screenHeight = 768

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView?.setNavigationItemSelectedListener(this)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        wv = findViewById(R.id.myWebView)
        if (wv != null) {
            val ws = wv!!.settings
            ws.javaScriptEnabled = true
            updateWebView()
        }
    }

    private fun updateWebView() {
        val script = "var metatag = document.createElement('meta');" +
                "metatag.name=\"viewport\";" +
                "metatag.setAttribute(\"content\", \"width=" + (if (prefs!!.getBoolean("usedevicewidth", false)) "device-width" else screenWidth.toString()) + ", initial-scale=0\");" +
                "document.getElementsByTagName('head')[0].appendChild(metatag);"
        wv!!.settings.loadWithOverviewMode = prefs!!.getBoolean("overviewmode", false)
        wv!!.settings.useWideViewPort = prefs!!.getBoolean("wideviewport", false)
        wv!!.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.proceed()
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    wv!!.evaluateJavascript(script, null)
                } else {
                    wv!!.loadUrl(
                            "javascript:(function(){$script})();"
                    )
                }
            }
        }
        wv!!.loadUrl(prefs!!.getString("url", getString(R.string.default_url))!!)
        goFullscreen()
        requestedOrientation = if (prefs!!.getBoolean("landscape", false)) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }

    private fun goFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val mDecorView = window.decorView
            mDecorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    override fun onResume() {
        super.onResume()
        goFullscreen()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        wv = findViewById(R.id.myWebView)
        if (prefs != null && !prefs!!.getBoolean("showed_drawer", false)) {
            val dl = findViewById<DrawerLayout>(R.id.drawer_layout)
            if (dl != null) {
                dl.openDrawer(GravityCompat.START)
                prefs!!.edit().putBoolean("showed_drawer", true).apply()
            }
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId
        val format = SimpleDateFormat("yyyy-MM-dd KK:mm:ss a zzz", Locale.US)
        val about = BuildConfig.VERSION_NAME + "<p/>" + getString(R.string.built) + " " + format.format(BuildConfig.buildTime) + "<p/>" +
                getString(R.string.about_hippos_long)
        if (id == R.id.nav_about) {
            val builder = AlertDialog.Builder(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setMessage(Html.fromHtml(about, HtmlCompat.FROM_HTML_MODE_LEGACY))
            } else {
                builder.setMessage(Html.fromHtml(about))
            }
            builder.setTitle(getString(R.string.app_name))
                    .setPositiveButton(R.string.cool) { _: DialogInterface?, _: Int -> }
            val d = builder.create()
            d.show()
            val tv = d.findViewById<TextView>(android.R.id.message)
            if (tv != null) {
                tv.movementMethod = LinkMovementMethod.getInstance()
                tv.textSize = 12f
            }
        } else if (id == R.id.nav_settings) {
            val builder = AlertDialog.Builder(this)
            val v = layoutInflater.inflate(R.layout.settings_main, null)
            val urlText = v.findViewById<EditText>(R.id.urlText)
            val widthText = v.findViewById<EditText>(R.id.screenWidth)
            val heightText = v.findViewById<EditText>(R.id.screenHeight)
            val wideViewPortCheckBox: Checkable = v.findViewById(R.id.wideviewport)
            val overviewModeCheckBox: Checkable = v.findViewById(R.id.overviewmode)
            val usedevicewidth: Checkable = v.findViewById(R.id.usedevicewidth)
            val forceLandscape: Checkable = v.findViewById(R.id.landscape)
            overviewModeCheckBox.isChecked = prefs!!.getBoolean("overviewmode", false)
            wideViewPortCheckBox.isChecked = prefs!!.getBoolean("wideviewport", false)
            usedevicewidth.isChecked = prefs!!.getBoolean("usedevicewidth", false)
            forceLandscape.isChecked = prefs!!.getBoolean("landscape", false)
            urlText.setText(prefs!!.getString("url", getString(R.string.default_url)))
            widthText.setText(prefs!!.getString("width", getString(R.string.default_width)))
            heightText.setText(prefs!!.getString("height", getString(R.string.default_height)))
            builder.setTitle(getString(R.string.set_the_URL))
                    .setView(v)
                    .setPositiveButton("Done") { _: DialogInterface?, _: Int ->
                        wv!!.loadUrl(urlText.text.toString())
                        try {
                            screenWidth = widthText.text.toString().toInt()
                            screenHeight = heightText.text.toString().toInt()
                        } catch (nfe: NumberFormatException) {
                            Log.e(getString(R.string.app_name), "Error!", nfe)
                        }
                        val editor = prefs!!.edit()
                        editor.putString("url", urlText.text.toString())
                        editor.putString("height", heightText.text.toString())
                        editor.putString("width", widthText.text.toString())
                        editor.putBoolean("wideviewport", wideViewPortCheckBox.isChecked)
                        editor.putBoolean("overviewmode", overviewModeCheckBox.isChecked)
                        editor.putBoolean("usedevicewidth", usedevicewidth.isChecked)
                        editor.putBoolean("landscape", forceLandscape.isChecked)
                        editor.apply()
                        updateWebView()
                    }
            val d = builder.create()
            d.show()
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer?.closeDrawer(GravityCompat.START)
        goFullscreen()
        return true
    }
}