import React from 'react';
import { ShieldCheck, Calendar, Sparkles, UserCheck } from 'lucide-react';

interface HeroBannerProps {
  onPostTaskClick: () => void;
  onExploreClick: () => void;
}

export const HeroBanner: React.FC<HeroBannerProps> = ({ onPostTaskClick, onExploreClick }) => {
  return (
    <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-teal-900 via-teal-800 to-teal-700 text-white shadow-lg my-4">
      <div className="absolute -right-10 -bottom-10 w-72 h-72 bg-amber-500/10 rounded-full blur-2xl pointer-events-none" />
      <div className="p-6 md:p-8 relative z-10">
        <div className="inline-flex items-center space-x-2 bg-amber-500/20 text-amber-300 border border-amber-400/30 px-3 py-1 rounded-full text-xs font-semibold mb-3">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Multi-AI Agent Vetting & Task Scheduling</span>
        </div>

        <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight mb-2">
          Discover Vetted Gig Workers & Schedule Tasks Instantly
        </h1>
        <p className="text-teal-100 text-sm md:text-base max-w-2xl mb-6 leading-relaxed">
          Kivulu automatically assesses worker competence via AI interviews, matches skilled local professionals to client tasks, and syncs schedules directly to Google Calendar.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="flex items-center space-x-3 bg-teal-900/40 p-3 rounded-xl border border-teal-700/50">
            <ShieldCheck className="w-6 h-6 text-emerald-400 flex-shrink-0" />
            <div>
              <p className="text-xs font-bold">1. AI Worker Vetting</p>
              <p className="text-[11px] text-teal-200">Verified skills, tools & experience</p>
            </div>
          </div>
          <div className="flex items-center space-x-3 bg-teal-900/40 p-3 rounded-xl border border-teal-700/50">
            <UserCheck className="w-6 h-6 text-amber-400 flex-shrink-0" />
            <div>
              <p className="text-xs font-bold">2. Precision Task Matching</p>
              <p className="text-[11px] text-teal-200">Clear specs & nearby worker discovery</p>
            </div>
          </div>
          <div className="flex items-center space-x-3 bg-teal-900/40 p-3 rounded-xl border border-teal-700/50">
            <Calendar className="w-6 h-6 text-blue-400 flex-shrink-0" />
            <div>
              <p className="text-xs font-bold">3. Google Calendar Sync</p>
              <p className="text-[11px] text-teal-200">Automatic event invite creation</p>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap gap-3">
          <button
            onClick={onPostTaskClick}
            className="bg-amber-500 hover:bg-amber-600 text-white font-bold px-5 py-2.5 rounded-xl text-sm transition-all shadow-md hover:shadow-lg flex items-center space-x-2"
          >
            <span>+ Post a Task with AI</span>
          </button>
          <button
            onClick={onExploreClick}
            className="bg-teal-900/60 hover:bg-teal-900 text-white font-semibold px-5 py-2.5 rounded-xl text-sm border border-teal-600/50 transition-all"
          >
            <span>Browse Vetted Workers</span>
          </button>
        </div>
      </div>
    </div>
  );
};
