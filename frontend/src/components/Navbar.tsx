import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { Package, Truck, Shield, LogIn, LogOut } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { session, logout, isAuthenticated, isAdmin, isAgent } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-200 bg-white/90 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <Link to="/track" className="flex items-center space-x-2.5">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center text-white shadow-md shadow-blue-500/20">
              <Package className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xl font-bold tracking-tight text-slate-900">EDOTS</span>
              <span className="hidden sm:inline-block ml-2 text-xs font-semibold px-2 py-0.5 rounded-md bg-blue-50 text-blue-700 border border-blue-200">
                EDOTS Order Spine
              </span>
            </div>
          </Link>
        </div>

        <nav className="flex items-center space-x-1 sm:space-x-3">
          <Link
            to="/track"
            className="text-sm font-medium text-slate-600 hover:text-blue-600 px-3 py-2 rounded-lg hover:bg-slate-50 transition"
          >
            Track Order
          </Link>

          {isAgent() && (
            <Link
              to="/agent"
              className="flex items-center space-x-1.5 text-sm font-medium text-slate-600 hover:text-blue-600 px-3 py-2 rounded-lg hover:bg-slate-50 transition"
            >
              <Truck className="w-4 h-4" />
              <span>Agent Portal</span>
            </Link>
          )}

          {isAdmin() && (
            <Link
              to="/admin"
              className="flex items-center space-x-1.5 text-sm font-medium text-slate-600 hover:text-blue-600 px-3 py-2 rounded-lg hover:bg-slate-50 transition"
            >
              <Shield className="w-4 h-4" />
              <span>Operations Hub</span>
            </Link>
          )}

          {isAuthenticated() ? (
            <div className="flex items-center space-x-3 pl-3 border-l border-slate-200">
              <div className="hidden md:block text-right">
                <div className="text-xs font-semibold text-slate-900">{session?.name}</div>
                <div className="text-[10px] text-slate-500">{session?.role}</div>
              </div>
              <button
                onClick={handleLogout}
                title="Logout"
                className="p-2 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <Link
              to="/login"
              className="flex items-center space-x-1.5 text-sm font-medium bg-slate-900 text-white hover:bg-slate-800 px-3.5 py-2 rounded-lg shadow-sm transition"
            >
              <LogIn className="w-4 h-4" />
              <span>Sign In</span>
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
};
