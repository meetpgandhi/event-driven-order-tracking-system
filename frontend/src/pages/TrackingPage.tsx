import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiClient } from '../services/apiClient';
import type { CustomerTrackingData } from '../types';
import { StageTimeline } from '../components/StageTimeline';
import { StageBadge } from '../components/StageBadge';
import { Search, Clock, User, Truck, ShieldAlert, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

export const TrackingPage: React.FC = () => {
  const { orderReference } = useParams<{ orderReference?: string }>();
  const navigate = useNavigate();

  const [searchInput, setSearchInput] = useState(orderReference || '');
  const [trackingData, setTrackingData] = useState<CustomerTrackingData | null>(null);
  const [loading, setLoading] = useState(false);
  const [notFound, setNotFound] = useState(false);

  const fetchTracking = async (ref: string) => {
    if (!ref.trim()) return;
    setLoading(true);
    setNotFound(false);

    try {
      const response = await apiClient.get<CustomerTrackingData>(`/tracking/${ref.trim().toUpperCase()}`);
      setTrackingData(response.data);
    } catch (err: any) {
      if (err.response?.status === 404) {
        setNotFound(true);
        setTrackingData(null);
      } else {
        toast.error('Failed to retrieve tracking details');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (orderReference) {
      setSearchInput(orderReference);
      fetchTracking(orderReference);

      // Lightweight polling every 3 seconds for active delivery updates (satisfies 5s SLA)
      const interval = setInterval(() => {
        apiClient.get<CustomerTrackingData>(`/tracking/${orderReference.trim().toUpperCase()}`)
          .then(res => setTrackingData(res.data))
          .catch(() => {});
      }, 3000);

      return () => clearInterval(interval);
    }
  }, [orderReference]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchInput.trim()) return;
    navigate(`/track/${searchInput.trim().toUpperCase()}`);
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 sm:py-12">
      {/* Search Header */}
      <div className="text-center max-w-2xl mx-auto mb-8">
        <h1 className="text-3xl font-black text-slate-900 tracking-tight sm:text-4xl">
          Real-Time Shipment Tracking
        </h1>
        <p className="mt-2 text-sm text-slate-500">
          Enter your order reference code below to view live milestone updates and delivery progress.
        </p>

        <form onSubmit={handleSearch} className="mt-6 flex items-center max-w-md mx-auto relative shadow-md shadow-slate-200/50 rounded-2xl">
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="e.g. ORD-2026-0005"
            className="w-full pl-11 pr-28 py-3 text-sm font-mono uppercase bg-white border border-slate-300 rounded-2xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition"
          />
          <Search className="w-4 h-4 text-slate-400 absolute left-4" />
          <button
            type="submit"
            disabled={loading}
            className="absolute right-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium text-xs rounded-xl shadow-xs transition flex items-center space-x-1"
          >
            {loading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <span>Track</span>}
          </button>
        </form>

        {/* Quick Sample Links */}
        <div className="mt-3 flex items-center justify-center space-x-2 text-xs text-slate-400">
          <span>Try:</span>
          {['ORD-2026-0001', 'ORD-2026-0005', 'ORD-2026-0006', 'ORD-2026-0007'].map((demoRef) => (
            <button
              key={demoRef}
              type="button"
              onClick={() => navigate(`/track/${demoRef}`)}
              className="text-blue-600 hover:underline font-mono text-[11px]"
            >
              {demoRef}
            </button>
          ))}
        </div>
      </div>

      {/* Loading state */}
      {loading && !trackingData && (
        <div className="p-16 text-center text-slate-400 bg-white rounded-3xl border border-slate-200 shadow-xs">
          <Loader2 className="w-8 h-8 animate-spin mx-auto text-blue-500 mb-2" />
          <p className="text-sm font-medium">Fetching order status...</p>
        </div>
      )}

      {/* Not Found state */}
      {notFound && (
        <div className="p-12 text-center bg-white rounded-3xl border border-slate-200 shadow-xs max-w-md mx-auto">
          <ShieldAlert className="w-12 h-12 text-rose-500 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-900">Shipment Not Found</h3>
          <p className="text-xs text-slate-500 mt-1">
            No active shipment was found matching reference <span className="font-mono font-bold text-slate-800">{searchInput}</span>. Please double-check your order ID.
          </p>
        </div>
      )}

      {/* Active Order Tracking View */}
      {trackingData && (
        <div className="space-y-6">
          {/* Main Status Card */}
          <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 border-b border-slate-100 gap-4">
              <div>
                <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Order Reference</span>
                <div className="text-2xl font-black text-slate-900 tracking-tight font-mono mt-0.5">
                  {trackingData.orderReference}
                </div>
                <div className="text-xs text-slate-500 mt-1">
                  Recipient: <span className="font-semibold text-slate-800">{trackingData.customerName}</span>
                </div>
              </div>

              <div className="sm:text-right">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-400 block mb-1">Current Milestone</span>
                <StageBadge stage={trackingData.currentStage} size="lg" />
              </div>
            </div>

            {/* Visual 8-Stage Progress Bar */}
            <div className="mt-4">
              <StageTimeline currentStage={trackingData.currentStage} />
            </div>

            {/* Additional details grid */}
            <div className="mt-4 pt-6 border-t border-slate-100 grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs text-slate-600">
              {trackingData.assignedAgentName && (
                <div className="flex items-center space-x-2 bg-slate-50 p-3 rounded-xl border border-slate-200/60">
                  <User className="w-4 h-4 text-blue-600 flex-shrink-0" />
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Assigned Agent</span>
                    <span className="font-semibold text-slate-900">{trackingData.assignedAgentName}</span>
                  </div>
                </div>
              )}

              {trackingData.carrierId && (
                <div className="flex items-center space-x-2 bg-slate-50 p-3 rounded-xl border border-slate-200/60">
                  <Truck className="w-4 h-4 text-indigo-600 flex-shrink-0" />
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Carrier Partner</span>
                    <span className="font-semibold text-slate-900">{trackingData.carrierId.toUpperCase()}</span>
                  </div>
                </div>
              )}

              <div className="flex items-center space-x-2 bg-slate-50 p-3 rounded-xl border border-slate-200/60">
                <Clock className="w-4 h-4 text-slate-500 flex-shrink-0" />
                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-bold">Last Updated</span>
                  <span className="font-semibold text-slate-900">
                    {new Date(trackingData.updatedAt).toLocaleTimeString()}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Chronological Event Timeline */}
          <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs">
            <h3 className="text-base font-bold text-slate-900 tracking-tight mb-4 flex items-center space-x-2">
              <Clock className="w-4 h-4 text-blue-600" />
              <span>Fulfilment Activity History</span>
            </h3>

            <div className="space-y-4">
              {trackingData.timeline.map((item) => (
                <div key={item.id} className="relative flex items-start space-x-3 text-xs">
                  {/* Timeline connector dot */}
                  <div className="mt-1 w-2.5 h-2.5 rounded-full bg-blue-600 flex-shrink-0 ring-4 ring-blue-50" />
                  
                  <div className="flex-1 bg-slate-50/60 p-3 rounded-xl border border-slate-100">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-slate-900 text-xs">
                        {item.newStageDisplayName}
                      </span>
                      <span className="text-slate-400 font-mono text-[11px]">
                        {new Date(item.timestamp).toLocaleString()}
                      </span>
                    </div>

                    <div className="text-slate-500 mt-1 flex items-center space-x-2">
                      <span>Attributed to: <strong className="text-slate-700">{item.actorId}</strong></span>
                      <span className="px-1.5 py-0.5 text-[9px] font-bold rounded bg-slate-200 text-slate-700">
                        {item.actorType}
                      </span>
                    </div>

                    {item.reasonCode && (
                      <div className="mt-1 text-rose-600 font-medium">
                        Reason: {item.reasonCode}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
