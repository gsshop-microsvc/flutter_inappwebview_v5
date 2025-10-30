package com.pichillilorenzo.flutterwebviewexample;

import android.os.Bundle;
import com.pichillilorenzo.flutter_inappwebview_v2.InAppWebViewFlutterPluginV2;

@SuppressWarnings("deprecation")
public class EmbedderV1Activity extends io.flutter.app.FlutterActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    InAppWebViewFlutterPluginV2.registerWith(
            registrarFor("com.pichillilorenzo.flutter_inappwebview_v2.InAppWebViewFlutterPluginV2"));
  }
}