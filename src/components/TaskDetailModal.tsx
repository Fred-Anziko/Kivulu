import React, { useState } from 'react';
import { ClientTask, GigWorker } from '../types';
import { X, MapPin, Clock, Star, CalendarCheck, CheckCircle, UserCheck } from 'lucide-react';

interface TaskDetailModalProps {
  task: ClientTask | null;
  workers: GigWorker[];
  onClose: () => void;
  onMatchWorker: (task: ClientTask, worker: GigWorker) => void;
  onRateTask: (task: ClientTask, rating: number, review: string) => void;
}

export const TaskDetailModal: React.FC<TaskDetailModalProps> = ({
  task,
  workers,
  onClose,
  onMatchWorker,
  onRateTask,
}) => {
  if (!task) return null;

  const [selectedWorkerId, setSelectedWorkerId] = useState<string>(
    workers.find((w) => w.category === task.category)?.id || workers[0]?.id || ''
  );
  const [rating, setRating] = useState<number>(5);
  const [reviewText, setReviewText] = useState<string>('Excellent, timely and skilled task completion!');

  const handleMatch = () => {
    const worker = workers.find((w) => w.id === selectedWorkerId);
    if (worker) {
      onMatchWorker(task, worker);
    }
  };

  const handleCompleteAndRate = () => {
    onRateTask(task, rating, reviewText);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl relative animate-in fade-in zoom-in duration-200">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 p-1 rounded-full hover:bg-gray-100"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Task Header */}
        <div className="mb-4">
          <div className="flex items-center justify-between mb-1">
            <span className="bg-teal-100 text-teal-800 text-[11px] font-bold px-2.5 py-0.5 rounded-full">
              {task.category}
            </span>
            <span className="text-base font-extrabold text-teal-800">{task.budget}</span>
          </div>

          <h2 className="text-xl font-bold text-gray-900 mb-2">{task.title}</h2>
          <p className="text-xs text-gray-600 leading-relaxed bg-gray-50 p-3 rounded-xl border border-gray-200">
            {task.description}
          </p>
        </div>

        <div className="grid grid-cols-2 gap-3 mb-4 text-xs text-gray-600">
          <div className="flex items-center space-x-1.5">
            <MapPin className="w-4 h-4 text-gray-400" />
            <span>{task.location}</span>
          </div>
          <div className="flex items-center space-x-1.5">
            <Clock className="w-4 h-4 text-gray-400" />
            <span>{task.deadline}</span>
          </div>
        </div>

        <hr className="my-4 border-gray-100" />

        {/* Task Status Actions */}
        {task.status === 'OPEN' && (
          <div>
            <h3 className="text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">Match Vetted Gig Worker</h3>
            <div className="space-y-2 mb-4 max-h-48 overflow-y-auto">
              {workers.map((w) => (
                <label
                  key={w.id}
                  className={`flex items-center justify-between p-3 rounded-xl border text-xs cursor-pointer transition-all ${
                    selectedWorkerId === w.id
                      ? 'border-teal-800 bg-teal-50/60 font-semibold text-teal-900'
                      : 'border-gray-200 hover:bg-gray-50 text-gray-700'
                  }`}
                >
                  <div className="flex items-center space-x-2.5">
                    <input
                      type="radio"
                      name="workerOption"
                      checked={selectedWorkerId === w.id}
                      onChange={() => setSelectedWorkerId(w.id)}
                      className="text-teal-800 focus:ring-teal-800"
                    />
                    <div>
                      <p className="font-bold">{w.name}</p>
                      <p className="text-[11px] text-gray-500">{w.category} • {w.location}</p>
                    </div>
                  </div>
                  <span className="flex items-center text-amber-600 font-bold">
                    <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400 mr-1" />
                    {w.rating}
                  </span>
                </label>
              ))}
            </div>

            <button
              onClick={handleMatch}
              className="w-full bg-teal-800 hover:bg-teal-900 text-white font-bold py-2.5 rounded-xl text-xs transition-all shadow flex items-center justify-center space-x-1.5"
            >
              <UserCheck className="w-4 h-4 text-amber-400" />
              <span>Assign & Schedule Task</span>
            </button>
          </div>
        )}

        {(task.status === 'MATCHED' || task.status === 'IN_PROGRESS') && (
          <div>
            <div className="bg-teal-50 border border-teal-200 rounded-xl p-3 mb-4">
              <div className="flex items-center justify-between text-xs text-teal-900 font-bold mb-1">
                <span>Assigned Worker: {task.assignedWorkerName}</span>
                <span className="flex items-center text-emerald-700">
                  <CalendarCheck className="w-3.5 h-3.5 mr-1" /> Google Calendar Synced
                </span>
              </div>
              <p className="text-[11px] text-teal-700">Scheduled: {task.scheduledTime || 'Confirmed'}</p>
            </div>

            <h3 className="text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">Complete Task & Rate Worker</h3>

            <div className="flex items-center space-x-1 mb-3">
              {[1, 2, 3, 4, 5].map((starIndex) => (
                <button
                  key={starIndex}
                  type="button"
                  onClick={() => setRating(starIndex)}
                  className="p-1 text-amber-400 hover:scale-110 transition-transform"
                >
                  <Star className={`w-6 h-6 ${starIndex <= rating ? 'fill-amber-400 text-amber-400' : 'text-gray-300'}`} />
                </button>
              ))}
            </div>

            <textarea
              rows={2}
              value={reviewText}
              onChange={(e) => setReviewText(e.target.value)}
              placeholder="Write client feedback..."
              className="w-full p-2.5 border border-gray-300 rounded-xl text-xs mb-3 focus:ring-2 focus:ring-teal-800 focus:outline-none"
            />

            <button
              onClick={handleCompleteAndRate}
              className="w-full bg-amber-500 hover:bg-amber-600 text-white font-bold py-2.5 rounded-xl text-xs transition-all shadow flex items-center justify-center space-x-1.5"
            >
              <CheckCircle className="w-4 h-4" />
              <span>Mark Completed & Submit Review</span>
            </button>
          </div>
        )}

        {task.status === 'COMPLETED' && (
          <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4 text-emerald-900">
            <h4 className="font-bold text-xs flex items-center space-x-1 mb-1">
              <CheckCircle className="w-4 h-4 text-emerald-600" />
              <span>Task Completed & Rated</span>
            </h4>
            <p className="text-xs font-semibold mb-1">Rating: {task.rating || 5} ★</p>
            <p className="text-xs text-emerald-800 italic">"{task.review || 'Great job!'}"</p>
          </div>
        )}
      </div>
    </div>
  );
};
