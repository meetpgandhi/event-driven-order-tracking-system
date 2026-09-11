import React, { useEffect, useState } from 'react';
import { apiClient } from '../services/apiClient';
import type { AgentOrder, Stage } from '../types';
import { StageBadge } from '../components/StageBadge';
import { Truck, RefreshCw, KeyRound, AlertTriangle, CheckCircle2, ChevronRight, Phone, User, Calendar, X, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

export const AgentDashboard: React.FC = () => {
  const [orders, setOrders] = useState<AgentOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedOrder, setSelectedOrder] = useState<AgentOrder | null>(null);

  // Transition form state
  const [targetStage, setTargetStage] = useState<Stage | ''>('');
  const [reasonCode, setReasonCode] = useState<string>('RECIPIENT_ABSENT');
  const [submitting, setSubmitting] = useState(false);

  // OTP modal state
  const [otpModalOpen, setOtpModalOpen] = useState(false);
  const [otpInput, setOtpInput] = useState('');
  const [generatedOtp, setGeneratedOtp] = useState<string | null>(null);
  const [otpLoading, setOtpLoading] = useState(false);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const response = await apiClient.get<AgentOrder[]>('/agent/orders');
      setOrders(response.data);
    } catch (err: any) {
      toast.error('Failed to load assigned orders');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const openTransitionModal = (order: AgentOrder) => {
    setSelectedOrder(order);
    setTargetStage(order.availableNextStages[0] || '');
    setReasonCode('RECIPIENT_ABSENT');
    setGeneratedOtp(null);
    setOtpInput('');
  };

  const handleGenerateOtp = async () => {
    if (!selectedOrder) return;
    setOtpLoading(true);
    try {
      const response = await apiClient.post(`/agent/orders/${selectedOrder.id}/otp/generate`);
      setGeneratedOtp(response.data.debugOtp);
      toast.success('OTP sent to customer. Enter code below to confirm delivery.');
    } catch (err: any) {
      toast.error(err.response?.data?.detail || 'Failed to generate OTP');
    } finally {
      setOtpLoading(false);
    }
  };

  const handleVerifyOtp = async () => {
    if (!selectedOrder || !otpInput) {
      toast.error('Please enter the 6-digit OTP');
      return;
    }
    setOtpLoading(true);
    try {
      await apiClient.post(`/agent/orders/${selectedOrder.id}/otp/verify`, { otpCode: otpInput });
      toast.success('OTP successfully verified! Completing delivery...');
      // Execute the DELIVERED transition
      await executeTransition('DELIVERED', undefined);
      setOtpModalOpen(false);
    } catch (err: any) {
      toast.error(err.response?.data?.detail || 'Invalid or expired OTP');
    } finally {
      setOtpLoading(false);
    }
  };

  const handleTransitionSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedOrder || !targetStage) return;

    if (targetStage === 'DELIVERED') {
      setOtpModalOpen(true);
      return;
    }

    const payloadReason = targetStage === 'FAILED_ATTEMPT' ? reasonCode : undefined;
    await executeTransition(targetStage, payloadReason);
  };

  const executeTransition = async (stage: Stage, reason?: string) => {
    if (!selectedOrder) return;
    setSubmitting(true);
    try {
      await apiClient.post(`/agent/orders/${selectedOrder.id}/transition`, {
        newStage: stage,
        reasonCode: reason
      });
      toast.success(`Order ${selectedOrder.orderReference} updated to ${stage}`);
      setSelectedOrder(null);
      await fetchOrders();
    } catch (err: any) {
      toast.error(err.response?.data?.detail || 'Stage transition failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-200">
        <div>
          <div className="flex items-center space-x-2 text-blue-600">
            <Truck className="w-5 h-5" />
            <span className="text-xs font-bold uppercase tracking-wider">Field Delivery App</span>
          </div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900 mt-1">
            My Assigned Shipments
          </h1>
          <p className="text-sm text-slate-500">
            Select an order to advance stages, record exceptions, or verify customer OTP.
          </p>
        </div>

        <button
          onClick={fetchOrders}
          disabled={loading}
          className="self-start sm:self-auto flex items-center space-x-1.5 px-3.5 py-2 text-xs font-semibold bg-white hover:bg-slate-50 border border-slate-200 rounded-lg text-slate-700 shadow-xs transition"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh Queue</span>
        </button>
      </div>

      {/* Orders List */}
      <div className="mt-6 space-y-4">
        {loading && orders.length === 0 ? (
          <div className="p-12 text-center text-slate-400">
            <Loader2 className="w-8 h-8 animate-spin mx-auto mb-2 text-blue-500" />
            <p className="text-sm">Loading assigned shipments...</p>
          </div>
        ) : orders.length === 0 ? (
          <div className="p-12 text-center bg-white rounded-xl border border-slate-200 shadow-xs">
            <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto mb-2" />
            <h3 className="text-base font-bold text-slate-800">All clear!</h3>
            <p className="text-sm text-slate-500">You currently have no active deliveries assigned.</p>
          </div>
        ) : (
          orders.map((order) => (
            <div
              key={order.id}
              className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs hover:shadow-md hover:border-blue-300 transition flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div className="space-y-2">
                <div className="flex items-center space-x-3">
                  <span className="text-base font-black text-slate-900 tracking-tight font-mono">
                    {order.orderReference}
                  </span>
                  <StageBadge stage={order.currentStage} />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-1 text-xs text-slate-600">
                  <div className="flex items-center space-x-1.5">
                    <User className="w-3.5 h-3.5 text-slate-400" />
                    <span className="font-semibold text-slate-900">{order.customerName}</span>
                  </div>
                  <div className="flex items-center space-x-1.5">
                    <Phone className="w-3.5 h-3.5 text-slate-400" />
                    <span>{order.customerPhone || 'N/A'}</span>
                  </div>
                  {order.attemptCount > 0 && (
                    <div className="flex items-center space-x-1.5 text-rose-600">
                      <AlertTriangle className="w-3.5 h-3.5" />
                      <span>Attempts: {order.attemptCount} / {order.maxAttempts}</span>
                    </div>
                  )}
                  {order.nextDeliverySlot && (
                    <div className="flex items-center space-x-1.5 text-blue-600">
                      <Calendar className="w-3.5 h-3.5" />
                      <span>Rescheduled: {new Date(order.nextDeliverySlot).toLocaleString()}</span>
                    </div>
                  )}
                </div>
              </div>

              <div className="flex items-center space-x-2 pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-100">
                {order.availableNextStages.length > 0 ? (
                  <button
                    onClick={() => openTransitionModal(order)}
                    className="w-full sm:w-auto flex items-center justify-center space-x-1.5 px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs rounded-xl shadow-xs shadow-blue-500/20 transition"
                  >
                    <span>Update Stage</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                ) : (
                  <span className="text-xs text-slate-400 font-medium italic">Terminal State</span>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {/* Transition Modal */}
      {selectedOrder && !otpModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100">
              <div>
                <h3 className="text-base font-bold text-slate-900">Update Shipment Stage</h3>
                <p className="text-xs text-slate-500 font-mono mt-0.5">{selectedOrder.orderReference}</p>
              </div>
              <button
                onClick={() => setSelectedOrder(null)}
                className="p-1 text-slate-400 hover:text-slate-600 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleTransitionSubmit} className="mt-4 space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                  Select Next Stage
                </label>
                <select
                  value={targetStage}
                  onChange={(e) => setTargetStage(e.target.value as Stage)}
                  className="w-full text-sm font-semibold border border-slate-300 rounded-xl px-3.5 py-2.5 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
                >
                  {selectedOrder.availableNextStages.map((st) => (
                    <option key={st} value={st}>
                      {st}
                    </option>
                  ))}
                </select>
              </div>

              {targetStage === 'FAILED_ATTEMPT' && (
                <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200 space-y-2">
                  <label className="block text-xs font-bold text-amber-900 uppercase tracking-wider">
                    Exception Reason Code (Required)
                  </label>
                  <select
                    value={reasonCode}
                    onChange={(e) => setReasonCode(e.target.value)}
                    className="w-full text-xs font-semibold border border-amber-300 rounded-lg px-3 py-2 bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-amber-500"
                  >
                    <option value="RECIPIENT_ABSENT">RECIPIENT_ABSENT (Customer not at address)</option>
                    <option value="WRONG_ADDRESS">WRONG_ADDRESS (Incorrect destination street/apt)</option>
                    <option value="ACCESS_DENIED">ACCESS_DENIED (Gated entry / security restricted)</option>
                  </select>
                </div>
              )}

              {targetStage === 'DELIVERED' && (
                <div className="p-3.5 rounded-xl bg-blue-50 border border-blue-200 text-xs text-blue-800 flex items-start space-x-2">
                  <KeyRound className="w-4 h-4 text-blue-600 mt-0.5 flex-shrink-0" />
                  <div>
                    <span className="font-bold">Proof of Delivery OTP Required:</span> You will be prompted to generate and verify the 6-digit customer token before finalizing delivery.
                  </div>
                </div>
              )}

              <div className="flex items-center space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setSelectedOrder(null)}
                  className="w-1/2 py-2.5 px-4 text-xs font-semibold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="w-1/2 py-2.5 px-4 text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-xs shadow-blue-500/20 disabled:opacity-50 transition flex items-center justify-center space-x-1.5"
                >
                  {submitting ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <span>Continue</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* OTP Modal for DELIVERED stage */}
      {otpModalOpen && selectedOrder && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center space-x-2 text-emerald-600">
                <KeyRound className="w-5 h-5" />
                <h3 className="text-base font-bold text-slate-900">OTP Delivery Verification</h3>
              </div>
              <button
                onClick={() => setOtpModalOpen(false)}
                className="p-1 text-slate-400 hover:text-slate-600 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="mt-4 space-y-4">
              <p className="text-xs text-slate-500">
                A 6-digit verification code must be confirmed with the recipient for order{' '}
                <span className="font-mono font-bold text-slate-900">{selectedOrder.orderReference}</span>.
              </p>

              {generatedOtp ? (
                <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-center">
                  <div className="text-xs text-emerald-700 font-semibold">Active OTP (Valid 10 min):</div>
                  <div className="text-2xl font-black tracking-widest text-emerald-900 font-mono my-1">
                    {generatedOtp}
                  </div>
                  <div className="text-[11px] text-emerald-600">Simulated SMS/Email delivery token</div>
                </div>
              ) : (
                <button
                  type="button"
                  onClick={handleGenerateOtp}
                  disabled={otpLoading}
                  className="w-full py-2.5 px-4 bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold rounded-xl flex items-center justify-center space-x-2 transition"
                >
                  {otpLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <span>Dispatch OTP to Customer</span>}
                </button>
              )}

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                  Enter 6-Digit Code
                </label>
                <input
                  type="text"
                  maxLength={6}
                  value={otpInput}
                  onChange={(e) => setOtpInput(e.target.value.replace(/\D/g, ''))}
                  placeholder="123456"
                  className="w-full text-center text-xl font-mono tracking-widest font-black border border-slate-300 rounded-xl py-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div className="flex items-center space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setOtpModalOpen(false)}
                  className="w-1/2 py-2.5 px-4 text-xs font-semibold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleVerifyOtp}
                  disabled={otpLoading || otpInput.length !== 6}
                  className="w-1/2 py-2.5 px-4 text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl shadow-xs shadow-emerald-500/20 disabled:opacity-50 transition flex items-center justify-center space-x-1.5"
                >
                  {otpLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <span>Verify & Confirm</span>}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
