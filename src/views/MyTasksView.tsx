import React, { useState } from 'react';
import { ClientTask } from '../types';
import { TaskCard } from '../components/TaskCard';
import { Plus, CheckSquare, Calendar, ShieldCheck } from 'lucide-react';

interface MyTasksViewProps {
  tasks: ClientTask[];
  onTaskClick: (task: ClientTask) => void;
  onPostTaskClick: () => void;
}

export const MyTasksView: React.FC<MyTasksViewProps> = ({ tasks, onTaskClick, onPostTaskClick }) => {
  const [filter, setFilter] = useState<'ACTIVE' | 'COMPLETED'>('ACTIVE');

  const activeTasks = tasks.filter(t => t.status === 'OPEN' || t.status === 'MATCHED' || t.status === 'IN_PROGRESS');
  const completedTasks = tasks.filter(t => t.status === 'COMPLETED' || t.status === 'CANCELLED');

  const displayed = filter === 'ACTIVE' ? activeTasks : completedTasks;

  return (
    <div className="max-w-6xl mx-auto px-4 py-6 pb-24">
      <div className="flex flex-col md:flex-row md:items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-teal-900 flex items-center space-x-2">
            <CheckSquare className="w-6 h-6 text-amber-500" />
            <span>My Tasks & Schedules</span>
          </h1>
          <p className="text-xs text-gray-500">
            Track booked gigs, matched workers, and synchronized Google Calendar events
          </p>
        </div>

        <button
          onClick={onPostTaskClick}
          className="mt-3 md:mt-0 bg-amber-500 hover:bg-amber-600 text-white font-bold px-4 py-2 rounded-xl text-xs flex items-center space-x-1.5 shadow"
        >
          <Plus className="w-4 h-4" />
          <span>Post New Task</span>
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="flex space-x-2 border-b border-gray-200 mb-6">
        <button
          onClick={() => setFilter('ACTIVE')}
          className={`pb-2.5 px-4 font-bold text-sm border-b-2 transition-all ${
            filter === 'ACTIVE' ? 'border-teal-800 text-teal-900' : 'border-transparent text-gray-400 hover:text-gray-600'
          }`}
        >
          Active Tasks ({activeTasks.length})
        </button>
        <button
          onClick={() => setFilter('COMPLETED')}
          className={`pb-2.5 px-4 font-bold text-sm border-b-2 transition-all ${
            filter === 'COMPLETED' ? 'border-teal-800 text-teal-900' : 'border-transparent text-gray-400 hover:text-gray-600'
          }`}
        >
          Completed Gigs ({completedTasks.length})
        </button>
      </div>

      {displayed.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-gray-200/80 shadow-sm">
          <Calendar className="w-12 h-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500 text-sm font-medium">
            {filter === 'ACTIVE' ? 'No active tasks in progress.' : 'No completed gigs history yet.'}
          </p>
          <button
            onClick={onPostTaskClick}
            className="mt-4 text-xs font-bold text-teal-800 hover:underline"
          >
            Post a new task now →
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {displayed.map((task) => (
            <TaskCard key={task.id} task={task} onTaskClick={onTaskClick} />
          ))}
        </div>
      )}
    </div>
  );
};
