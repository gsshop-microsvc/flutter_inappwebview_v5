# Android 9/10 WebView Keyboard Input Recovery Guide

## Problem Summary

On Android 9 and 10, when a WebView with focused input is subject to:
- Screen lock/unlock
- Navigation to native screen and back
- Activity pausing and resuming

The IME (Input Method Editor/keyboard) becomes **visible but non-functional** — typed characters do not reach the WebView input element. This is because the InputConnection becomes stale when the view hierarchy is reconfigured.

## Root Cause

Android 9/10's WebView implementation has limitations in maintaining valid InputConnection state across lifecycle transitions. Unlike Android 11+, these versions require explicit recovery of the InputConnection after such transitions.

## Solution Overview

### 1. Explicit IME Hiding Before Screen Lock

**When:** App lifecycle transitions to `inactive` or `paused`

**What:** Call `hideInputConnectionBeforeScreenLock()` to explicitly close the IME before screen lock occurs.

```dart
@override
void didChangeAppLifecycleState(AppLifecycleState state) {
  if (state == AppLifecycleState.inactive || state == AppLifecycleState.paused) {
    _hideInputBeforeLock();
  }
}

Future<void> _hideInputBeforeLock() async {
  if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
    // Recommended: Check SDK version to avoid unnecessary calls
    final info = await DeviceInfoPlugin().androidInfo;
    if (info.version.sdkInt! >= 28 && info.version.sdkInt! <= 29) {
      // Explicitly for Android 9/10, but no harm calling on others
      await _webViewController.android?.hideInputConnectionBeforeScreenLock();
    } else if (info.version.sdkInt! < 28) {
      // Android < 9 may also benefit
      await _webViewController.android?.hideInputConnectionBeforeScreenLock();
    }
  }
}
```

### 2. Explicit Input Recovery After Screen Unlock

**When:** App lifecycle transitions to `resumed`

**What:** Call `recoverInputConnectionAfterScreenUnlock()` to restore the InputConnection if there is a focused editable element.

```dart
@override
void didChangeAppLifecycleState(AppLifecycleState state) {
  if (state == AppLifecycleState.resumed) {
    _recoverInputAfterUnlock();
  }
}

Future<void> _recoverInputAfterUnlock() async {
  if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
    // Recommended: Check SDK version to focus on Android 9/10
    final info = await DeviceInfoPlugin().androidInfo;
    if (info.version.sdkInt! >= 28 && info.version.sdkInt! <= 29) {
      final recovered = await _webViewController.android?.recoverInputConnectionAfterScreenUnlock();
      if (recovered != null) {
        debugPrint('[WebView] Input connection recovered: $recovered');
      }
    } else if (info.version.sdkInt! < 28) {
      // Android < 9 may also benefit
      await _webViewController.android?.recoverInputConnectionAfterScreenUnlock();
    }
  }
}
```

## Complete Example

```dart
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart' as foundation;
import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

class WebViewScreen extends StatefulWidget {
  @override
  State<WebViewScreen> createState() => _WebViewScreenState();
}

class _WebViewScreenState extends State<WebViewScreen>
    with WidgetsBindingObserver {
  late InAppWebViewController _webViewController;
  late DeviceInfoPlugin _deviceInfo;
  bool _isAndroid9Or10 = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _deviceInfo = DeviceInfoPlugin();
    _initializeAndroidVersion();
  }

  Future<void> _initializeAndroidVersion() async {
    if (!foundation.kIsWeb &&
        foundation.defaultTargetPlatform == TargetPlatform.android) {
      final info = await _deviceInfo.androidInfo;
      _isAndroid9Or10 = info.version.sdkInt! >= 28 && info.version.sdkInt! <= 29;
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    switch (state) {
      case AppLifecycleState.inactive:
      case AppLifecycleState.paused:
        _handleLifecycleInactive();
        break;
      case AppLifecycleState.resumed:
        _handleLifecycleResumed();
        break;
      case AppLifecycleState.detached:
        // cleanup if needed
        break;
      case AppLifecycleState.hidden:
        // handle if needed (Flutter 3.13+)
        break;
    }
  }

  Future<void> _handleLifecycleInactive() async {
    if (_isAndroid9Or10) {
      try {
        final hidden =
            await _webViewController.android?.hideInputConnectionBeforeScreenLock();
        debugPrint('[WebView] IME hidden before lock: $hidden');
      } catch (e) {
        debugPrint('[WebView] Error hiding IME: $e');
      }
    }
  }

  Future<void> _handleLifecycleResumed() async {
    if (_isAndroid9Or10) {
      try {
        final recovered =
            await _webViewController.android?.recoverInputConnectionAfterScreenUnlock();
        debugPrint('[WebView] Input recovered after unlock: $recovered');
      } catch (e) {
        debugPrint('[WebView] Error recovering input: $e');
      }
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('WebView')),
      body: InAppWebView(
        initialUrlRequest: URLRequest(url: WebUri('https://example.com')),
        onWebViewCreated: (controller) {
          _webViewController = controller;
        },
      ),
    );
  }
}
```

## Platform-Specific Behavior

| Android Version | hideInputConnectionBeforeScreenLock | recoverInputConnectionAfterScreenUnlock | Notes |
|---|---|---|---|
| < 9 (API < 28) | Supported (may help with older issues) | Supported | Legacy versions, no-op if not needed |
| 9 (API 28) | **REQUIRED** | **REQUIRED** | Critical – InputConnection becomes stale |
| 10 (API 29) | **REQUIRED** | **REQUIRED** | Critical – InputConnection becomes stale |
| 11+ (API >= 30) | No-op (returns false) | No-op (returns false) | Android 11+ handles this internally |

## Best Practices

### 1. Guard Calls with Platform/Version Checks

```dart
// ❌ Don't call unconditionally
await _webViewController.android?.hideInputConnectionBeforeScreenLock();

// ✅ Do check version
final info = await DeviceInfoPlugin().androidInfo;
if (info.version.sdkInt! >= 28 && info.version.sdkInt! <= 29) {
  await _webViewController.android?.hideInputConnectionBeforeScreenLock();
}
```

### 2. Handle Exceptions Gracefully

```dart
try {
  final result = await _webViewController.android?.hideInputConnectionBeforeScreenLock();
  if (result == null) {
    // Not on Android
    return;
  }
  // Handle result
} catch (e) {
  debugPrint('Keyboard fix error: $e');
}
```

### 3. Avoid Spamming Calls

Only call these methods at specific lifecycle boundaries, not on every frame or event.

### 4. Return Value Interpretation

- **`hideInputConnectionBeforeScreenLock()`**
  - `true`: IME was successfully hidden
  - `false`: Not applicable (Android 11+) or no IME was visible
  
- **`recoverInputConnectionAfterScreenUnlock()`**
  - `true`: Found editable element and attempted recovery
  - `false`: No focused editable element or Android 11+

## Diagnosis Helper

If the fix still doesn't work, use the diagnostic method:

```dart
Future<void> _diagnoseInputConnection() async {
  try {
    final diagnosis = await _webViewController.android?.diagnoseInputConnection();
    debugPrint('[WebView] Input diagnosis: $diagnosis');
    // This returns a Map with native and JS state info for debugging
  } catch (e) {
    debugPrint('[WebView] Diagnosis error: $e');
  }
}
```

## Additional Notes

### Navigation Transitions

If navigating between screens within the app:

```dart
// Before popping to previous screen with WebView
Navigator.of(context).pop();

// Or better: Preemptively hide IME
await _webViewController.android?.hideInputConnectionBeforeScreenLock();
Navigator.of(context).pop();
```

### Multiple WebViews

If the app has multiple WebViews, apply the fix to each:

```dart
List<InAppWebViewController> _controllers = [];

Future<void> _handleLifecycleResumed() async {
  for (var controller in _controllers) {
    await controller.android?.recoverInputConnectionAfterScreenUnlock();
  }
}
```

### Testing

1. **Manual Test:**
   - Focus on a WebView input field
   - Press device Power button to lock screen
   - Unlock screen
   - Type – should work now

2. **Automated Test:**
   - Use WidgetsFlutterBinding.ensureInitialized()
   - Simulate lifecycle changes
   - Verify input still works after state transitions

## Related Resources

- Flutter docs: [Handling Lifecycle States](https://flutter.dev/docs/development/data-and-backend/state-mgmt/lifecycle)
- Android docs: [InputMethodManager](https://developer.android.com/reference/android/view/inputmethod/InputMethodManager)
- WebView InputConnection: [Android Platform Documentation](https://developer.android.com/reference/android/view/inputmethod/InputConnection)

## Troubleshooting

| Issue | Possible Cause | Solution |
|---|---|---|
| Method not found | Using older flutter_inappwebview version | Update to version with Android 9/10 fix |
| fix doesn't work | Lifecycle callbacks not being called | Verify WidgetsBindingObserver is properly added |
| IME still doesn't appear | JS-side focus not working | Check if focused element is actually editable (not readonly/disabled) |
| Multiple IME hide/show | Rapid lifecycle changes | Add debouncing or async wait |
