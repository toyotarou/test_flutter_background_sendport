import 'dart:isolate';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:background_task/background_task.dart';

final ReceivePort _backgroundReceivePort = ReceivePort();

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  BackgroundReceivePortSingleton.instance.register(_backgroundReceivePort.sendPort);

  _backgroundReceivePort.listen((message) {
    debugPrint('📥 Dart received: $message');
  });

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('ReceivePortテスト')),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ElevatedButton(
                onPressed: () {
                  backgroundHandler({'lat': 35.6895, 'lng': 139.6917});
                },
                child: const Text('📦 テストデータ送信（Dart内）'),
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: () async {
                  await BackgroundTask.instance.setBackgroundHandler(backgroundHandler);
                  await BackgroundTask.instance.start();
                  debugPrint('✅ BackgroundTask started');
                },
                child: const Text('▶️ Background Task 開始'),
              ),
              const SizedBox(height: 20),
              ElevatedButton(onPressed: sendWifiLocationFromKotlin, child: const Text('📡 Kotlin から Wi-Fi 情報取得')),
            ],
          ),
        ),
      ),
    );
  }
}

class BackgroundReceivePortSingleton {
  BackgroundReceivePortSingleton._();

  static final instance = BackgroundReceivePortSingleton._();

  SendPort? _sendPort;

  void register(SendPort port) {
    _sendPort = port;
  }

  SendPort? get port => _sendPort;
}

@pragma('vm:entry-point')
void backgroundHandler(dynamic data) {
  debugPrint('📡 backgroundHandler 呼ばれました！');
  debugPrint('📡 backgroundHandler: $data');

  final sendPort = BackgroundReceivePortSingleton.instance.port;
  if (sendPort != null) {
    sendPort.send('[backgroundHandler] $data');
  } else {
    debugPrint('⚠️ SendPort not found');
  }
}

Future<void> sendWifiLocationFromKotlin() async {
  const methodChannel = MethodChannel('com.example.flutter_background_sendport/bg');

  try {
    final result = await methodChannel.invokeMethod('getCurrentWifiLocation');
    debugPrint('✅ Kotlinからの結果: $result');
  } on PlatformException catch (e) {
    debugPrint('⚠️ PlatformException: ${e.message}');
  }
}
