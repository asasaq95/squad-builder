package com.abdullah.squadbuilder;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private static final int PICK_FILE = 1;
    private WebView web;
    private ValueCallback<Uri[]> pendingChooser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);   // persistent storage for the squads
        s.setAllowFileAccess(true);

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb,
                                             FileChooserParams params) {
                if (pendingChooser != null) pendingChooser.onReceiveValue(null);
                pendingChooser = cb;
                try {
                    startActivityForResult(params.createIntent(), PICK_FILE);
                } catch (Exception e) {
                    pendingChooser = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FILE && pendingChooser != null) {
            pendingChooser.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            pendingChooser = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    private class Bridge {
        @JavascriptInterface
        public void exportData(String json) {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/json");
            send.putExtra(Intent.EXTRA_SUBJECT, "squads.json");
            send.putExtra(Intent.EXTRA_TEXT, json);
            startActivity(Intent.createChooser(send, "Save squads backup"));
        }

        @JavascriptInterface
        public void printPage() {
            runOnUiThread(() -> {
                PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                pm.print("Squad Builder",
                        web.createPrintDocumentAdapter("Squad Builder"),
                        new PrintAttributes.Builder().build());
            });
        }
    }
}
