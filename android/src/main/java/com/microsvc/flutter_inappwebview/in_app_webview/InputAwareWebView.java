package com.microsvc.flutter_inappwebview.in_app_webview;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ListPopupWindow;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A WebView subclass that mirrors the same implementation hacks that the system WebView does in
 * order to correctly create an InputConnection.
 *
 * These hacks are only needed in Android versions below N and exist to create an InputConnection
 * on the WebView's dedicated input, or IME, thread. The majority of this proxying logic is in
 * https://github.com/flutter/plugins/blob/master/packages/webview_flutter/android/src/main/java/io/flutter/plugins/webviewflutter/InputAwareWebView.java
 */
public class InputAwareWebView extends WebView {
  private static final String LOG_TAG = "InputAwareWebView";
  private static final long RECONNECT_INPUT_DELAY_MS = 120L;
  private static final long AUTO_RECONNECT_INITIAL_DELAY_MS = 80L;
  private static final int MAX_RECONNECT_INPUT_RETRIES = 2;
  @Nullable
  public View containerView;
  private View threadedInputConnectionProxyView;
  private ThreadedInputConnectionProxyAdapterView proxyAdapterView;
  private boolean useHybridComposition = false;

  public InputAwareWebView(Context context, @Nullable View containerView, Boolean useHybridComposition) {
    super(context);
    this.containerView = containerView;
    this.useHybridComposition = useHybridComposition == null ? false : useHybridComposition;
  }

  public InputAwareWebView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.containerView = null;
  }

  public InputAwareWebView(Context context) {
    super(context);
    this.containerView = null;
  }

  public InputAwareWebView(Context context, AttributeSet attrs, int defaultStyle) {
    super(context, attrs, defaultStyle);
    this.containerView = null;
  }

  public void setContainerView(View containerView) {
    View previousContainerView = this.containerView;
    this.containerView = containerView;

    if (proxyAdapterView == null) {
      return;
    }

    Log.w(LOG_TAG, "The containerView has changed while the proxyAdapterView exists.");
    if (containerView != null) {
      // On some Android 9/10 route transitions, Flutter reattaches with a different container view.
      // Recreate proxyAdapterView so it uses the latest window token/handler chain.
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
              && previousContainerView != null
              && previousContainerView != containerView
              && threadedInputConnectionProxyView != null
              && threadedInputConnectionProxyView.getHandler() != null) {
        proxyAdapterView =
                new ThreadedInputConnectionProxyAdapterView(
                        /*containerView=*/ containerView,
                        /*targetView=*/ threadedInputConnectionProxyView,
                        /*imeHandler=*/ threadedInputConnectionProxyView.getHandler());
        logReconnectDebug("recreated proxyAdapterView after containerView swap");
      }
      setInputConnectionTarget(proxyAdapterView);
    }
  }

  /**
   * Set our proxy adapter view to use its cached input connection instead of creating new ones.
   *
   * <p>This is used to avoid losing our input connection when the virtual display is resized.
   */
  public void lockInputConnection() {
    if (proxyAdapterView == null) {
      return;
    }

    // API 29 and below can call lock before any valid connection exists.
    // Locking with a null cache causes first text input to fail.
    if (shouldReconnectInputConnectionWorkaround() && !proxyAdapterView.hasCachedConnection()) {
      logReconnectDebug("skip lockInputConnection because cached connection is null");
      return;
    }

    proxyAdapterView.setLocked(true);
  }

  /** Sets the proxy adapter view back to its default behavior. */
  public void unlockInputConnection() {
    if (proxyAdapterView == null) {
      return;
    }

    proxyAdapterView.setLocked(false);
  }

  /** Restore the original InputConnection, if needed. */
  void dispose() {
    if (useHybridComposition) {
      return;
    }
    resetInputConnection();
  }

  /**
   * Creates an InputConnection from the IME thread when needed.
   *
   * <p>We only need to create a {@link ThreadedInputConnectionProxyAdapterView} and create an
   * InputConnectionProxy on the IME thread when WebView is doing the same thing. So we rely on the
   * system calling this method for WebView's proxy view in order to know when we need to create our
   * own.
   *
   * <p>This method would normally be called for any View that used the InputMethodManager. We rely
   * on flutter/engine filtering the calls we receive down to the ones in our hierarchy and the
   * system WebView in order to know whether or not the system WebView expects an InputConnection on
   * the IME thread.
   */
  @Override
  public boolean checkInputConnectionProxy(final View view) {
    if (useHybridComposition) {
      return super.checkInputConnectionProxy(view);
    }
    // Check to see if the view param is WebView's ThreadedInputConnectionProxyView.
    View previousProxy = threadedInputConnectionProxyView;
    threadedInputConnectionProxyView = view;
    if (previousProxy == view) {
      // This isn't a new ThreadedInputConnectionProxyView. Ignore it.
      return super.checkInputConnectionProxy(view);
    }
    if (containerView == null) {
      Log.e(
        LOG_TAG,
        "Can't create a proxy view because there's no container view. Text input may not work.");
      return super.checkInputConnectionProxy(view);
    }

    // We've never seen this before, so we make the assumption that this is WebView's
    // ThreadedInputConnectionProxyView. We are making the assumption that the only view that could
    // possibly be interacting with the IMM here is WebView's ThreadedInputConnectionProxyView.
    proxyAdapterView =
      new ThreadedInputConnectionProxyAdapterView(
        /*containerView=*/ containerView,
        /*targetView=*/ view,
        /*imeHandler=*/ view.getHandler());
    setInputConnectionTarget(/*targetView=*/ proxyAdapterView);
    return super.checkInputConnectionProxy(view);
  }

  /**
   * Ensure that input creation happens back on {@link #containerView}'s thread once this view no
   * longer has focus.
   *
   * <p>The logic in {@link #checkInputConnectionProxy} forces input creation to happen on Webview's
   * thread for all connections. We undo it here so users will be able to go back to typing in
   * Flutter UIs as expected.
   */
  @Override
  public void clearFocus() {
    super.clearFocus();

    if (useHybridComposition) {
      return;
    }
    resetInputConnection();
  }

  /**
   * Ensure that input creation happens back on {@link #containerView}.
   *
   * <p>The logic in {@link #checkInputConnectionProxy} forces input creation to happen on Webview's
   * thread for all connections. We undo it here so users will be able to go back to typing in
   * Flutter UIs as expected.
   */
  private void resetInputConnection() {
    if (proxyAdapterView == null) {
      // No need to reset the InputConnection to the default thread if we've never changed it.
      return;
    }
    if (containerView == null) {
      Log.e(LOG_TAG, "Can't reset the input connection to the container view because there is none.");
      return;
    }
    setInputConnectionTarget(/*targetView=*/ containerView);
  }

  /**
   * This is the crucial trick that gets the InputConnection creation to happen on the correct
   * thread pre Android N.
   * https://cs.chromium.org/chromium/src/content/public/android/java/src/org/chromium/content/browser/input/ThreadedInputConnectionFactory.java?l=169&rcl=f0698ee3e4483fad5b0c34159276f71cfaf81f3a
   *
   * <p>{@code targetView} should have a {@link View#getHandler} method with the thread that future
   * InputConnections should be created on.
   */
  private void setInputConnectionTarget(final View targetView) {
    if (containerView == null) {
      Log.e(
        LOG_TAG,
        "Can't set the input connection target because there is no containerView to use as a handler.");
      return;
    }

    targetView.requestFocus();
    containerView.post(
      new Runnable() {
        @Override
        public void run() {
          if (containerView == null) {
            Log.e(
                    LOG_TAG,
                    "Can't set the input connection target because there is no containerView to use as a handler.");
            return;
          }

          InputMethodManager imm =
                  (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
          // This is a hack to make InputMethodManager believe that the target view now has focus.
          // As a result, InputMethodManager will think that targetView is focused, and will call
          // getHandler() of the view when creating input connection.

          // Step 1: Set targetView as InputMethodManager#mNextServedView. This does not affect
          // the real window focus.
          targetView.onWindowFocusChanged(true);

          // Step 2: Have InputMethodManager focus in on targetView. As a result, IMM will call
          // onCreateInputConnection() on targetView on the same thread as
          // targetView.getHandler(). It will also call subsequent InputConnection methods on this
          // thread. This is the IME thread in cases where targetView is our proxyAdapterView.

          // TODO (ALexVincent525): Currently only prompt has been tested, still needs more test cases.
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            imm.isActive(containerView);
          }
        }
      });
  }

  protected boolean shouldReconnectInputConnectionWorkaround() {
    return Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q;
  }

  public void reconnectInputConnection() {
    reconnectInputConnection("manual", true);
  }

  public void reconnectInputConnectionAfterUnlock(String reason) {
    reconnectInputConnectionDelayed(reason, false, 24L);
  }

  protected void reconnectInputConnectionDelayed(String reason, boolean showSoftInput, long delayMs) {
    if (!shouldReconnectInputConnectionWorkaround()) {
      return;
    }
    if (!isAttachedToWindow()) {
      logReconnectDebug("skip delayed: not attached (reason=" + reason + ")");
      return;
    }
    postDelayed(new ReconnectInputRunnable(reason, showSoftInput), Math.max(0L, delayMs));
  }

  protected long getDefaultAutoReconnectInitialDelayMs() {
    return AUTO_RECONNECT_INITIAL_DELAY_MS;
  }

  protected void reconnectInputConnection(String reason, boolean showSoftInput) {
    if (!shouldReconnectInputConnectionWorkaround()) {
      return;
    }
    if (!isAttachedToWindow()) {
      logReconnectDebug("skip: not attached (reason=" + reason + ")");
      return;
    }
    post(new ReconnectInputRunnable(reason, showSoftInput));
  }

  private void logReconnectDebug(String message) {
    if (!shouldReconnectInputConnectionWorkaround()) {
      return;
    }
    Log.d(LOG_TAG, "[reconnectInput] " + message);
    Map<String, Object> payload = new HashMap<>();
    payload.put("message", message);
    payload.put("hasWindowFocus", hasWindowFocus());
    payload.put("hasFocus", hasFocus());
    payload.put("isShown", isShown());
    payload.put("useHybridComposition", useHybridComposition);
    onInputConnectionDebugLog(payload);
  }

  protected void onInputConnectionDebugLog(Map<String, Object> payload) {}

  @Nullable
  private InputMethodManager getInputMethodManager() {
    return (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
  }

  private final class ReconnectInputRunnable implements Runnable {
    private final String reason;
    private final boolean showSoftInput;
    private int attempt = 0;

    ReconnectInputRunnable(String reason, boolean showSoftInput) {
      this.reason = reason;
      this.showSoftInput = showSoftInput;
    }

    @Override
    public void run() {
      if (!isAttachedToWindow()) {
        logReconnectDebug("abort: detached (reason=" + reason + ")");
        return;
      }

      attempt++;

      // Keep current focus chain; clearFocus() may close IME on some Android 9/10 devices.
      boolean focusRequested = hasFocus() || requestFocus();
      View targetView = thisTargetView();
      boolean targetFocusRequested = targetView != null && (targetView.isFocused() || targetView.requestFocus());

      InputMethodManager imm = getInputMethodManager();
      boolean restarted = false;
      boolean shown = false;
      if (imm != null) {
        if (!useHybridComposition && targetView != null && containerView != null) {
          setInputConnectionTarget(targetView);
        }
        View restartTarget = targetView != null ? targetView : InputAwareWebView.this;
        imm.viewClicked(restartTarget);
        imm.restartInput(restartTarget);
        restarted = true;
        if (showSoftInput && hasWindowFocus() && isShown() && !imm.isActive(restartTarget)) {
          shown = imm.showSoftInput(InputAwareWebView.this, InputMethodManager.SHOW_IMPLICIT);
        }
      }

      boolean active = imm != null && (imm.isActive(InputAwareWebView.this)
              || (targetView != null && imm.isActive(targetView)));
      logReconnectDebug(
              "attempt=" + attempt
                      + ", reason=" + reason
                      + ", focusRequested=" + focusRequested
                      + ", targetFocusRequested=" + targetFocusRequested
                      + ", restarted=" + restarted
                      + ", shown=" + shown
                      + ", active=" + active
                      + ", hybrid=" + useHybridComposition);

      if (!active && attempt < MAX_RECONNECT_INPUT_RETRIES) {
        postDelayed(this, RECONNECT_INPUT_DELAY_MS);
      } else if (!active) {
        logReconnectFailureSnapshot(reason, targetView, imm);
        logReconnectDebug("failed after retries (reason=" + reason + ")");
      } else {
        logReconnectDebug("success (reason=" + reason + ")");
      }
    }

    @Nullable
    private View thisTargetView() {
      if (useHybridComposition) {
        return InputAwareWebView.this;
      }
      if (proxyAdapterView != null) {
        return proxyAdapterView;
      }
      if (threadedInputConnectionProxyView != null) {
        return threadedInputConnectionProxyView;
      }
      return InputAwareWebView.this;
    }

    private void logReconnectFailureSnapshot(String reason, @Nullable View targetView, @Nullable InputMethodManager imm) {
      View root = getRootView();
      View focused = root != null ? root.findFocus() : null;
      logReconnectDebug(
              "failureSnapshot reason=" + reason
                      + ", webViewHasFocus=" + hasFocus()
                      + ", webViewWindowFocus=" + hasWindowFocus()
                      + ", webViewShown=" + isShown()
                      + ", targetClass=" + (targetView != null ? targetView.getClass().getName() : "null")
                      + ", targetFocused=" + (targetView != null && targetView.isFocused())
                      + ", rootFocusedClass=" + (focused != null ? focused.getClass().getName() : "null")
                      + ", immAvailable=" + (imm != null)
                      + ", immAcceptingText=" + (imm != null && imm.isAcceptingText())
                      + ", immActiveWebView=" + (imm != null && imm.isActive(InputAwareWebView.this))
                      + ", immActiveTarget=" + (imm != null && targetView != null && imm.isActive(targetView)));
    }
  }

  @Override
  protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
    if (useHybridComposition) {
      super.onFocusChanged(focused, direction, previouslyFocusedRect);
      return;
    }
    // This works around a crash when old (<67.0.3367.0) Chromium versions are used.

    // Prior to Chromium 67.0.3367 the following sequence happens when a select drop down is shown
    // on tablets:
    //
    //  - WebView is calling ListPopupWindow#show
    //  - buildDropDown is invoked, which sets mDropDownList to a DropDownListView.
    //  - showAsDropDown is invoked - resulting in mDropDownList being added to the window and is
    //    also synchronously performing the following sequence:
    //    - WebView's focus change listener is loosing focus (as mDropDownList got it)
    //    - WebView is hiding all popups (as it lost focus)
    //    - WebView's SelectPopupDropDown#hide is invoked.
    //    - DropDownPopupWindow#dismiss is invoked setting mDropDownList to null.
    //  - mDropDownList#setSelection is invoked and is throwing a NullPointerException (as we just set mDropDownList to null).
    //
    // To workaround this, we drop the problematic focus lost call.
    // See more details on: https://github.com/flutter/flutter/issues/54164
    //
    // We don't do this after Android P as it shipped with a new enough WebView version, and it's
    // better to not do this on all future Android versions in case DropDownListView's code changes.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P
            && isCalledFromListPopupWindowShow()
            && !focused) {
      return;
    }
    super.onFocusChanged(focused, direction, previouslyFocusedRect);
  }

  private boolean isCalledFromListPopupWindowShow() {
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    for (StackTraceElement stackTraceElement : stackTraceElements) {
      if (stackTraceElement.getClassName().equals(ListPopupWindow.class.getCanonicalName())
              && stackTraceElement.getMethodName().equals("show")) {
        return true;
      }
    }
    return false;
  }
}