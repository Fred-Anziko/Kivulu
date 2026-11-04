import React from 'react';
import { UserRole, NavigationTab } from '../types';
import { Store, Bot, CheckSquare, ShieldCheck, Wrench } from 'lucide-react';

interface NavbarProps {
  currentRole: UserRole;
  selectedTab: NavigationTab;
  onRoleChange: (role: UserRole) => void;
  onTabChange: (tab: NavigationTab) => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentRole,
  selectedTab,
  onRoleChange,
  onTabChange,
}) => {
  return (
    <header className="sticky top-0 z-40 bg-teal-800 text-white shadow-md">
      <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center space-x-3 cursor-pointer" onClick={() => onTabChange('EXPLORE')}>
          <div className="bg-amber-500 p-2 rounded-xl text-white shadow-sm">
            <Wrench className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-bold text-xl tracking-tight">Kivulu</span>
              <span className="bg-amber-500/20 text-amber-300 text-xs px-2 py-0.5 rounded-full font-semibold border border-amber-400/30">
                Gig Marketplace
              </span>
            </div>
            <p className="text-xs text-teal-100/80">AI Vetting & Calendar Scheduling</p>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav className="hidden md:flex items-center space-x-1">
          <button
            onClick={() => onTabChange('EXPLORE')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg font-medium text-sm transition-all ${
              selectedTab === 'EXPLORE' ? 'bg-teal-900/60 text-white shadow-inner' : 'text-teal-100 hover:bg-teal-700/50'
            }`}
          >
            <Store className="w-4 h-4" />
            <span>Explore</span>
          </button>
          <button
            onClick={() => onTabChange('ASSISTANT')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg font-medium text-sm transition-all ${
              selectedTab === 'ASSISTANT' ? 'bg-teal-900/60 text-white shadow-inner' : 'text-teal-100 hover:bg-teal-700/50'
            }`}
          >
            <Bot className="w-4 h-4 text-amber-400" />
            <span>AI Assistant</span>
          </button>
          <button
            onClick={() => onTabChange('MY_TASKS')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg font-medium text-sm transition-all ${
              selectedTab === 'MY_TASKS' ? 'bg-teal-900/60 text-white shadow-inner' : 'text-teal-100 hover:bg-teal-700/50'
            }`}
          >
            <CheckSquare className="w-4 h-4" />
            <span>My Tasks</span>
          </button>
          <button
            onClick={() => onTabChange('PROFILE')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg font-medium text-sm transition-all ${
              selectedTab === 'PROFILE' ? 'bg-teal-900/60 text-white shadow-inner' : 'text-teal-100 hover:bg-teal-700/50'
            }`}
          >
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>Worker Vetting</span>
          </button>
        </nav>

        {/* Role Toggle Button */}
        <div className="flex items-center bg-teal-900/70 p-1 rounded-xl border border-teal-700/50">
          <button
            onClick={() => onRoleChange('CLIENT')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              currentRole === 'CLIENT' ? 'bg-amber-500 text-white shadow' : 'text-teal-200 hover:text-white'
            }`}
          >
            Client Mode
          </button>
          <button
            onClick={() => onRoleChange('GIG_WORKER')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              currentRole === 'GIG_WORKER' ? 'bg-amber-500 text-white shadow' : 'text-teal-200 hover:text-white'
            }`}
          >
            Gig Worker Mode
          </button>
        </div>
      </div>

      {/* Mobile Bottom Bar */}
      <div className="md:hidden flex justify-around bg-teal-900 border-t border-teal-800 py-2">
        <button
          onClick={() => onTabChange('EXPLORE')}
          className={`flex flex-col items-center text-xs font-medium ${selectedTab === 'EXPLORE' ? 'text-amber-400' : 'text-teal-200'}`}
        >
          <Store className="w-5 h-5 mb-0.5" />
          <span>Explore</span>
        </button>
        <button
          onClick={() => onTabChange('ASSISTANT')}
          className={`flex flex-col items-center text-xs font-medium ${selectedTab === 'ASSISTANT' ? 'text-amber-400' : 'text-teal-200'}`}
        >
          <Bot className="w-5 h-5 mb-0.5" />
          <span>AI Assistant</span>
        </button>
        <button
          onClick={() => onTabChange('MY_TASKS')}
          className={`flex flex-col items-center text-xs font-medium ${selectedTab === 'MY_TASKS' ? 'text-amber-400' : 'text-teal-200'}`}
        >
          <CheckSquare className="w-5 h-5 mb-0.5" />
          <span>My Tasks</span>
        </button>
        <button
          onClick={() => onTabChange('PROFILE')}
          className={`flex flex-col items-center text-xs font-medium ${selectedTab === 'PROFILE' ? 'text-amber-400' : 'text-teal-200'}`}
        >
          <ShieldCheck className="w-5 h-5 mb-0.5" />
          <span>Vetting</span>
        </button>
      </div>
    </header>
  );
};
