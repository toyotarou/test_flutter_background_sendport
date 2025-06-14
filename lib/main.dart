import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:isolate';

import 'package:background_task/background_task.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';
import 'package:isar/isar.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';

import 'wifi_location_service_api.dart';

part 'main.g.dart';

final ReceivePort _backgroundReceivePort = ReceivePort();
late Isar isar;

@collection
class WifiCoordinate {
  Id id = Isar.autoIncrement;

  late String date;
  late String time;
  late String latitude;
  late String longitude;
  late String ssid;
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 🔽 パーミッションのリクエスト追加
  await [Permission.location, Permission.locationWhenInUse, Permission.locationAlways].request();

  final Directory dir = await getApplicationSupportDirectory();
  isar = await Isar.open(<CollectionSchema>[WifiCoordinateSchema], directory: dir.path);

  BackgroundReceivePortSingleton.instance.register(_backgroundReceivePort.sendPort);

  _backgroundReceivePort.listen((message) async {
    debugPrint('📥 Dart received: $message');
    if (message is String && message.contains('backgroundHandler')) {
      final Map<String, String>? data = _extractData(message);
      debugPrint('📦 パース結果: $data');
      if (data != null) {
        await _saveToIsar(data);
      }
    }
  });

  runApp(const MyApp());
}

Future<void> _saveToIsar(Map<String, String> data) async {
  final DateTime now = DateTime.now();

  final WifiCoordinate wifi = WifiCoordinate()
    ..date =
        '${now.year.toString().padLeft(4, '0')}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}'
    ..time =
        '${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}:${now.second.toString().padLeft(2, '0')}'
    ..latitude = data['lat'] ?? '0.0'
    ..longitude = data['lng'] ?? '0.0'
    ..ssid = data['ssid'] ?? 'unknown';

  await isar.writeTxn(() async {
    await isar.wifiCoordinates.put(wifi);
  });

  debugPrint('💾 保存完了: ${wifi.ssid} ${wifi.latitude}, ${wifi.longitude}');
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  List<WifiCoordinate> _records = <WifiCoordinate>[];
  Timer? _timer;
  late Ticker _ticker;
  late DateTime _lastSentTime;
  double _elapsedSeconds = 0.0;

  @override
  void initState() {
    super.initState();

    _loadRecords();

    // Plugin 初期化後にタイマー等を開始
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await BackgroundTask.instance.setBackgroundHandler(backgroundHandler);
      await BackgroundTask.instance.start();
      debugPrint('✅ BackgroundTask started');

      _lastSentTime = DateTime.now();

      // 🔽 ここでようやくタイマーを開始
      _timer = Timer.periodic(const Duration(minutes: 1), (Timer t) async {
        debugPrint('⏱️ タイマー発動');
        await sendWifiLocationFromKotlin(); // MissingPluginException を回避
      });

      // タイマー表示用の Ticker
      _ticker = Ticker((_) {
        setState(() {
          _elapsedSeconds = DateTime.now().difference(_lastSentTime).inMilliseconds / 1000.0;
        });
      });
      _ticker.start();
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _ticker.dispose();
    super.dispose();
  }

  Future<void> _loadRecords() async {
    final List<WifiCoordinate> list = await isar.wifiCoordinates.where().sortByDate().thenByTime().findAll();
    setState(() {
      _records = list;
    });
  }

  @override
  Widget build(BuildContext context) {
    final double remaining = (60.0 - _elapsedSeconds).clamp(0, 60);

    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('📡 Wi-Fi 情報記録一覧')),
        body: Column(
          children: <Widget>[
            const SizedBox(height: 12),

            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  ElevatedButton.icon(
                    onPressed: sendWifiLocationFromKotlin,
                    icon: const Icon(Icons.wifi),
                    label: const Text('📡 現在の位置を取得'),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton.icon(
                    onPressed: () {
                      WifiLocationServiceApi().startService();
                    },
                    icon: const Icon(Icons.play_arrow),
                    label: const Text('🟢 サービス開始'),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton.icon(
                    onPressed: () {
                      WifiLocationServiceApi().stopService();
                    },
                    icon: const Icon(Icons.stop),
                    label: const Text('⛔️ サービス停止'),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton(
                    onPressed: () async {
                      await startForegroundService();
                    },
                    child: const Text("🚀 ForegroundService起動"),
                  ),
                ],
              ),
            ),

            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(
                '⏱ 次回取得まで: ${remaining.toStringAsFixed(3)} 秒',
                style: const TextStyle(fontSize: 12, color: Colors.grey),
              ),
            ),
            const Divider(),
            const Text('📋 保存済みWi-Fi情報一覧'),
            Expanded(
              child: ListView.builder(
                itemCount: _records.length,
                itemBuilder: (BuildContext context, int index) {
                  final WifiCoordinate item = _records[index];
                  return ListTile(
                    title: Text('${item.date} ${item.time}'),
                    subtitle: Text('SSID: ${item.ssid}\nLat: ${item.latitude}, Lng: ${item.longitude}'),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> sendWifiLocationFromKotlin() async {
    const MethodChannel methodChannel = MethodChannel('com.example.flutter_background_sendport/bg');

    try {
      if (!Platform.isAndroid) return;

      final String? result = await methodChannel.invokeMethod<String>('getCurrentWifiLocation');
      debugPrint('✅ Kotlinからの結果: $result');

      if (result != null) {
        final Map<String, dynamic> parsed = json.decode(result) as Map<String, dynamic>;
        final Map<String, String> data = parsed.map((String k, v) => MapEntry(k, v.toString()));

        debugPrint('📦 Dartで解析済みデータ: $data');

        await _saveToIsar(data);
        await _loadRecords();
        _lastSentTime = DateTime.now(); // ✅ 時刻更新
      }
    } on MissingPluginException catch (e) {
      debugPrint('❌ MissingPluginException: ${e.message}');
    } on PlatformException catch (e) {
      debugPrint('⚠️ PlatformException: ${e.message}');
    }
  }
}

class BackgroundReceivePortSingleton {
  BackgroundReceivePortSingleton._();

  static final BackgroundReceivePortSingleton instance = BackgroundReceivePortSingleton._();

  SendPort? _sendPort;

  void register(SendPort port) {
    _sendPort = port;
  }

  SendPort? get port => _sendPort;
}

@pragma('vm:entry-point')
Future<void> backgroundHandler(dynamic data) async {
  debugPrint('📡 backgroundHandler 呼ばれました！');
  debugPrint('📡 backgroundHandler: $data');

  final SendPort? sendPort = BackgroundReceivePortSingleton.instance.port;
  if (sendPort != null) {
    if (data is Map) {
      sendPort.send('[backgroundHandler] ${jsonEncode(data)}');
    } else {
      sendPort.send('[backgroundHandler] $data');
    }
  } else {
    debugPrint('⚠️ SendPort not found');
  }
}

Map<String, String>? _extractData(String message) {
  final RegExp regex = RegExp(r'\{.*\}');
  final RegExpMatch? match = regex.firstMatch(message);
  if (match == null) {
    return null;
  }

  try {
    final String jsonText = match.group(0)!;
    final Map<String, dynamic> decoded = Map<String, dynamic>.from(jsonDecode(jsonText) as Map<dynamic, dynamic>);
    final Map<String, String> parsed = decoded.map((String key, value) => MapEntry(key, value.toString()));
    debugPrint('📦 パース結果: $parsed');
    return parsed;
  } catch (e) {
    debugPrint('❌ JSONパースエラー: $e');
    return null;
  }
}

Future<void> startForegroundService() async {
  const MethodChannel methodChannel = MethodChannel('com.example.flutter_background_sendport/bg');

  try {
    final result = await methodChannel.invokeMethod<String>('startForegroundService');
    debugPrint('🚀 ForegroundService 起動結果: $result');
  } on PlatformException catch (e) {
    debugPrint('❌ ForegroundService 起動エラー: ${e.message}');
  }
}
