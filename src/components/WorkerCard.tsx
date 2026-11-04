import React from 'react';
import { GigWorker } from '../types';
import { ShieldCheck, Star, MapPin, Calendar, CheckCircle } from 'lucide-react';

interface WorkerCardProps {
  worker: GigWorker;
  onWorkerClick: (worker: GigWorker) => void;
  onScheduleClick: (worker: GigWorker) => void;
}

export const WorkerCard: React.FC<WorkerCardProps> = ({ worker, onWorkerClick, onScheduleClick }) => {
  return (
    <div className="bg-white rounded-xl p-5 border border-gray-200/80 shadow-sm hover:shadow-md transition-all flex flex-col justify-between">
      <div>
        <div className="flex items-start justify-between mb-3">
          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 rounded-full bg-teal-800 text-white font-bold text-lg flex items-center justify-center flex-shrink-0 shadow-inner">
              {worker.name.charAt(0)}
            </div>
            <div>
              <div className="flex items-center space-x-1.5">
                <h3 className="font-bold text-gray-900 text-base">{worker.name}</h3>
                {worker.verificationStatus === 'VETTED_APPROVED' && (
                  <ShieldCheck className="w-4 h-4 text-emerald-600 flex-shrink-0" title="Vetted & Approved" />
                )}
              </div>
              <p className="text-xs text-gray-500 font-medium">{worker.category} • {worker.experienceYears} yrs experience</p>
            </div>
          </div>

          <div className="flex items-center space-x-1 bg-amber-50 text-amber-700 border border-amber-200 px-2.5 py-1 rounded-lg text-xs font-bold">
            <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
            <span>{worker.rating}</span>
            <span className="text-gray-400 font-normal">({worker.reviewCount})</span>
          </div>
        </div>

        <p className="text-xs text-gray-600 mb-3 line-clamp-2 leading-relaxed">{worker.bio}</p>

        {/* Skill badges */}
        <div className="flex flex-wrap gap-1.5 mb-4">
          {worker.skills.slice(0, 3).map((skill, idx) => (
            <span key={idx} className="bg-gray-100 text-gray-700 text-[11px] font-medium px-2.5 py-0.5 rounded-md">
              {skill}
            </span>
          ))}
          {worker.skills.length > 3 && (
            <span className="bg-gray-100 text-gray-500 text-[11px] font-medium px-2 py-0.5 rounded-md">
              +{worker.skills.length - 3} more
            </span>
          )}
        </div>
      </div>

      <div className="pt-3 border-t border-gray-100 flex items-center justify-between">
        <div className="flex items-center space-x-1 text-xs text-gray-500">
          <MapPin className="w-3.5 h-3.5 text-gray-400" />
          <span>{worker.location}</span>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => onWorkerClick(worker)}
            className="text-xs font-semibold text-teal-800 hover:text-teal-900 px-3 py-1.5 rounded-lg hover:bg-teal-50"
          >
            Details
          </button>
          <button
            onClick={() => onScheduleClick(worker)}
            className="bg-amber-500 hover:bg-amber-600 text-white font-bold text-xs px-3.5 py-1.5 rounded-lg transition-all flex items-center space-x-1 shadow-sm"
          >
            <Calendar className="w-3.5 h-3.5" />
            <span>Schedule</span>
          </button>
        </div>
      </div>
    </div>
  );
};
