package com.pichillilorenzo.flutter_inappwebview_v2.in_app_browser;

import android.content.Intent;

public interface ActivityResultListener {
  /** @return true if the result has been handled. */
  boolean onActivityResult(int requestCode, int resultCode, Intent data);
}
