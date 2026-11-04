import React, { useState } from 'react';
import { GigWorker, ClientTask } from '../types';
import { X, ShieldCheck, Star, Calendar, MapPin, CheckCircle, BrainCircuit } from 'lucide-react';

interface WorkerDetailModalProps {
  worker: GigWorker | null;
  clientTasks: ClientTask[];
  onClose: () => void;
  onScheduleWithTask: (task: ClientTask, worker: GigWorker, time: string) => void;
}

export const WorkerDetailModal: React.FC<WorkerDetailModalProps> = ({
  worker,
  clientTasks,
  onClose,
  onScheduleWithTask,
}) => {
  if (!worker) return null;

  const [selectedTaskId, setSelectedTaskId] = useState<string>(clientTasks[0]?.id || '');
  const [scheduledTime, setScheduledTime] = useState<string>('Tomorrow at 10:00 AM');

  const handleConfirmSchedule = () => {
    const task = clientTasks.find((t) => t.id === selectedTaskId) || clientTasks[0];
    if (task) {
      onScheduleWithTask(task, worker, scheduledTime);
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-xl relative animate-in fade-in zoom-in duration-200">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 p-1 rounded-full hover:bg-gray-100"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Worker Header */}
        <div className="flex items-start space-x-4 mb-4">
          <div className="w-16 h-16 rounded-full bg-teal-800 text-white font-bold text-2xl flex items-center justify-center flex-shrink-0 shadow">
            {worker.name.charAt(0)}
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-xl font-bold text-gray-900">{worker.name}</h2>
              {worker.verificationStatus === 'VETTED_APPROVED' && (
                <ShieldCheck className="w-5 h-5 text-emerald-600" title="Vetted & Approved" />
              )}
            </div>
            <p className="text-sm font-semibold text-teal-800">{worker.category} • {worker.hourlyRate}</p>
            <div className="flex items-center space-x-3 text-xs text-gray-500 mt-1">
              <span className="flex items-center space-x-1 text-amber-600 font-bold">
                <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                <span>{worker.rating} ({worker.reviewCount} reviews)</span>
              </span>
              <span>•</span>
              <span className="flex items-center space-x-1">
                <MapPin className="w-3.5 h-3.5 text-gray-400" />
                <span>{worker.location}</span>
              </span>
            </div>
          </div>
        </div>

        {/* AI Vetting Box */}
        <div className="bg-teal-50/80 border border-teal-200 rounded-xl p-3.5 mb-4">
          <div className="flex items-center space-x-2 text-teal-900 font-bold text-xs mb-1">
            <BrainCircuit className="w-4 h-4 text-teal-700" />
            <span>Kivulu AI Vetting Certificate</span>
          </div>
          <p className="text-xs text-teal-800 leading-relaxed">{worker.aiVettingSummary}</p>
        </div>

        {/* About & Skills */}
        <div className="mb-4">
          <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">About Worker</h4>
          <p className="text-xs text-gray-600 leading-relaxed">{worker.bio}</p>
        </div>

        <div className="mb-5">
          <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">Verified Skills & Tools</h4>
          <div className="flex flex-wrap gap-1.5">
            {worker.skills.map((skill, idx) => (
              <span key={idx} className="bg-gray-100 text-gray-800 text-xs font-medium px-2.5 py-1 rounded-lg">
                {skill}
              </span>
            ))}
            {worker.equipments.map((eq, idx) => (
              <span key={`eq_${idx}`} className="bg-teal-100/60 text-teal-900 text-xs font-medium px-2.5 py-1 rounded-lg">
                🛠️ {eq}
              </span>
            ))}
          </div>
        </div>

        <hr className="my-4 border-gray-100" />

        {/* Scheduling Section */}
        <div>
          <h3 className="text-sm font-bold text-teal-900 mb-2 flex items-center space-x-1.5">
            <Calendar className="w-4 h-4 text-amber-500" />
            <span>Schedule Task into Google Calendar</span>
          </h3>

          {clientTasks.length === 0 ? (
            <p className="text-xs text-gray-500">
              You don't have any open tasks yet. Create a task first to schedule with this worker.
            </p>
          ) : (
            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Select Task to Assign:</label>
                <div className="space-y-1.5 max-h-32 overflow-y-auto">
                  {clientTasks.map((t) => (
                    <label
                      key={t.id}
                      className={`flex items-center justify-between p-2.5 rounded-xl border text-xs cursor-pointer transition-all ${
                        selectedTaskId === t.id
                          ? 'border-teal-800 bg-teal-50/60 font-semibold text-teal-900'
                          : 'border-gray-200 hover:bg-gray-50 text-gray-700'
                      }`}
                    >
                      <div className="flex items-center space-x-2">
                        <input
                          type="radio"
                          name="selectedTask"
                          checked={selectedTaskId === t.id}
                          onChange={() => setSelectedTaskId(t.id)}
                          className="text-teal-800 focus:ring-teal-800"
                        />
                        <span>{t.title}</span>
                      </div>
                      <span className="font-bold text-teal-800">{t.budget}</span>
                    </label>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Proposed Schedule Time:</label>
                <input
                  type="text"
                  value={scheduledTime}
                  onChange={(e) => setScheduledTime(e.target.value)}
                  className="w-full px-3 py-1.5 border border-gray-300 rounded-xl text-xs focus:ring-2 focus:ring-teal-800 focus:outline-none"
                />
              </div>

              <button
                onClick={handleConfirmSchedule}
                className="w-full bg-amber-500 hover:bg-amber-600 text-white font-bold py-2.5 rounded-xl text-xs transition-all shadow flex items-center justify-center space-x-2 mt-2"
              >
                <CheckCircle className="w-4 h-4" />
                <span>Confirm & Sync Google Calendar</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
