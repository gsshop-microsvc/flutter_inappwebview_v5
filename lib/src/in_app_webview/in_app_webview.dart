import 'dart:async';
import 'dart:collection';
import 'dart:developer';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter/gestures.dart';

import '../context_menu.dart';
import '../types.dart';

import 'webview.dart';
import 'in_app_webview_controller.dart';
import 'in_app_webview_options.dart';
import '../pull_to_refresh/pull_to_refresh_controller.dart';
import '../pull_to_refresh/pull_to_refresh_options.dart';

///Flutter Widget for adding an **inline native WebView** integrated in the flutter widget tree.
class InAppWebViewV2 extends StatefulWidget implements WebView {
  /// `gestureRecognizers` specifies which gestures should be consumed by the WebView.
  /// It is possible for other gesture recognizers to be competing with the web view on pointer
  /// events, e.g if the web view is inside a [ListView] the [ListView] will want to handle
  /// vertical drags. The web view will claim gestures that are recognized by any of the
  /// recognizers on this list.
  /// When `gestureRecognizers` is empty or null, the web view will only handle pointer events for gestures that
  /// were not claimed by any other gesture recognizer.
  final Set<Factory<OneSequenceGestureRecognizer>>? gestureRecognizers;

  ///The window id of a [CreateWindowAction.windowId].
  final int? windowId;

  const InAppWebViewV2({
    Key? key,
    this.windowId,
    this.initialUrlRequest,
    this.initialFile,
    this.initialData,
    this.initialOptions,
    this.initialUserScripts,
    this.pullToRefreshController,
    this.implementation = WebViewImplementation.NATIVE,
    this.contextMenu,
    this.onWebViewCreated,
    this.onLoadStart,
    this.onLoadStop,
    this.onLoadError,
    this.onLoadHttpError,
    this.onConsoleMessage,
    this.onProgressChanged,
    this.shouldOverrideUrlLoading,
    this.onLoadResource,
    this.onScrollChanged,
    @Deprecated('Use `onDownloadStartRequest` instead') this.onDownloadStart,
    this.onDownloadStartRequest,
    this.onLoadResourceCustomScheme,
    this.onCreateWindow,
    this.onCloseWindow,
    this.onJsAlert,
    this.onJsConfirm,
    this.onJsPrompt,
    this.onReceivedHttpAuthRequest,
    this.onReceivedServerTrustAuthRequest,
    this.onReceivedClientCertRequest,
    this.onFindResultReceived,
    this.shouldInterceptAjaxRequest,
    this.onAjaxReadyStateChange,
    this.onAjaxProgress,
    this.shouldInterceptFetchRequest,
    this.onUpdateVisitedHistory,
    this.onPrint,
    this.onLongPressHitTestResult,
    this.onEnterFullscreen,
    this.onExitFullscreen,
    this.onPageCommitVisible,
    this.onTitleChanged,
    this.onWindowFocus,
    this.onWindowBlur,
    this.onOverScrolled,
    this.onZoomScaleChanged,
    this.androidOnSafeBrowsingHit,
    this.androidOnPermissionRequest,
    this.androidOnGeolocationPermissionsShowPrompt,
    this.androidOnGeolocationPermissionsHidePrompt,
    this.androidShouldInterceptRequest,
    this.androidOnRenderProcessGone,
    this.androidOnRenderProcessResponsive,
    this.androidOnRenderProcessUnresponsive,
    this.androidOnFormResubmission,
    @Deprecated('Use `onZoomScaleChanged` instead') this.androidOnScaleChanged,
    this.androidOnReceivedIcon,
    this.androidOnReceivedTouchIconUrl,
    this.androidOnJsBeforeUnload,
    this.androidOnReceivedLoginRequest,
    this.iosOnWebContentProcessDidTerminate,
    this.iosOnDidReceiveServerRedirectForProvisionalNavigation,
    this.iosOnNavigationResponse,
    this.iosShouldAllowDeprecatedTLS,
    this.gestureRecognizers,
  }) : super(key: key);

  @override
  _InAppWebViewV2State createState() => _InAppWebViewV2State();

  @override
  final void Function(InAppWebViewControllerV2 controller)?
      androidOnGeolocationPermissionsHidePrompt;

  @override
  final Future<GeolocationPermissionShowPromptResponse?> Function(
          InAppWebViewControllerV2 controller, String origin)?
      androidOnGeolocationPermissionsShowPrompt;

  @override
  final Future<PermissionRequestResponse?> Function(
      InAppWebViewControllerV2 controller,
      String origin,
      List<String> resources)? androidOnPermissionRequest;

  @override
  final Future<SafeBrowsingResponse?> Function(
      InAppWebViewControllerV2 controller,
      Uri url,
      SafeBrowsingThreat? threatType)? androidOnSafeBrowsingHit;

  @override
  final InAppWebViewInitialData? initialData;

  @override
  final String? initialFile;

  @override
  final InAppWebViewGroupOptions? initialOptions;

  @override
  final URLRequest? initialUrlRequest;

  @override
  final WebViewImplementation implementation;

  @override
  final UnmodifiableListView<UserScript>? initialUserScripts;

  @override
  final PullToRefreshController? pullToRefreshController;

  @override
  final ContextMenu? contextMenu;

  @override
  final void Function(InAppWebViewControllerV2 controller, Uri? url)?
      onPageCommitVisible;

  @override
  final void Function(InAppWebViewControllerV2 controller, String? title)?
      onTitleChanged;

  @override
  final void Function(InAppWebViewControllerV2 controller)?
      iosOnDidReceiveServerRedirectForProvisionalNavigation;

  @override
  final void Function(InAppWebViewControllerV2 controller)?
      iosOnWebContentProcessDidTerminate;

  @override
  final Future<IOSNavigationResponseAction?> Function(
      InAppWebViewControllerV2 controller,
      IOSWKNavigationResponse navigationResponse)? iosOnNavigationResponse;

  @override
  final Future<IOSShouldAllowDeprecatedTLSAction?> Function(
      InAppWebViewControllerV2 controller,
      URLAuthenticationChallenge challenge)? iosShouldAllowDeprecatedTLS;

  @override
  final Future<AjaxRequestAction> Function(
          InAppWebViewControllerV2 controller, AjaxRequest ajaxRequest)?
      onAjaxProgress;

  @override
  final Future<AjaxRequestAction?> Function(
          InAppWebViewControllerV2 controller, AjaxRequest ajaxRequest)?
      onAjaxReadyStateChange;

  @override
  final void Function(
          InAppWebViewControllerV2 controller, ConsoleMessage consoleMessage)?
      onConsoleMessage;

  @override
  final Future<bool?> Function(InAppWebViewControllerV2 controller,
      CreateWindowAction createWindowAction)? onCreateWindow;

  @override
  final void Function(InAppWebViewControllerV2 controller)? onCloseWindow;

  @override
  final void Function(InAppWebViewControllerV2 controller)? onWindowFocus;

  @override
  final void Function(InAppWebViewControllerV2 controller)? onWindowBlur;

  @override
  final void Function(InAppWebViewControllerV2 controller, Uint8List icon)?
      androidOnReceivedIcon;

  @override
  final void Function(
          InAppWebViewControllerV2 controller, Uri url, bool precomposed)?
      androidOnReceivedTouchIconUrl;

  ///Use [onDownloadStartRequest] instead
  @Deprecated('Use `onDownloadStartRequest` instead')
  @override
  final void Function(InAppWebViewControllerV2 controller, Uri url)?
      onDownloadStart;

  @override
  final void Function(InAppWebViewControllerV2 controller,
      DownloadStartRequest downloadStartRequest)? onDownloadStartRequest;

  @override
  final void Function(
      InAppWebViewControllerV2 controller,
      int activeMatchOrdinal,
      int numberOfMatches,
      bool isDoneCounting)? onFindResultReceived;

  @override
  final Future<JsAlertResponse?> Function(
          InAppWebViewControllerV2 controller, JsAlertRequest jsAlertRequest)?
      onJsAlert;

  @override
  final Future<JsConfirmResponse?> Function(InAppWebViewControllerV2 controller,
      JsConfirmRequest jsConfirmRequest)? onJsConfirm;

  @override
  final Future<JsPromptResponse?> Function(
          InAppWebViewControllerV2 controller, JsPromptRequest jsPromptRequest)?
      onJsPrompt;

  @override
  final void Function(InAppWebViewControllerV2 controller, Uri? url, int code,
      String message)? onLoadError;

  @override
  final void Function(InAppWebViewControllerV2 controller, Uri? url,
      int statusCode, String description)? onLoadHttpError;

  @override
  final void Function(
          InAppWebViewControllerV2 controller, LoadedResource resource)?
      onLoadResource;

  @override
  final Future<CustomSchemeResponse?> Function(
      InAppWebViewControllerV2 controller, Uri url)? onLoadResourceCustomScheme;

  @override
  final void Function(InAppWebViewControllerV2 controller, Uri? url)?
      onLoadStart;

  @override
  final void Function(InAppWebViewControllerV2 controller, Uri? url)?
      onLoadStop;

  @override
  final void Function(InAppWebViewControllerV2 controller,
      InAppWebViewHitTestResult hitTestResult)? onLongPressHitTestResult;

  @override
  final void Function(InAppWebViewControllerV2 controller, Uri? url)? onPrint;

  @override
  final void Function(InAppWebViewControllerV2 controller, int progress)?
      onProgressChanged;

  @override
  final Future<ClientCertResponse?> Function(
      InAppWebViewControllerV2 controller,
      URLAuthenticationChallenge challenge)? onReceivedClientCertRequest;

  @override
  final Future<HttpAuthResponse?> Function(InAppWebViewControllerV2 controller,
      URLAuthenticationChallenge challenge)? onReceivedHttpAuthRequest;

  @override
  final Future<ServerTrustAuthResponse?> Function(
      InAppWebViewControllerV2 controller,
      URLAuthenticationChallenge challenge)? onReceivedServerTrustAuthRequest;

  @override
  final void Function(InAppWebViewControllerV2 controller, int x, int y)?
      onScrollChanged;

  @override
  final void Function(
          InAppWebViewControllerV2 controller, Uri? url, bool? androidIsReload)?
      onUpdateVisitedHistory;

  @override
  final void Function(InAppWebViewControllerV2 controller)? onWebViewCreated;

  @override
  final Future<AjaxRequest?> Function(
          InAppWebViewControllerV2 controller, AjaxRequest ajaxRequest)?
      shouldInterceptAjaxRequest;

  @override
  final Future<FetchRequest?> Function(
          InAppWebViewControllerV2 controller, FetchRequest fetchRequest)?
      shouldInterceptFetchRequest;

  @override
  final Future<NavigationActionPolicy?> Function(
      InAppWebViewControllerV2 controller,
      NavigationAction navigationAction)? shouldOverrideUrlLoading;

  @override
  final void Function(InAppWebViewControllerV2 controller)? onEnterFullscreen;

  @override
  final void Function(InAppWebViewControllerV2 controller)? onExitFullscreen;

  @override
  final void Function(InAppWebViewControllerV2 controller, int x, int y,
      bool clampedX, bool clampedY)? onOverScrolled;

  @override
  final void Function(InAppWebViewControllerV2 controller, double oldScale,
      double newScale)? onZoomScaleChanged;

  @override
  final Future<WebResourceResponse?> Function(
          InAppWebViewControllerV2 controller, WebResourceRequest request)?
      androidShouldInterceptRequest;

  @override
  final Future<WebViewRenderProcessAction?> Function(
          InAppWebViewControllerV2 controller, Uri? url)?
      androidOnRenderProcessUnresponsive;

  @override
  final Future<WebViewRenderProcessAction?> Function(
          InAppWebViewControllerV2 controller, Uri? url)?
      androidOnRenderProcessResponsive;

  @override
  final void Function(
          InAppWebViewControllerV2 controller, RenderProcessGoneDetail detail)?
      androidOnRenderProcessGone;

  @override
  final Future<FormResubmissionAction?> Function(
      InAppWebViewControllerV2 controller, Uri? url)? androidOnFormResubmission;

  ///Use [onZoomScaleChanged] instead.
  @Deprecated('Use `onZoomScaleChanged` instead')
  @override
  final void Function(InAppWebViewControllerV2 controller, double oldScale,
      double newScale)? androidOnScaleChanged;

  @override
  final Future<JsBeforeUnloadResponse?> Function(
      InAppWebViewControllerV2 controller,
      JsBeforeUnloadRequest jsBeforeUnloadRequest)? androidOnJsBeforeUnload;

  @override
  final void Function(
          InAppWebViewControllerV2 controller, LoginRequest loginRequest)?
      androidOnReceivedLoginRequest;
}

class _InAppWebViewV2State extends State<InAppWebViewV2> {
  late InAppWebViewControllerV2 _controller;
  AndroidViewController? _androidViewController;
  late MethodChannel _channel;

  int _persistedId = DateTime.now().millisecondsSinceEpoch % 100000;
  ValueNotifier<bool> _lifecycleState = ValueNotifier<bool>(false);

  late AppLifecycleListener _lifecycleListener;

  @override
  void initState() {
    super.initState();
    _lifecycleListener = AppLifecycleListener(
      onRestart: () {
        log('[keykat] onRestart');
        _lifecycleState.value = !(_lifecycleState.value);
      },
      onHide: () {
        log('[keykat] onHide');
      },
      onPause: () {
        log('[keykat] onPause');
      },
      onDetach: () {
        log('[keykat] onDeatch');
      },
      onInactive: () {
        log('[keykat] onInactive');
      },
    );

    _channel = MethodChannel(
        'com.microsvc/flutter_inappwebview_v2_sub_${_persistedId}');
  }

  @override
  dispose() {
    super.dispose();
    _lifecycleListener.dispose();
    if (Platform.isAndroid) {
      _channel.invokeMethod('persistedDispose');
    }
  }

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      var useHybridComposition =
          widget.initialOptions?.android.useHybridComposition ?? false;

      if (!useHybridComposition && widget.pullToRefreshController != null) {
        throw new Exception(
            "To use the pull-to-refresh feature, useHybridComposition Android-specific option MUST be true!");
      }

      return ValueListenableBuilder<bool>(
          valueListenable: _lifecycleState,
          builder: (context, state, child) {
            // log('[keykat] valueListenableBuilder: $_persistedId $state');
            return PlatformViewLink(
              key: ValueKey('${_persistedId}_$state'),
              viewType: 'com.microsvc/flutter_inappwebview_v2',
              surfaceFactory: (
                BuildContext context,
                PlatformViewController controller,
              ) {
                return AndroidViewSurface(
                  controller: controller as AndroidViewController,
                  gestureRecognizers: widget.gestureRecognizers ??
                      const <Factory<OneSequenceGestureRecognizer>>{},
                  hitTestBehavior: PlatformViewHitTestBehavior.opaque,
                );
              },
              onCreatePlatformView: (PlatformViewCreationParams params) {
                return _initSurfaceAndroidViewController(params);
                // return _androidViewController = state
                //     ? _initExpensiveAndroidViewController(params)
                //     : _initSurfaceAndroidViewController(params);
              },
            );
          });
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      return UiKitView(
        viewType: 'com.microsvc/flutter_inappwebview',
        onPlatformViewCreated: _onPlatformViewCreated,
        gestureRecognizers: widget.gestureRecognizers,
        creationParams: <String, dynamic>{
          'initialUrlRequest': widget.initialUrlRequest?.toMap(),
          'initialFile': widget.initialFile,
          'initialData': widget.initialData?.toMap(),
          'initialOptions': widget.initialOptions?.toMap() ?? {},
          'contextMenu': widget.contextMenu?.toMap() ?? {},
          'windowId': widget.windowId,
          'implementation': widget.implementation.toValue(),
          'initialUserScripts':
              widget.initialUserScripts?.map((e) => e.toMap()).toList() ?? [],
          'pullToRefreshOptions':
              widget.pullToRefreshController?.options.toMap() ??
                  PullToRefreshOptions(enabled: false).toMap()
        },
        creationParamsCodec: const StandardMessageCodec(),
      );
    }
    return Text(
        '$defaultTargetPlatform is not yet supported by the flutter_inappwebview plugin');
  }

  AndroidViewController _initExpensiveAndroidViewController(
    PlatformViewCreationParams params,
  ) {
    return PlatformViewsService.initExpensiveAndroidView(
      id: params.id,
      viewType: 'com.microsvc/flutter_inappwebview_v2',
      layoutDirection: Directionality.maybeOf(context) ?? TextDirection.rtl,
      creationParams: <String, dynamic>{
        'initialUrlRequest': widget.initialUrlRequest?.toMap(),
        'initialFile': widget.initialFile,
        'initialData': widget.initialData?.toMap(),
        'initialOptions': widget.initialOptions?.toMap() ?? {},
        'contextMenu': widget.contextMenu?.toMap() ?? {},
        'windowId': widget.windowId,
        'persistedId': _persistedId,
        'implementation': widget.implementation.toValue(),
        'initialUserScripts':
            widget.initialUserScripts?.map((e) => e.toMap()).toList() ?? [],
        'pullToRefreshOptions':
            widget.pullToRefreshController?.options.toMap() ??
                PullToRefreshOptions(enabled: false).toMap()
      },
      creationParamsCodec: const StandardMessageCodec(),
    )
      ..addOnPlatformViewCreatedListener(params.onPlatformViewCreated)
      ..addOnPlatformViewCreatedListener(
          (id) => _onPlatformViewCreated(_persistedId))
      ..create();
  }

  AndroidViewController _initSurfaceAndroidViewController(
    PlatformViewCreationParams params,
  ) {
    return PlatformViewsService.initSurfaceAndroidView(
      id: params.id,
      viewType: 'com.microsvc/flutter_inappwebview_v2',
      layoutDirection: Directionality.maybeOf(context) ?? TextDirection.rtl,
      creationParams: <String, dynamic>{
        'initialUrlRequest': widget.initialUrlRequest?.toMap(),
        'initialFile': widget.initialFile,
        'initialData': widget.initialData?.toMap(),
        'initialOptions': widget.initialOptions?.toMap() ?? {},
        'contextMenu': widget.contextMenu?.toMap() ?? {},
        'windowId': widget.windowId,
        'persistedId': _persistedId,
        'implementation': widget.implementation.toValue(),
        'initialUserScripts':
            widget.initialUserScripts?.map((e) => e.toMap()).toList() ?? [],
        'pullToRefreshOptions':
            widget.pullToRefreshController?.options.toMap() ??
                PullToRefreshOptions(enabled: false).toMap()
      },
      creationParamsCodec: const StandardMessageCodec(),
    )
      ..addOnPlatformViewCreatedListener(params.onPlatformViewCreated)
      ..addOnPlatformViewCreatedListener(
          (id) => _onPlatformViewCreated(_persistedId))
      ..create();
  }

  @override
  void didUpdateWidget(InAppWebViewV2 oldWidget) {
    super.didUpdateWidget(oldWidget);
  }

  void _onPlatformViewCreated(int id) {
    _controller = InAppWebViewControllerV2(id, widget);
    widget.pullToRefreshController?.initMethodChannel(id);
    if (widget.onWebViewCreated != null) {
      widget.onWebViewCreated!(_controller);
    }
  }
}
