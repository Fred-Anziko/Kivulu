import React from 'react';
import { ClientTask } from '../types';
import { MapPin, Clock, User, CalendarCheck } from 'lucide-react';

interface TaskCardProps {
  task: ClientTask;
  onTaskClick: (task: ClientTask) => void;
}

export const TaskCard: React.FC<TaskCardProps> = ({ task, onTaskClick }) => {
  const getStatusBadge = () => {
    switch (task.status) {
      case 'OPEN':
        return <span className="bg-amber-100 text-amber-800 text-[11px] font-bold px-2.5 py-0.5 rounded-full">OPEN TASK</span>;
      case 'MATCHED':
        return <span className="bg-teal-100 text-teal-800 text-[11px] font-bold px-2.5 py-0.5 rounded-full">WORKER MATCHED</span>;
      case 'IN_PROGRESS':
        return <span className="bg-blue-100 text-blue-800 text-[11px] font-bold px-2.5 py-0.5 rounded-full">IN PROGRESS</span>;
      case 'COMPLETED':
        return <span className="bg-emerald-100 text-emerald-800 text-[11px] font-bold px-2.5 py-0.5 rounded-full">COMPLETED</span>;
      default:
        return <span className="bg-gray-100 text-gray-700 text-[11px] font-bold px-2.5 py-0.5 rounded-full">{task.status}</span>;
    }
  };

  return (
    <div
      onClick={() => onTaskClick(task)}
      className="bg-white rounded-xl p-5 border border-gray-200/80 shadow-sm hover:shadow-md transition-all cursor-pointer flex flex-col justify-between"
    >
      <div>
        <div className="flex items-center justify-between mb-2">
          {getStatusBadge()}
          <span className="font-extrabold text-teal-800 text-sm">{task.budget}</span>
        </div>

        <h3 className="font-bold text-gray-900 text-base mb-1 hover:text-teal-800 transition-colors">{task.title}</h3>
        <p className="text-xs text-gray-600 mb-4 line-clamp-2 leading-relaxed">{task.description}</p>
      </div>

      <div>
        <div className="flex items-center justify-between text-xs text-gray-500 pt-3 border-t border-gray-100">
          <div className="flex items-center space-x-1">
            <MapPin className="w-3.5 h-3.5 text-gray-400" />
            <span className="truncate max-w-[140px]">{task.location}</span>
          </div>

          <div className="flex items-center space-x-1">
            <Clock className="w-3.5 h-3.5 text-gray-400" />
            <span>{task.deadline}</span>
          </div>
        </div>

        {task.assignedWorkerName && (
          <div className="mt-2.5 bg-teal-50 border border-teal-100 rounded-lg p-2 flex items-center justify-between text-xs">
            <div className="flex items-center space-x-1.5 text-teal-900 font-semibold">
              <User className="w-3.5 h-3.5 text-teal-700" />
              <span>Worker: {task.assignedWorkerName}</span>
            </div>
            {task.calendarEventLink && (
              <span className="text-[10px] text-emerald-700 font-bold flex items-center space-x-1">
                <CalendarCheck className="w-3 h-3" />
                <span>Google Calendar Sync</span>
              </span>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
