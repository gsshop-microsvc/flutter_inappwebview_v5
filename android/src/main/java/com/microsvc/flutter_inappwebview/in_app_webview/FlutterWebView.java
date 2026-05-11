package com.microsvc.flutter_inappwebview.in_app_webview;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.microsvc.flutter_inappwebview.InAppWebViewFlutterPlugin;
import com.microsvc.flutter_inappwebview.InAppWebViewMethodHandler;
import com.microsvc.flutter_inappwebview.plugin_scripts_js.JavaScriptBridgeJS;
import com.microsvc.flutter_inappwebview.pull_to_refresh.PullToRefreshLayout;
import com.microsvc.flutter_inappwebview.pull_to_refresh.PullToRefreshOptions;
import com.microsvc.flutter_inappwebview.types.PlatformWebView;
import com.microsvc.flutter_inappwebview.types.URLRequest;
import com.microsvc.flutter_inappwebview.types.UserScript;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

class WebViewManager {
    static Map<Integer, Pair<InAppWebView, PullToRefreshLayout>> persistedWebViewMap = new HashMap<>();
    static Map<Integer, Boolean> persistedWebViewInitialLoadedMap = new HashMap<>();
    static Map<Integer, MethodChannel> persistedMethodChannel = new HashMap<>();
    static Map<Integer, MethodChannel> persistedSubMethodChannel = new HashMap<>();
}

public class FlutterWebView implements PlatformWebView {

    static final String LOG_TAG = "IAWFlutterWebView";

    public InAppWebView webView;
//    public final MethodChannel channel;
//    public MethodChannel subChannel;
    public InAppWebViewMethodHandler methodCallDelegate;
    Integer persistedId;
    public PullToRefreshLayout pullToRefreshLayout;


    public FlutterWebView(final InAppWebViewFlutterPlugin plugin, final Context context, Object id,
                          HashMap<String, Object> params) {
        DisplayListenerProxy displayListenerProxy = new DisplayListenerProxy();
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        displayListenerProxy.onPreWebViewInitialization(displayManager);

        Map<String, Object> initialOptions = (Map<String, Object>) params.get("initialOptions");
        Map<String, Object> contextMenu = (Map<String, Object>) params.get("contextMenu");

        persistedId = (Integer) params.get("persistedId");
        Integer windowId = (Integer) params.get("windowId");

        List<Map<String, Object>> initialUserScripts = (List<Map<String, Object>>) params.get("initialUserScripts");
        Map<String, Object> pullToRefreshInitialOptions = (Map<String, Object>) params.get("pullToRefreshOptions");

        InAppWebViewOptions options = new InAppWebViewOptions();
        options.parse(initialOptions);

        List<UserScript> userScripts = new ArrayList<>();
        if (initialUserScripts != null) {
            for (Map<String, Object> initialUserScript : initialUserScripts) {
                userScripts.add(UserScript.fromMap(initialUserScript));
            }
        }

        Pair<InAppWebView, PullToRefreshLayout> pairsView
            = WebViewManager.persistedWebViewMap.get(persistedId);

        if (pairsView != null) {
            System.out.println("[keykat] not null persistedId: " + persistedId);

            return;
        }
        

            
        MethodChannel channel = new MethodChannel(plugin.messenger, "com.microsvc/flutter_inappwebview_v2_" + persistedId);
        MethodChannel subChannel = new MethodChannel(plugin.messenger, "com.microsvc/flutter_inappwebview_v2_sub_" + persistedId);
        webView = new InAppWebView(context, plugin, channel, persistedId, windowId, options, contextMenu, options.useHybridComposition ? null : plugin.flutterView, userScripts);
        displayListenerProxy.onPostWebViewInitialization(displayManager);

        if (options.useHybridComposition) { 
            // set MATCH_PARENT layout params to the WebView, otherwise it won't take all the available space!
            webView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            MethodChannel pullToRefreshLayoutChannel = new MethodChannel(plugin.messenger, "com.microsvc/flutter_inappwebview_v2_pull_to_refresh_" + persistedId);
            PullToRefreshOptions pullToRefreshOptions = new PullToRefreshOptions();
            pullToRefreshOptions.parse(pullToRefreshInitialOptions);
            pullToRefreshLayout = new PullToRefreshLayout(context, pullToRefreshLayoutChannel, pullToRefreshOptions);
            pullToRefreshLayout.addView(webView);
            pullToRefreshLayout.prepare();
        }

        methodCallDelegate = new InAppWebViewMethodHandler(webView);
        channel.setMethodCallHandler(methodCallDelegate);
        subChannel.setMethodCallHandler(new MethodChannel.MethodCallHandler() {
            @Override
            public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
                if (call.method.equals("persistedDispose")) {
                    try {
                        persistedDispose(persistedId);
                        result.success(true);
                    } catch (Exception e) {
                        result.error("11486", e.toString(), e.getMessage());
                    }
                }
            }
        });

        webView.prepare();

        if (persistedId != null) {
            WebViewManager.persistedWebViewMap.put(persistedId, new Pair<>(webView, pullToRefreshLayout));
            WebViewManager.persistedMethodChannel.put(persistedId, channel);
            WebViewManager.persistedSubMethodChannel.put(persistedId, subChannel);
            WebViewManager.persistedWebViewInitialLoadedMap.put(persistedId, false);
        }
    }

    @Override
    public View getView() {
        Pair<InAppWebView, PullToRefreshLayout> pairsView
                = WebViewManager.persistedWebViewMap.get(persistedId);

        if (pairsView == null) {
            return null;
        }

        InAppWebView webView = pairsView.first;
        PullToRefreshLayout pullToRefreshLayout = pairsView.second;

        return pullToRefreshLayout != null ? pullToRefreshLayout : webView;
    }

    public void makeInitialLoad(HashMap<String, Object> params) {
        Pair<InAppWebView, PullToRefreshLayout> pairsView
                = WebViewManager.persistedWebViewMap.get(persistedId);

        if (pairsView == null) {
            return;
        }

        InAppWebView webView = pairsView.first;

        if (Boolean.TRUE.equals(WebViewManager.persistedWebViewInitialLoadedMap.get(persistedId))) {
            return;
        }

        WebViewManager.persistedWebViewInitialLoadedMap.put(persistedId, true);
        Integer windowId = (Integer) params.get("windowId");
        Map<String, Object> initialUrlRequest = (Map<String, Object>) params.get("initialUrlRequest");
        final String initialFile = (String) params.get("initialFile");
        final Map<String, String> initialData = (Map<String, String>) params.get("initialData");

        if (windowId != null) {
            Message resultMsg = InAppWebViewChromeClient.windowWebViewMessages.get(windowId);
            if (resultMsg != null) {
                ((WebView.WebViewTransport) resultMsg.obj).setWebView(webView);
                resultMsg.sendToTarget();
            }
        } else {
            if (initialFile != null) {
                try {
                    webView.loadFile(initialFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, initialFile + " asset file cannot be found!", e);
                }
            } else if (initialData != null) {
                String data = initialData.get("data");
                String mimeType = initialData.get("mimeType");
                String encoding = initialData.get("encoding");
                String baseUrl = initialData.get("baseUrl");
                String historyUrl = initialData.get("historyUrl");
                webView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
            } else if (initialUrlRequest != null) {
                URLRequest urlRequest = URLRequest.fromMap(initialUrlRequest);
                webView.loadUrl(urlRequest);
            }
        }
    }

    public void persistedDispose(Integer persistedId) {
        Pair<InAppWebView, PullToRefreshLayout> pairsView
                = WebViewManager.persistedWebViewMap.get(persistedId);

        MethodChannel channel = WebViewManager.persistedMethodChannel.get(persistedId);
        MethodChannel subChannel = WebViewManager.persistedSubMethodChannel.get(persistedId);

        if (channel != null) {
            channel.setMethodCallHandler(null);
        }
        if (subChannel != null) {
            subChannel.setMethodCallHandler(null);
        }
        if (methodCallDelegate != null) {
            methodCallDelegate.dispose();
            methodCallDelegate = null;
        }
        if (pairsView == null) {
            cleanupPersistedState(persistedId);
            this.persistedId = null;
            return;
        }

        final InAppWebView webView = pairsView.first;
        final PullToRefreshLayout pullToRefreshLayout = pairsView.second;
        if (webView != null) {
            webView.removeJavascriptInterface(JavaScriptBridgeJS.JAVASCRIPT_BRIDGE_NAME);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && WebViewFeature.isFeatureSupported(WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE)) {
                WebViewCompat.setWebViewRenderProcessClient(webView, null);
            }
            webView.setWebChromeClient(new WebChromeClient());
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    if (webView.inAppWebViewRenderProcessClient != null) {
                        webView.inAppWebViewRenderProcessClient.dispose();
                    }
                    webView.inAppWebViewChromeClient.dispose();
                    webView.inAppWebViewClient.dispose();
                    webView.javaScriptBridgeInterface.dispose();
                    webView.dispose();
                    webView.destroy();
//                    webView = null;

                    if (pullToRefreshLayout != null) {
                        pullToRefreshLayout.dispose();
//                        pullToRefreshLayout = null;
                    }
                }
            });
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(false);
            webView.loadUrl("about:blank");
        }

        cleanupPersistedState(persistedId);

        this.persistedId = null;
    }

    private void cleanupPersistedState(Integer persistedId) {
        WebViewManager.persistedWebViewMap.put(persistedId, null);
        WebViewManager.persistedWebViewInitialLoadedMap.put(persistedId, false);
        WebViewManager.persistedMethodChannel.put(persistedId, null);
        WebViewManager.persistedSubMethodChannel.put(persistedId, null);
    }

    @Override
    public void dispose() {

    }

    @Override
    public void onInputConnectionLocked() {
        if (!shouldUseAndroid9And10InputWorkaround()) {
            return;
        }
        Pair<InAppWebView, PullToRefreshLayout> pairsView =
                WebViewManager.persistedWebViewMap.get(persistedId);
        if (pairsView == null) {
            return;
        }
        final InAppWebView webView = pairsView.first;
        if (webView != null && webView.inAppBrowserDelegate == null && !webView.options.useHybridComposition) {
            webView.lockInputConnection();
        }
    }

    @Override
    public void onInputConnectionUnlocked() {
        if (!shouldUseAndroid9And10InputWorkaround()) {
            return;
        }
        Pair<InAppWebView, PullToRefreshLayout> pairsView =
                WebViewManager.persistedWebViewMap.get(persistedId);
        if (pairsView == null) {
            return;
        }
        final InAppWebView webView = pairsView.first;
        if (webView != null && webView.inAppBrowserDelegate == null && !webView.options.useHybridComposition) {
            webView.unlockInputConnection();
            webView.reconnectInputConnectionAfterUnlock("platformView:inputUnlocked");
        }
    }

    @Override
    public void onFlutterViewAttached(@NonNull View flutterView) {
        if (!shouldUseAndroid9And10InputWorkaround()) {
            return;
        }
        Pair<InAppWebView, PullToRefreshLayout> pairsView =
                WebViewManager.persistedWebViewMap.get(persistedId);
        if (pairsView == null) {
            return;
        }
        final InAppWebView webView = pairsView.first;
        if (webView != null && !webView.options.useHybridComposition) {
            webView.setContainerView(flutterView);
            webView.reconnectInputConnectionAfterUnlock("platformView:flutterViewAttached");
        }
    }

    @Override
    public void onFlutterViewDetached() {
        if (!shouldUseAndroid9And10InputWorkaround()) {
            return;
        }
        Pair<InAppWebView, PullToRefreshLayout> pairsView =
                WebViewManager.persistedWebViewMap.get(persistedId);
        if (pairsView == null) {
            return;
        }
        final InAppWebView webView = pairsView.first;
        if (webView != null && !webView.options.useHybridComposition) {
            webView.setContainerView(null);
        }
    }

    private boolean shouldUseAndroid9And10InputWorkaround() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q;
    }
}