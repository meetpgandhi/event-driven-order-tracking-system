import React, { useEffect, useState } from 'react';
import { apiClient } from '../services/apiClient';
import { createWebSocketClient } from '../services/wsClient';
import type { AdminOrder, DashboardStats, NotificationRule, Stage } from '../types';
import { StageBadge } from '../components/StageBadge';
import {
  Shield, Package, AlertTriangle, RefreshCw, Bell, Plus, Edit2, Trash2,
  TrendingUp, Filter, Radio, X, Loader2
} from 'lucide-react';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, Cell } from 'recharts';
import { toast } from 'sonner';

const STAGES: Stage[] = [
  'ORDER_PLACED', 'PICKING', 'PACKED', 'DISPATCHED',
  'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED_ATTEMPT', 'RETURNED'
];

const COLORS = [
  '#3b82f6', '#6366f1', '#a855f7', '#f59e0b',
  '#0284c7', '#10b981', '#f43f5e', '#64748b'
];

export const AdminDashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [orders, setOrders] = useState<AdminOrder[]>([]);
  const [rules, setRules] = useState<NotificationRule[]>([]);
  const [selectedStage, setSelectedStage] = useState<string>('ALL');
  const [loading, setLoading] = useState(true);
  const [liveEventCount, setLiveEventCount] = useState(0);

  // Notification Rule Modal
  const [ruleModalOpen, setRuleModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<NotificationRule | null>(null);
  const [ruleEventType, setRuleEventType] = useState<string>('ORDER_PLACED');
  const [ruleChannels, setRuleChannels] = useState<string[]>(['EMAIL']);
  const [ruleTemplate, setRuleTemplate] = useState('');
  const [ruleSaving, setRuleSaving] = useState(false);

  const fetchStats = async () => {
    try {
      const res = await apiClient.get<DashboardStats>('/admin/dashboard/stats');
      setStats(res.data);
    } catch {
      // ignore
    }
  };

  const fetchOrders = async () => {
    try {
      const params = selectedStage !== 'ALL' ? { stage: selectedStage } : {};
      const res = await apiClient.get<{ content: AdminOrder[] }>('/admin/orders', { params });
      setOrders(res.data.content || []);
    } catch {
      toast.error('Failed to load orders');
    }
  };

  const fetchRules = async () => {
    try {
      const res = await apiClient.get<NotificationRule[]>('/admin/notifications/rules');
      setRules(res.data);
    } catch {
      // ignore
    }
  };

  const reloadAll = async () => {
    setLoading(true);
    await Promise.all([fetchStats(), fetchOrders(), fetchRules()]);
    setLoading(false);
  };

  useEffect(() => {
    reloadAll();

    // Subscribe to STOMP WebSocket for real-time live events!
    const ws = createWebSocketClient((event) => {
      console.log('⚡ Received live event over WebSocket:', event);
      setLiveEventCount((prev) => prev + 1);
      toast.info(`Live event received: Order ${event.orderReference} -> ${event.newStage}`);
      fetchStats();
      fetchOrders();
    });

    ws.activate();

    return () => {
      ws.deactivate();
    };
  }, []);

  useEffect(() => {
    fetchOrders();
  }, [selectedStage]);

  const handleOpenCreateRule = () => {
    setEditingRule(null);
    setRuleEventType('ORDER_PLACED');
    setRuleChannels(['EMAIL', 'IN_APP']);
    setRuleTemplate('Status update: Your order {{orderReference}} is now {{stage}}.');
    setRuleModalOpen(true);
  };

  const handleOpenEditRule = (rule: NotificationRule) => {
    setEditingRule(rule);
    setRuleEventType(rule.eventType);
    setRuleChannels(rule.channels);
    setRuleTemplate(rule.templateBody || '');
    setRuleModalOpen(true);
  };

  const handleSaveRule = async (e: React.FormEvent) => {
    e.preventDefault();
    setRuleSaving(true);
    try {
      const payload = {
        eventType: ruleEventType,
        channels: ruleChannels,
        templateBody: ruleTemplate,
        active: true
      };

      if (editingRule) {
        await apiClient.put(`/admin/notifications/rules/${editingRule.id}`, payload);
        toast.success('Notification rule updated');
      } else {
        await apiClient.post('/admin/notifications/rules', payload);
        toast.success('Notification rule created');
      }

      setRuleModalOpen(false);
      fetchRules();
    } catch (err: any) {
      toast.error(err.response?.data?.detail || 'Failed to save rule');
    } finally {
      setRuleSaving(false);
    }
  };

  const handleDeactivateRule = async (id: number) => {
    try {
      await apiClient.delete(`/admin/notifications/rules/${id}`);
      toast.success('Rule deactivated');
      fetchRules();
    } catch {
      toast.error('Failed to deactivate rule');
    }
  };

  const toggleChannel = (ch: string) => {
    setRuleChannels((prev) =>
      prev.includes(ch) ? prev.filter((c) => c !== ch) : [...prev, ch]
    );
  };

  // Format chart data
  const chartData = STAGES.map((st) => ({
    name: st.replace(/_/g, ' '),
    count: stats?.ordersByStage[st] || 0
  }));

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-200">
        <div>
          <div className="flex items-center space-x-2 text-indigo-600">
            <Shield className="w-5 h-5" />
            <span className="text-xs font-bold uppercase tracking-wider">Operations Command Hub</span>
            {liveEventCount > 0 && (
              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 animate-pulse">
                <Radio className="w-2.5 h-2.5 mr-1 text-emerald-600" />
                Live WS Active ({liveEventCount} events)
              </span>
            )}
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight mt-1">
            Fulfillment Pipeline Dashboard
          </h1>
          <p className="text-sm text-slate-500">
            Live pipeline queue, 48-hour inactivity monitor, real-time STOMP telemetry, and notification rules.
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={handleOpenCreateRule}
            className="flex items-center space-x-1.5 px-3.5 py-2 text-xs font-semibold bg-slate-900 hover:bg-slate-800 text-white rounded-xl shadow-xs transition"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>New Notification Rule</span>
          </button>
          <button
            onClick={reloadAll}
            disabled={loading}
            className="flex items-center space-x-1.5 px-3.5 py-2 text-xs font-semibold bg-white hover:bg-slate-50 border border-slate-200 rounded-xl text-slate-700 shadow-xs transition"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* KPI Stats Cards */}
      {stats && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
            <div>
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Total Orders</span>
              <div className="text-2xl font-black text-slate-900 mt-1">{stats.totalOrders}</div>
              <span className="text-[11px] text-slate-500 font-medium">All active & fulfilled</span>
            </div>
            <div className="w-11 h-11 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
              <Package className="w-5 h-5" />
            </div>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
            <div>
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Today's Events</span>
              <div className="text-2xl font-black text-indigo-600 mt-1">{stats.todayEventsCount}</div>
              <span className="text-[11px] text-slate-500 font-medium">State changes logged</span>
            </div>
            <div className="w-11 h-11 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <TrendingUp className="w-5 h-5" />
            </div>
          </div>

          <div className={`p-5 rounded-2xl border shadow-xs flex items-center justify-between ${
            stats.staleOrdersCount > 0 ? 'bg-amber-50/70 border-amber-300' : 'bg-white border-slate-200'
          }`}>
            <div>
              <span className="text-xs font-bold text-amber-800 uppercase tracking-wider">48h Inactivity Alert</span>
              <div className="text-2xl font-black text-amber-900 mt-1">{stats.staleOrdersCount}</div>
              <span className="text-[11px] text-amber-700 font-medium">Orders stalled &gt; 48 hours</span>
            </div>
            <div className="w-11 h-11 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center">
              <AlertTriangle className="w-5 h-5" />
            </div>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
            <div>
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Active Rules</span>
              <div className="text-2xl font-black text-emerald-600 mt-1">
                {rules.filter(r => r.active).length}
              </div>
              <span className="text-[11px] text-slate-500 font-medium">Event channels configured</span>
            </div>
            <div className="w-11 h-11 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <Bell className="w-5 h-5" />
            </div>
          </div>
        </div>
      )}

      {/* Orders By Stage Bar Chart */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs">
        <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider mb-4">
          Order Volume by Fulfilment Stage
        </h3>
        <div className="h-64 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 20 }}>
              <XAxis dataKey="name" tick={{ fontSize: 10 }} interval={0} angle={-15} textAnchor="end" />
              <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
              <Tooltip />
              <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                {chartData.map((_, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Live Order Queue Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h3 className="text-base font-bold text-slate-900 tracking-tight">Live Order Queue</h3>
            <p className="text-xs text-slate-500">Real-time status updates broadcast from field agents and carrier webhooks.</p>
          </div>

          <div className="flex items-center space-x-2">
            <Filter className="w-4 h-4 text-slate-400" />
            <select
              value={selectedStage}
              onChange={(e) => setSelectedStage(e.target.value)}
              className="text-xs font-semibold border border-slate-300 rounded-lg px-2.5 py-1.5 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="ALL">All Stages</option>
              {STAGES.map((st) => (
                <option key={st} value={st}>{st}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider">
                <th className="py-3 px-4">Order Ref</th>
                <th className="py-3 px-4">Customer</th>
                <th className="py-3 px-4">Current Stage</th>
                <th className="py-3 px-4">Assigned To</th>
                <th className="py-3 px-4">Attempts</th>
                <th className="py-3 px-4">Last Updated</th>
                <th className="py-3 px-4">Flags</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {orders.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-400">
                    No orders match the selected filter.
                  </td>
                </tr>
              ) : (
                orders.map((order) => (
                  <tr
                    key={order.id}
                    className={`hover:bg-slate-50/60 transition ${
                      order.isStale ? 'bg-amber-50/50' : ''
                    }`}
                  >
                    <td className="py-3.5 px-4 font-mono font-bold text-slate-900">
                      {order.orderReference}
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-slate-900">{order.customerName}</div>
                      <div className="text-[10px] text-slate-400">{order.customerEmail || 'No email'}</div>
                    </td>
                    <td className="py-3.5 px-4">
                      <StageBadge stage={order.currentStage} size="sm" />
                    </td>
                    <td className="py-3.5 px-4 text-slate-600">
                      {order.assignedAgentName || order.carrierId?.toUpperCase() || (
                        <span className="text-slate-400 italic">Unassigned</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-slate-600">
                      {order.attemptCount > 0 ? (
                        <span className="text-rose-600 font-bold">{order.attemptCount} / {order.maxAttempts}</span>
                      ) : (
                        <span>0</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-slate-500 font-mono text-[11px]">
                      {new Date(order.updatedAt).toLocaleTimeString()}
                    </td>
                    <td className="py-3.5 px-4">
                      {order.isStale && (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-rose-100 text-rose-800 border border-rose-200">
                          <AlertTriangle className="w-3 h-3 mr-1" />
                          STALE (&gt;48h)
                        </span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Notification Rules Configuration Section */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs p-6">
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-4">
          <div>
            <h3 className="text-base font-bold text-slate-900">Notification Rules Configuration</h3>
            <p className="text-xs text-slate-500">
              Configure event-triggered messaging channels and templates. Changes apply immediately on the next trigger.
            </p>
          </div>
          <button
            onClick={handleOpenCreateRule}
            className="flex items-center space-x-1 px-3 py-1.5 text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Add Rule</span>
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {rules.map((rule) => (
            <div
              key={rule.id}
              className={`p-4 rounded-xl border transition ${
                rule.active ? 'bg-white border-slate-200 shadow-xs' : 'bg-slate-50 border-slate-200 opacity-60'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="font-mono text-xs font-bold text-slate-900">{rule.eventType}</span>
                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                  rule.active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-200 text-slate-600'
                }`}>
                  {rule.active ? 'Active' : 'Inactive'}
                </span>
              </div>

              <div className="flex items-center space-x-1.5 my-2">
                {rule.channels.map((ch) => (
                  <span key={ch} className="px-2 py-0.5 bg-blue-50 border border-blue-200 text-blue-700 text-[10px] font-bold rounded">
                    {ch}
                  </span>
                ))}
              </div>

              <p className="text-xs text-slate-600 italic line-clamp-2 my-2 font-serif bg-slate-50 p-2 rounded border border-slate-100">
                "{rule.templateBody || 'Default notification template'}"
              </p>

              <div className="flex items-center justify-end space-x-2 pt-2 border-t border-slate-100">
                <button
                  onClick={() => handleOpenEditRule(rule)}
                  className="p-1.5 text-slate-500 hover:text-blue-600 hover:bg-blue-50 rounded"
                  title="Edit Rule"
                >
                  <Edit2 className="w-3.5 h-3.5" />
                </button>
                {rule.active && (
                  <button
                    onClick={() => handleDeactivateRule(rule.id)}
                    className="p-1.5 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded"
                    title="Deactivate"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Rule Create/Edit Modal */}
      {ruleModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-bold text-slate-900">
                {editingRule ? 'Edit Notification Rule' : 'Create Notification Rule'}
              </h3>
              <button onClick={() => setRuleModalOpen(false)} className="p-1 text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveRule} className="mt-4 space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                  Trigger Event Type
                </label>
                <select
                  value={ruleEventType}
                  onChange={(e) => setRuleEventType(e.target.value)}
                  className="w-full text-xs font-semibold border border-slate-300 rounded-lg px-3 py-2 bg-white focus:ring-2 focus:ring-blue-500"
                >
                  {STAGES.map((st) => (
                    <option key={st} value={st}>{st}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                  Dispatch Channels
                </label>
                <div className="flex items-center space-x-2">
                  {['EMAIL', 'SMS', 'IN_APP'].map((ch) => (
                    <button
                      type="button"
                      key={ch}
                      onClick={() => toggleChannel(ch)}
                      className={`px-3 py-1.5 text-xs font-semibold rounded-lg border transition ${
                        ruleChannels.includes(ch)
                          ? 'bg-blue-600 text-white border-blue-600'
                          : 'bg-white text-slate-600 border-slate-300'
                      }`}
                    >
                      {ch}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                  Message Template
                </label>
                <textarea
                  rows={3}
                  value={ruleTemplate}
                  onChange={(e) => setRuleTemplate(e.target.value)}
                  placeholder="Your order {{orderReference}} is now {{stage}}..."
                  className="w-full text-xs border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
                <div className="text-[10px] text-slate-400 mt-1">
                  Available placeholders: {'{{orderReference}}'}, {'{{stage}}'}, {'{{agentName}}'}, {'{{reasonCode}}'}, {'{{deepLink}}'}
                </div>
              </div>

              <div className="flex items-center space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setRuleModalOpen(false)}
                  className="w-1/2 py-2.5 text-xs font-semibold bg-slate-100 text-slate-600 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={ruleSaving || ruleChannels.length === 0}
                  className="w-1/2 py-2.5 text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-xs disabled:opacity-50 transition flex items-center justify-center space-x-1"
                >
                  {ruleSaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <span>Save Rule</span>}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
