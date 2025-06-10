import 'package:pigeon/pigeon.dart';

@HostApi()
abstract class WifiLocationServiceApi {
  void startService();

  void stopService();
}
