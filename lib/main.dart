import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(const ThumbBreakApp());

class ThumbBreakApp extends StatelessWidget {
  const ThumbBreakApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ThumbBreak',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: Colors.black,
        colorScheme: const ColorScheme.dark(primary: Colors.white),
        useMaterial3: true,
      ),
      home: const HomeScreen(),
    );
  }
}

/// Snapshot of what the native accessibility service has recorded today.
class BreakState {
  final int instagram;
  final int youtube;
  final int limit;
  final bool serviceEnabled;

  const BreakState({
    this.instagram = 0,
    this.youtube = 0,
    this.limit = 20,
    this.serviceEnabled = false,
  });

  int get total => instagram + youtube;
  bool get limitReached => total >= limit;

  factory BreakState.fromMap(Map<dynamic, dynamic> m, {bool? serviceEnabled}) {
    return BreakState(
      instagram: m['instagram'] as int? ?? 0,
      youtube: m['youtube'] as int? ?? 0,
      limit: m['limit'] as int? ?? 20,
      serviceEnabled: serviceEnabled ?? m['serviceEnabled'] as bool? ?? false,
    );
  }
}

class NativeBridge {
  static const _control = MethodChannel('thumbbreak/control');
  static const _events = EventChannel('thumbbreak/state');

  static Future<BreakState> getState() async {
    final m = await _control.invokeMapMethod<String, dynamic>('getState');
    return BreakState.fromMap(m ?? const {});
  }

  static Future<void> setLimit(int limit) =>
      _control.invokeMethod('setLimit', {'limit': limit});

  static Future<void> openAccessibilitySettings() =>
      _control.invokeMethod('openAccessibilitySettings');

  static Stream<Map<dynamic, dynamic>> stateStream() =>
      _events.receiveBroadcastStream().cast<Map<dynamic, dynamic>>();
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with WidgetsBindingObserver {
  BreakState _state = const BreakState();
  StreamSubscription<Map<dynamic, dynamic>>? _sub;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refresh();
    _sub = NativeBridge.stateStream().listen((m) {
      setState(() => _state =
          BreakState.fromMap(m, serviceEnabled: _state.serviceEnabled));
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _sub?.cancel();
    super.dispose();
  }

  // Coming back from Accessibility settings — re-check if the service is on.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _refresh();
  }

  Future<void> _refresh() async {
    final s = await NativeBridge.getState();
    if (mounted) setState(() => _state = s);
  }

  Future<void> _changeLimit(int delta) async {
    final next = (_state.limit + delta).clamp(1, 500);
    await NativeBridge.setLimit(next);
    _refresh();
  }

  @override
  Widget build(BuildContext context) {
    final s = _state;
    final progress = (s.total / s.limit).clamp(0.0, 1.0);
    final muted = Colors.white54;

    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('ThumbBreak',
                  style: TextStyle(color: muted, letterSpacing: 1.5)),
              if (!s.serviceEnabled) ...[
                const SizedBox(height: 24),
                _EnableBanner(onTap: NativeBridge.openAccessibilitySettings),
              ],
              const Spacer(),
              Text(
                '${s.total}',
                style: TextStyle(
                  fontSize: 120,
                  fontWeight: FontWeight.w200,
                  height: 1,
                  color: s.limitReached ? Colors.redAccent : Colors.white,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                s.limitReached
                    ? 'Limit reached. Reels & Shorts are blocked.'
                    : 'of ${s.limit} reels & shorts today',
                style: TextStyle(color: muted, fontSize: 16),
              ),
              const SizedBox(height: 24),
              ClipRRect(
                borderRadius: BorderRadius.circular(2),
                child: LinearProgressIndicator(
                  value: progress,
                  minHeight: 3,
                  backgroundColor: Colors.white12,
                  color: s.limitReached ? Colors.redAccent : Colors.white,
                ),
              ),
              const SizedBox(height: 16),
              Text('Instagram ${s.instagram}   ·   YouTube ${s.youtube}',
                  style: TextStyle(color: muted, fontSize: 13)),
              const Spacer(),
              Row(
                children: [
                  Text('Daily limit', style: TextStyle(color: muted)),
                  const Spacer(),
                  _StepButton(icon: Icons.remove, onTap: () => _changeLimit(-5)),
                  SizedBox(
                    width: 56,
                    child: Text('${s.limit}',
                        textAlign: TextAlign.center,
                        style: const TextStyle(fontSize: 20)),
                  ),
                  _StepButton(icon: Icons.add, onTap: () => _changeLimit(5)),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _EnableBanner extends StatelessWidget {
  final VoidCallback onTap;
  const _EnableBanner({required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          border: Border.all(color: Colors.white24),
          borderRadius: BorderRadius.circular(12),
        ),
        child: const Text(
          'Tap to enable ThumbBreak in Accessibility settings. '
          'It\'s needed to count and block Reels & Shorts.',
          style: TextStyle(fontSize: 13, height: 1.4),
        ),
      ),
    );
  }
}

class _StepButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback onTap;
  const _StepButton({required this.icon, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      onPressed: onTap,
      icon: Icon(icon, size: 20),
      style: IconButton.styleFrom(
        side: const BorderSide(color: Colors.white24),
      ),
    );
  }
}
