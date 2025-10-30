import 'package:flutter_inappwebview_v2/src/platform_util.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_inappwebview_v2/flutter_inappwebview.dart';

/// 패키지 이름 변경이 제대로 되었는지 확인하는 기본 테스트
void main() {
  group('Package Name Migration Test', () {
    test('InAppWebView 클래스가 import되는지 확인', () {
      // 패키지가 제대로 import 되었는지 확인
      expect(InAppWebViewV2, isNotNull);
    });

    test('InAppWebViewSettings 클래스가 import되는지 확인', () {
      // 옵션 클래스가 제대로 import 되었는지 확인
      expect(InAppWebViewOptions, isNotNull);
    });

    test('CookieManager 클래스가 import되는지 확인', () {
      // CookieManager가 제대로 import 되었는지 확인
      expect(() => CookieManager.instance(), returnsNormally);
    });

    test('기본 InAppWebViewSettings 생성 테스트', () {
      // 기본 옵션으로 InAppWebViewSettings 생성
      final options = InAppWebViewOptions();
      expect(options, isNotNull);
      expect(options.javaScriptEnabled, isNotNull);
    });

    test('PlatformUtil 클래스가 import되는지 확인', () {
      // PlatformUtil이 제대로 import 되었는지 확인
      expect(() => PlatformUtil.instance(), returnsNormally);
    });
  });
}
