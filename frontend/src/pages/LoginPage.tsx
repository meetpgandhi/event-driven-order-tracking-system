import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../services/apiClient';
import { useAuthStore } from '../store/authStore';
import type { UserSession } from '../types';
import { Shield, Truck, Lock, Mail, ArrowRight, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuthStore();
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      toast.error('Please provide both email and password');
      return;
    }

    setLoading(true);
    try {
      const response = await apiClient.post<UserSession>('/auth/login', { email, password });
      login(response.data);
      toast.success(`Welcome back, ${response.data.name}!`);

      if (response.data.role === 'ADMIN') {
        navigate('/admin');
      } else {
        navigate('/agent');
      }
    } catch (err: any) {
      const msg = err.response?.data?.detail || err.response?.data?.message || 'Invalid credentials';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const setPreset = (presetEmail: string) => {
    setEmail(presetEmail);
    setPassword(presetEmail === 'admin@edots.dev' ? 'admin123' : 'agent123');
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 bg-white p-8 rounded-2xl border border-slate-200 shadow-xl shadow-slate-200/50">
        <div className="text-center">
          <div className="mx-auto w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center text-white shadow-lg shadow-blue-500/30">
            <Lock className="w-6 h-6" />
          </div>
          <h2 className="mt-4 text-2xl font-extrabold text-slate-900 tracking-tight">
            Sign In to EDOTS
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            Access Delivery Agent Field Portal or Admin Operations Hub
          </p>
        </div>

        {/* Quick Demo Fill Buttons */}
        <div className="bg-slate-50 p-3.5 rounded-xl border border-slate-200/80">
          <div className="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-2">
            Instant Demo Sign-in
          </div>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => setPreset('admin@edots.dev')}
              className="flex items-center justify-center space-x-1.5 px-3 py-2 text-xs font-medium bg-white hover:bg-blue-50 hover:text-blue-700 hover:border-blue-300 border border-slate-200 rounded-lg shadow-xs transition"
            >
              <Shield className="w-3.5 h-3.5 text-indigo-600" />
              <span>Admin Portal</span>
            </button>
            <button
              type="button"
              onClick={() => setPreset('agent1@edots.dev')}
              className="flex items-center justify-center space-x-1.5 px-3 py-2 text-xs font-medium bg-white hover:bg-blue-50 hover:text-blue-700 hover:border-blue-300 border border-slate-200 rounded-lg shadow-xs transition"
            >
              <Truck className="w-3.5 h-3.5 text-blue-600" />
              <span>Agent 1 (Field)</span>
            </button>
          </div>
        </div>

        <form className="mt-6 space-y-4" onSubmit={handleLogin}>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Email Address</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@edots.dev"
                className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Password</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full flex items-center justify-center space-x-2 py-2.5 px-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-medium text-sm rounded-lg shadow-md shadow-blue-500/20 disabled:opacity-50 transition"
          >
            {loading ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <>
                <span>Sign In</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
};
