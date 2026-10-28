import React, { useState, useEffect, useRef } from 'react';
import {
  ShieldCheck,
  AlertTriangle,
  Activity,
  Volume2,
  VolumeX,
  RotateCcw,
  Sparkles,
  CheckCircle2,
  Radio,
  Sliders,
  Clock,
  TrendingDown
} from 'lucide-react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ReferenceLine,
  BarChart,
  Bar,
  Cell,
  CartesianGrid
} from 'recharts';

export default function App() {
  const [activeTab, setActiveTab] = useState('home'); // 'home' | 'telemetry'
  const [wsConnected, setWsConnected] = useState(false);
  const [audioEnabled, setAudioEnabled] = useState(true);

  const [telemetry, setTelemetry] = useState({
    timestamp: '--:--:--',
    hardware_connected: false,
    port: 'SIMULATION',
    state: 'IDLE',
    trigger_reason: 'None',
    hf_energy: 1.45,
    recent_peak: 1.85,
    drop_delta: 0.4,
    peak_threshold: 8.0,
    low_threshold: 2.2,
    cliff_delta_threshold: 4.5,
    csi_altitude: 14.16,
    subcarrier_energies: [1.2, 1.5, 1.1, 1.6, 1.4],
    logs: []
  });

  // Live threshold editor state
  const [customPeak, setCustomPeak] = useState(8.0);
  const [customLow, setCustomLow] = useState(2.2);
  const [customCliff, setCustomCliff] = useState(4.5);

  const [history, setHistory] = useState([]);
  const wsRef = useRef(null);
  const lastAlertStateRef = useRef('IDLE');

  // Loud Dual-Tone Emergency Siren Synthesizer (Web Audio API)
  const triggerSirenSound = () => {
    if (!audioEnabled) return;
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      const ctx = new AudioCtx();
      [0, 0.4, 0.8, 1.2, 1.6].forEach((offset, idx) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sawtooth';
        const freq = idx % 2 === 0 ? 1150 : 850;
        osc.frequency.setValueAtTime(freq, ctx.currentTime + offset);
        gain.gain.setValueAtTime(0.3, ctx.currentTime + offset);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + offset + 0.35);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(ctx.currentTime + offset);
        osc.stop(ctx.currentTime + offset + 0.35);
      });
    } catch (e) {
      console.error('Audio play failed:', e);
    }
  };

  useEffect(() => {
    let reconnectTimer;
    const connectWs = () => {
      const ws = new WebSocket('ws://127.0.0.1:8000/ws');
      wsRef.current = ws;

      ws.onopen = () => setWsConnected(true);
      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          setTelemetry(data);
          setHistory((prev) => {
            const next = [
              ...prev,
              {
                time: data.timestamp,
                hf_energy: data.hf_energy,
                peak_threshold: data.peak_threshold,
                low_threshold: data.low_threshold
              }
            ];
            return next.slice(-60);
          });

          if (data.state === 'CONFIRMED_FALL' && lastAlertStateRef.current !== 'CONFIRMED_FALL') {
            triggerSirenSound();
          }
          lastAlertStateRef.current = data.state;
        } catch (err) {
          console.error(err);
        }
      };

      ws.onclose = () => {
        setWsConnected(false);
        reconnectTimer = setTimeout(connectWs, 2000);
      };
    };

    connectWs();
    return () => {
      clearTimeout(reconnectTimer);
      if (wsRef.current) wsRef.current.close();
    };
  }, [audioEnabled]);

  const handleReset = async () => {
    if (wsConnected) {
      await fetch('http://127.0.0.1:8000/api/reset', { method: 'POST' }).catch(() => {});
    }
    setTelemetry((prev) => ({ ...prev, state: 'IDLE', drop_delta: 0.0 }));
    lastAlertStateRef.current = 'IDLE';
  };

  const handleSaveThresholds = async () => {
    if (wsConnected) {
      await fetch('http://127.0.0.1:8000/api/thresholds', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          peak_threshold: Number(customPeak),
          low_stillness_max: Number(customLow),
          cliff_drop_delta: Number(customCliff)
        })
      }).catch(() => {});
    }
  };

  const triggerDemo = async (type) => {
    if (wsConnected) {
      await fetch(`http://127.0.0.1:8000/api/simulate/${type}`, { method: 'POST' }).catch(() => {});
      return;
    }
    if (type === 'fall') {
      triggerSirenSound();
      setTelemetry((prev) => ({
        ...prev,
        state: 'CONFIRMED_FALL',
        trigger_reason: 'Trigger 2: Rapid Graph Cliff Drop',
        hf_energy: 1.15,
        recent_peak: 10.2,
        drop_delta: 9.05
      }));
    }
  };

  const getStatusMeta = () => {
    switch (telemetry.state) {
      case 'CONFIRMED_FALL':
        return {
          title: 'CRITICAL: FALL DETECTED!',
          subtitle: `Triggered via ${telemetry.trigger_reason || 'Rapid Peak-to-Stillness Drop'}`,
          bg: 'bg-red-950/60 border-red-500/60',
          ring: 'bg-red-600 text-white shadow-red-900/50',
          badge: 'bg-red-500/20 text-red-300 border border-red-500/40',
          label: 'SIREN ACTIVE — EMERGENCY'
        };
      case 'IMPACT_DETECTED':
        return {
          title: 'Peak Motion Detected — Checking Stillness...',
          subtitle: `Peak reached ${telemetry.recent_peak}. Waiting for energy to drop below ${telemetry.low_threshold} to confirm fall.`,
          bg: 'bg-amber-950/50 border-amber-500/50',
          ring: 'bg-amber-500 text-slate-950 shadow-amber-900/50',
          badge: 'bg-amber-500/20 text-amber-300 border border-amber-500/40',
          label: 'EVALUATING DROP'
        };
      case 'FALSE_ALARM':
        return {
          title: 'Normal Walking / Continuous Activity',
          subtitle: 'High motion peak detected, but person did not drop to low stillness.',
          bg: 'bg-blue-950/50 border-blue-500/40',
          ring: 'bg-blue-600 text-white shadow-blue-900/50',
          badge: 'bg-blue-500/20 text-blue-300 border border-blue-500/40',
          label: 'WALKING FILTERED'
        };
      default:
        return {
          title: 'Room is Safe & Normal',
          subtitle: 'Dual-Trigger CSI Engine monitoring Peak-to-Stillness & Graph Cliff Drops.',
          bg: 'bg-slate-900 border-slate-800',
          ring: 'bg-emerald-500 text-slate-950 shadow-emerald-950/50',
          badge: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30',
          label: 'MONITORING ACTIVE'
        };
    }
  };

  const meta = getStatusMeta();
  const subcarrierChartData = [10, 20, 30, 40, 50].map((sc, idx) => ({
    name: `SC ${sc}`,
    energy: telemetry.subcarrier_energies?.[idx] || 1.2
  }));

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {/* Header */}
      <header className="bg-slate-900/90 backdrop-blur border-b border-slate-800 sticky top-0 z-20">
        <div className="max-w-5xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-blue-600 flex items-center justify-center text-white shadow-sm">
              <Radio className="w-5 h-5" />
            </div>
            <div>
              <h1 className="font-semibold text-white tracking-tight leading-none text-lg">
                Sentinel CSI
              </h1>
              <p className="text-xs text-slate-400 mt-0.5">
                Privacy-Safe Through-Wall Wi-Fi Radar
              </p>
            </div>
          </div>

          <div className="flex items-center bg-slate-800 p-1 rounded-full border border-slate-700">
            <button
              onClick={() => setActiveTab('home')}
              className={`px-5 py-1.5 rounded-full text-sm font-medium transition ${
                activeTab === 'home'
                  ? 'bg-slate-950 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Live Monitor
            </button>
            <button
              onClick={() => setActiveTab('telemetry')}
              className={`px-5 py-1.5 rounded-full text-sm font-medium transition ${
                activeTab === 'telemetry'
                  ? 'bg-slate-950 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              DSP Telemetry & Logs
            </button>
          </div>

          <div className="flex items-center gap-2.5">
            <span
              className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium ${
                wsConnected
                  ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                  : 'bg-amber-500/15 text-amber-400 border border-amber-500/30'
              }`}
            >
              <span
                className={`w-2 h-2 rounded-full ${
                  wsConnected ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'
                }`}
              />
              {wsConnected ? `Backend (${telemetry.port})` : 'Demo Mode'}
            </span>

            <button
              onClick={() => setAudioEnabled(!audioEnabled)}
              className="p-2 rounded-full border border-slate-700 hover:bg-slate-800 text-slate-300"
              title="Toggle Siren Audio"
            >
              {audioEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-5xl w-full mx-auto px-6 py-8">
        {activeTab === 'home' ? (
          <div className="space-y-6">
            <div
              className={`rounded-3xl border p-8 sm:p-10 transition-all shadow-lg flex flex-col items-center text-center ${meta.bg}`}
            >
              <span className={`px-3.5 py-1 rounded-full text-xs font-semibold tracking-wide uppercase ${meta.badge}`}>
                {meta.label}
              </span>

              <div
                className={`w-28 h-28 rounded-full flex items-center justify-center my-6 shadow-xl transition-transform ${meta.ring}`}
              >
                {telemetry.state === 'CONFIRMED_FALL' ? (
                  <AlertTriangle className="w-14 h-14 animate-bounce" />
                ) : telemetry.state === 'FALSE_ALARM' ? (
                  <CheckCircle2 className="w-14 h-14" />
                ) : (
                  <ShieldCheck className="w-14 h-14" />
                )}
              </div>

              <h2 className="text-3xl sm:text-4xl font-bold text-white tracking-tight">
                {meta.title}
              </h2>
              <p className="text-slate-300 max-w-lg mt-2 text-base">
                {meta.subtitle}
              </p>

              {telemetry.state !== 'IDLE' && (
                <button
                  onClick={handleReset}
                  className="mt-6 inline-flex items-center gap-2 px-6 py-3 rounded-full bg-white text-slate-950 text-sm font-semibold hover:bg-slate-200 transition shadow"
                >
                  <RotateCcw className="w-4 h-4" />
                  Acknowledge & Reset Siren
                </button>
              )}

              {/* Accurate 3-Pillar Summary */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 w-full max-w-2xl mt-8 pt-8 border-t border-slate-800">
                <div className="bg-slate-950/70 rounded-2xl p-4 border border-slate-800">
                  <p className="text-xs text-slate-400 font-medium">Current Activity Score</p>
                  <p className="text-2xl font-bold text-white mt-1">
                    {telemetry.hf_energy}
                  </p>
                  <p className="text-[11px] text-slate-400 mt-0.5">
                    Stillness Zone: &le; {telemetry.low_threshold}
                  </p>
                </div>

                <div className="bg-slate-950/70 rounded-2xl p-4 border border-slate-800">
                  <p className="text-xs text-slate-400 font-medium">Recent 4s Peak</p>
                  <p className="text-2xl font-bold text-white mt-1">
                    {telemetry.recent_peak}
                  </p>
                  <p className="text-[11px] text-slate-400 mt-0.5">
                    Peak Trigger: &ge; {telemetry.peak_threshold}
                  </p>
                </div>

                <div className="bg-slate-950/70 rounded-2xl p-4 border border-slate-800">
                  <p className="text-xs text-slate-400 font-medium">Rapid Graph Drop (&Delta;)</p>
                  <p className="text-2xl font-bold text-emerald-400 mt-1">
                    -{telemetry.drop_delta}
                  </p>
                  <p className="text-[11px] text-slate-400 mt-0.5">
                    Cliff Trigger: &ge; {telemetry.cliff_delta_threshold} Drop
                  </p>
                </div>
              </div>
            </div>

            {/* Judge Controls */}
            <div className="bg-slate-900 rounded-3xl border border-slate-800 p-6 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-slate-800 flex items-center justify-center text-blue-400">
                  <Sparkles className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-white">
                    Exhibition Judge Test Controls
                  </h3>
                  <p className="text-xs text-slate-400">
                    Test the emergency siren or reset the state machine at any time.
                  </p>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-2.5">
                <button
                  onClick={() => triggerDemo('drop')}
                  className="px-4 py-2.5 rounded-full border border-slate-700 text-slate-200 hover:bg-slate-800 text-xs font-semibold transition"
                >
                  Test Walking Spike (No Fall)
                </button>
                <button
                  onClick={() => triggerDemo('fall')}
                  className="px-4 py-2.5 rounded-full bg-red-600 text-white hover:bg-red-500 text-xs font-semibold transition shadow-sm"
                >
                  Trigger Siren Demo
                </button>
              </div>
            </div>
          </div>
        ) : (
          /* TAB 2: TELEMETRY, GRAPH & LIVE THRESHOLD TUNING */
          <div className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Main Activity Graph with Peak & Stillness Lines */}
              <div className="lg:col-span-2 bg-slate-900 rounded-3xl border border-slate-800 p-6">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="font-semibold text-white flex items-center gap-2">
                      <Activity className="w-4 h-4 text-blue-400" />
                      Real-Time CSI Activity Score & Cliff Detector
                    </h3>
                    <p className="text-xs text-slate-400">
                      Triggers siren when score peaks (&ge;{telemetry.peak_threshold}) and drops into stillness (&le;{telemetry.low_threshold}) or drops by -{telemetry.cliff_delta_threshold}
                    </p>
                  </div>
                  <span className="text-xs font-mono bg-slate-800 px-2.5 py-1 rounded-md text-slate-300">
                    CSI Mean: {telemetry.csi_altitude}
                  </span>
                </div>

                <div className="h-64 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={history}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#1E293B" />
                      <XAxis dataKey="time" tick={{ fontSize: 11, fill: '#94A3B8' }} />
                      <YAxis tick={{ fontSize: 11, fill: '#94A3B8' }} domain={[0, 14]} />
                      <Tooltip
                        contentStyle={{ backgroundColor: '#0F172A', borderColor: '#334155', color: '#F8FAFC' }}
                      />
                      <ReferenceLine
                        y={telemetry.peak_threshold}
                        stroke="#EF4444"
                        strokeDasharray="4 4"
                        label={{ value: `Peak (${telemetry.peak_threshold})`, position: 'insideTopRight', fontSize: 11, fill: '#F87171' }}
                      />
                      <ReferenceLine
                        y={telemetry.low_threshold}
                        stroke="#10B981"
                        strokeDasharray="4 4"
                        label={{ value: `Stillness (${telemetry.low_threshold})`, position: 'insideBottomRight', fontSize: 11, fill: '#34D399' }}
                      />
                      <Area
                        type="monotone"
                        dataKey="hf_energy"
                        stroke="#3B82F6"
                        fill="#1D4ED8"
                        fillOpacity={0.35}
                        strokeWidth={2.5}
                        isAnimationActive={false}
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
              </div>

              {/* Live Threshold Tuning Panel */}
              <div className="bg-slate-900 rounded-3xl border border-slate-800 p-6 flex flex-col justify-between">
                <div>
                  <h3 className="font-semibold text-white flex items-center gap-2 mb-1">
                    <Sliders className="w-4 h-4 text-blue-400" />
                    Live Threshold Calibration
                  </h3>
                  <p className="text-xs text-slate-400 mb-5">
                    Adjust trigger numbers instantly to match your room graph.
                  </p>

                  <div className="space-y-4">
                    <div>
                      <div className="flex justify-between text-xs mb-1">
                        <span className="text-slate-300">Peak Threshold (Fall Spike)</span>
                        <span className="font-mono text-red-400 font-semibold">{customPeak}</span>
                      </div>
                      <input
                        type="range"
                        min="4.0"
                        max="15.0"
                        step="0.5"
                        value={customPeak}
                        onChange={(e) => setCustomPeak(e.target.value)}
                        className="w-full accent-blue-500"
                      />
                    </div>

                    <div>
                      <div className="flex justify-between text-xs mb-1">
                        <span className="text-slate-300">Stillness Threshold (Floor Low)</span>
                        <span className="font-mono text-emerald-400 font-semibold">{customLow}</span>
                      </div>
                      <input
                        type="range"
                        min="0.5"
                        max="5.0"
                        step="0.2"
                        value={customLow}
                        onChange={(e) => setCustomLow(e.target.value)}
                        className="w-full accent-emerald-500"
                      />
                    </div>

                    <div>
                      <div className="flex justify-between text-xs mb-1">
                        <span className="text-slate-300">Graph Cliff Drop (&Delta; in 4s)</span>
                        <span className="font-mono text-blue-400 font-semibold">-{customCliff}</span>
                      </div>
                      <input
                        type="range"
                        min="2.5"
                        max="10.0"
                        step="0.5"
                        value={customCliff}
                        onChange={(e) => setCustomCliff(e.target.value)}
                        className="w-full accent-blue-500"
                      />
                    </div>
                  </div>
                </div>

                <button
                  onClick={handleSaveThresholds}
                  className="w-full mt-6 py-2.5 rounded-full bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold transition"
                >
                  Apply Thresholds to Backend
                </button>
              </div>
            </div>

            {/* Event Log Table */}
            <div className="bg-slate-900 rounded-3xl border border-slate-800 p-6">
              <h3 className="font-semibold text-white flex items-center gap-2 mb-4">
                <Clock className="w-4 h-4 text-blue-400" />
                Timestamped Detection Log
              </h3>

              {telemetry.logs && telemetry.logs.length > 0 ? (
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse text-sm">
                    <thead>
                      <tr className="border-b border-slate-800 text-xs text-slate-400 uppercase">
                        <th className="py-2.5 px-3">Timestamp</th>
                        <th className="py-2.5 px-3">Event</th>
                        <th className="py-2.5 px-3">Trigger Rule</th>
                        <th className="py-2.5 px-3">Peak Score</th>
                        <th className="py-2.5 px-3">Low Score</th>
                        <th className="py-2.5 px-3">Drop (&Delta;)</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/60">
                      {telemetry.logs.map((item, idx) => (
                        <tr key={idx} className="hover:bg-slate-800/40">
                          <td className="py-3 px-3 font-mono text-xs text-slate-300">{item.time}</td>
                          <td className="py-3 px-3">
                            <span
                              className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                                item.type === 'CONFIRMED_FALL'
                                  ? 'bg-red-500/20 text-red-300 border border-red-500/30'
                                  : 'bg-blue-500/20 text-blue-300 border border-blue-500/30'
                              }`}
                            >
                              {item.type}
                            </span>
                          </td>
                          <td className="py-3 px-3 text-xs text-slate-300">{item.reason}</td>
                          <td className="py-3 px-3 font-mono text-slate-200">{item.peak_score}</td>
                          <td className="py-3 px-3 font-mono text-emerald-400">{item.low_score}</td>
                          <td className="py-3 px-3 font-mono text-blue-400">-{item.drop_delta}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="text-sm text-slate-400 py-6 text-center">
                  No fall events logged yet. Walk and drop to the floor to trigger the Dual-Trigger Engine.
                </p>
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}