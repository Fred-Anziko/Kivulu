import React, { useState } from 'react';
import { GigWorker, ClientTask, UserRole } from '../types';
import { HeroBanner } from '../components/HeroBanner';
import { WorkerCard } from '../components/WorkerCard';
import { TaskCard } from '../components/TaskCard';
import { Search, Plus } from 'lucide-react';

interface ExploreViewProps {
  workers: GigWorker[];
  tasks: ClientTask[];
  currentRole: UserRole;
  onWorkerClick: (worker: GigWorker) => void;
  onScheduleClick: (worker: GigWorker) => void;
  onTaskClick: (task: ClientTask) => void;
  onPostTaskClick: () => void;
}

export const ExploreView: React.FC<ExploreViewProps> = ({
  workers,
  tasks,
  currentRole,
  onWorkerClick,
  onScheduleClick,
  onTaskClick,
  onPostTaskClick,
}) => {
  const [activeTab, setActiveTab] = useState<'WORKERS' | 'TASKS'>('WORKERS');
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const categories = ['All', 'Electrical', 'Plumbing', 'IT & Tech', 'Cleaning', 'Carpentry', 'Delivery'];

  const filteredWorkers = workers.filter((w) => {
    const matchesCategory = selectedCategory === 'All' || w.category.toLowerCase().includes(selectedCategory.toLowerCase());
    const matchesSearch =
      !searchQuery ||
      w.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      w.skills.some((s) => s.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesCategory && matchesSearch;
  });

  const filteredTasks = tasks.filter((t) => {
    const matchesCategory = selectedCategory === 'All' || t.category.toLowerCase().includes(selectedCategory.toLowerCase());
    const matchesSearch =
      !searchQuery ||
      t.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.description.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  return (
    <div className="max-w-6xl mx-auto px-4 py-4 pb-24">
      {/* Hero Banner */}
      <HeroBanner onPostTaskClick={onPostTaskClick} onExploreClick={() => setActiveTab('WORKERS')} />

      {/* Search & Filters */}
      <div className="bg-white rounded-2xl p-4 shadow-sm border border-gray-200/80 my-4 space-y-4">
        <div className="relative">
          <Search className="w-5 h-5 text-gray-400 absolute left-3.5 top-3" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by skill, name, trade or location..."
            className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-teal-800 focus:outline-none"
          />
        </div>

        {/* Category Pills */}
        <div className="flex items-center space-x-2 overflow-x-auto pb-1 scrollbar-none">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all ${
                selectedCategory === cat
                  ? 'bg-teal-800 text-white shadow-sm'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* View Toggle Tabs */}
      <div className="flex items-center justify-between mb-4 border-b border-gray-200 pb-2">
        <div className="flex space-x-4">
          <button
            onClick={() => setActiveTab('WORKERS')}
            className={`font-bold text-sm pb-2 border-b-2 transition-all ${
              activeTab === 'WORKERS' ? 'border-teal-800 text-teal-900' : 'border-transparent text-gray-500 hover:text-gray-800'
            }`}
          >
            Vetted Gig Workers ({filteredWorkers.length})
          </button>
          <button
            onClick={() => setActiveTab('TASKS')}
            className={`font-bold text-sm pb-2 border-b-2 transition-all ${
              activeTab === 'TASKS' ? 'border-teal-800 text-teal-900' : 'border-transparent text-gray-500 hover:text-gray-800'
            }`}
          >
            Client Tasks Needed ({filteredTasks.length})
          </button>
        </div>

        <button
          onClick={onPostTaskClick}
          className="bg-amber-500 hover:bg-amber-600 text-white font-bold px-3.5 py-1.5 rounded-xl text-xs flex items-center space-x-1 shadow"
        >
          <Plus className="w-4 h-4" />
          <span>Post Task</span>
        </button>
      </div>

      {/* Content Grid */}
      {activeTab === 'WORKERS' ? (
        filteredWorkers.length === 0 ? (
          <div className="text-center py-12 text-gray-500 text-sm">No vetted workers matched your filter criteria.</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredWorkers.map((worker) => (
              <WorkerCard
                key={worker.id}
                worker={worker}
                onWorkerClick={onWorkerClick}
                onScheduleClick={onScheduleClick}
              />
            ))}
          </div>
        )
      ) : filteredTasks.length === 0 ? (
        <div className="text-center py-12 text-gray-500 text-sm">No open client tasks posted yet.</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredTasks.map((task) => (
            <TaskCard key={task.id} task={task} onTaskClick={onTaskClick} />
          ))}
        </div>
      )}
    </div>
  );
};
