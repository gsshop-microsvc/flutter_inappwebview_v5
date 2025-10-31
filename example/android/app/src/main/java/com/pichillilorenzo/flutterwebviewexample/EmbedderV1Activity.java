package com.microsvc.flutterwebviewexample;

import android.os.Bundle;
import com.microsvc.flutter_inappwebview.InAppWebViewFlutterPlugin;

@SuppressWarnings("deprecation")
public class EmbedderV1Activity extends io.flutter.app.FlutterActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    InAppWebViewFlutterPlugin.registerWith(
            registrarFor("com.microsvc.flutter_inappwebview.InAppWebViewFlutterPlugin"));
  }
}