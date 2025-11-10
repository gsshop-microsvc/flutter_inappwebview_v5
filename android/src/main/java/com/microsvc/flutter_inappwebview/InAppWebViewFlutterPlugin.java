package com.microsvc.flutter_inappwebview;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.webkit.ValueCallback;

import androidx.annotation.Nullable;

import com.microsvc.flutter_inappwebview.chrome_custom_tabs.ChromeSafariBrowserManager;
import com.microsvc.flutter_inappwebview.credential_database.CredentialDatabaseHandler;
import com.microsvc.flutter_inappwebview.in_app_browser.InAppBrowserManager;
import com.microsvc.flutter_inappwebview.headless_in_app_webview.HeadlessInAppWebViewManager;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.platform.PlatformViewRegistry;

public class InAppWebViewFlutterPlugin implements FlutterPlugin, ActivityAware {

  protected static final String LOG_TAG = "InAppWebViewFlutterPL";

  public PlatformUtil platformUtil;
  public InAppBrowserManager inAppBrowserManager;
  public HeadlessInAppWebViewManager headlessInAppWebViewManager;
  public ChromeSafariBrowserManager chromeSafariBrowserManager;
  public InAppWebViewStatic inAppWebViewStatic;
  public MyCookieManager myCookieManager;
  public CredentialDatabaseHandler credentialDatabaseHandler;
  public MyWebStorage myWebStorage;
  public ServiceWorkerManager serviceWorkerManager;
  public WebViewFeatureManager webViewFeatureManager;
  public FlutterWebViewFactory flutterWebViewFactory;
  public static ValueCallback<Uri> filePathCallbackLegacy;
  public static ValueCallback<Uri[]> filePathCallback;

  public Context applicationContext;
  @SuppressWarnings("deprecation")
  @Nullable
  public Object registrar; // PluginRegistry.Registrar for legacy Flutter support
  public BinaryMessenger messenger;
  public FlutterPlugin.FlutterAssets flutterAssets;
  @Nullable
  public ActivityPluginBinding activityPluginBinding;
  @Nullable
  public Activity activity;
  @SuppressWarnings("deprecation")
  public View flutterView;

  public InAppWebViewFlutterPlugin() {}

  @SuppressWarnings("deprecation")
  public static void registerWith(Object registrar) {
    try {
      final InAppWebViewFlutterPlugin instance = new InAppWebViewFlutterPlugin();
      instance.registrar = registrar;
      
      // Use reflection to call methods on PluginRegistry.Registrar for legacy Flutter support
      Class<?> registrarClass = registrar.getClass();
      java.lang.reflect.Method contextMethod = registrarClass.getMethod("context");
      java.lang.reflect.Method messengerMethod = registrarClass.getMethod("messenger");
      java.lang.reflect.Method activityMethod = registrarClass.getMethod("activity");
      java.lang.reflect.Method platformViewRegistryMethod = registrarClass.getMethod("platformViewRegistry");
      java.lang.reflect.Method viewMethod = registrarClass.getMethod("view");
      
      Context context = (Context) contextMethod.invoke(registrar);
      BinaryMessenger messenger = (BinaryMessenger) messengerMethod.invoke(registrar);
      Activity activity = (Activity) activityMethod.invoke(registrar);
      PlatformViewRegistry platformViewRegistry = (PlatformViewRegistry) platformViewRegistryMethod.invoke(registrar);
      View flutterView = (View) viewMethod.invoke(registrar);
      
      instance.onAttachedToEngine(context, messenger, activity, platformViewRegistry, flutterView);
    } catch (Exception e) {
      throw new RuntimeException("Failed to register plugin with legacy Flutter API", e);
    }
  }

  @Override
  public void onAttachedToEngine(FlutterPluginBinding binding) {
    this.flutterAssets = binding.getFlutterAssets();

    // Shared.activity could be null or not.
    // It depends on who is called first between onAttachedToEngine event and onAttachedToActivity event.
    //
    // See https://github.com/pichillilorenzo/flutter_inappwebview/issues/390#issuecomment-647039084
    onAttachedToEngine(
            binding.getApplicationContext(), binding.getBinaryMessenger(), this.activity, binding.getPlatformViewRegistry(), null);
  }

  @SuppressWarnings("deprecation")
  private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger, Activity activity, PlatformViewRegistry platformViewRegistry, View flutterView) {
    this.applicationContext = applicationContext;
    this.activity = activity;
    this.messenger = messenger;
    this.flutterView = flutterView;

    inAppBrowserManager = new InAppBrowserManager(this);
    headlessInAppWebViewManager = new HeadlessInAppWebViewManager(this);
    chromeSafariBrowserManager = new ChromeSafariBrowserManager(this);
    flutterWebViewFactory = new FlutterWebViewFactory(this);
    platformViewRegistry.registerViewFactory(
                    "com.microsvc/flutter_inappwebview_v2", flutterWebViewFactory);

    platformUtil = new PlatformUtil(this);
    inAppWebViewStatic = new InAppWebViewStatic(this);
    myCookieManager = new MyCookieManager(this);
    myWebStorage = new MyWebStorage(this);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      serviceWorkerManager = new ServiceWorkerManager(this);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      credentialDatabaseHandler = new CredentialDatabaseHandler(this);
    }
    webViewFeatureManager = new WebViewFeatureManager(this);
  }

  @Override
  public void onDetachedFromEngine(FlutterPluginBinding binding) {
    if (platformUtil != null) {
      platformUtil.dispose();
      platformUtil = null;
    }
    if (inAppBrowserManager != null) {
      inAppBrowserManager.dispose();
      inAppBrowserManager = null;
    }
    if (headlessInAppWebViewManager != null) {
      headlessInAppWebViewManager.dispose();
      headlessInAppWebViewManager = null;
    }
    if (chromeSafariBrowserManager != null) {
      chromeSafariBrowserManager.dispose();
      chromeSafariBrowserManager = null;
    }
    if (myCookieManager != null) {
      myCookieManager.dispose();
      myCookieManager = null;
    }
    if (myWebStorage != null) {
      myWebStorage.dispose();
      myWebStorage = null;
    }
    if (credentialDatabaseHandler != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      credentialDatabaseHandler.dispose();
      credentialDatabaseHandler = null;
    }
    if (inAppWebViewStatic != null) {
      inAppWebViewStatic.dispose();
      inAppWebViewStatic = null;
    }
    if (serviceWorkerManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      serviceWorkerManager.dispose();
      serviceWorkerManager = null;
    }
    if (webViewFeatureManager != null) {
      webViewFeatureManager.dispose();
      webViewFeatureManager = null;
    }
    filePathCallbackLegacy = null;
    filePathCallback = null;
  }

  @Override
  public void onAttachedToActivity(ActivityPluginBinding activityPluginBinding) {
    this.activityPluginBinding = activityPluginBinding;
    this.activity = activityPluginBinding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    this.activityPluginBinding = null;
    this.activity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(ActivityPluginBinding activityPluginBinding) {
    this.activityPluginBinding = activityPluginBinding;
    this.activity = activityPluginBinding.getActivity();
  }

  @Override
  public void onDetachedFromActivity() {
    this.activityPluginBinding = null;
    this.activity = null;
  }

  @SuppressWarnings("deprecation")
  public void addActivityResultListener(Object listener) {
    if (registrar != null) {
      try {
        // Use reflection to find the method with ActivityResultListener parameter
        java.lang.reflect.Method[] methods = registrar.getClass().getMethods();
        for (java.lang.reflect.Method method : methods) {
          if (method.getName().equals("addActivityResultListener") && method.getParameterTypes().length == 1) {
            method.invoke(registrar, listener);
            return;
          }
        }
      } catch (Exception e) {
        // Ignore if method doesn't exist or invocation fails
      }
    } else if (activityPluginBinding != null) {
      try {
        activityPluginBinding.addActivityResultListener((io.flutter.plugin.common.PluginRegistry.ActivityResultListener) listener);
      } catch (ClassCastException e) {
        // Ignore if listener is not the right type
      }
    }
  }

  @SuppressWarnings("deprecation")
  public String lookupKeyForAsset(String asset) {
    if (registrar != null) {
      try {
        java.lang.reflect.Method method = registrar.getClass().getMethod("lookupKeyForAsset", String.class);
        return (String) method.invoke(registrar, asset);
      } catch (Exception e) {
        // Fallback to modern API
      }
    }
    return flutterAssets != null ? flutterAssets.getAssetFilePathByName(asset) : asset;
  }
}
