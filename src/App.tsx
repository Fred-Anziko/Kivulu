import React, { useState } from 'react';
import {
  GigWorker,
  ClientTask,
  VettingInterview,
  ChatMessage,
  UserRole,
  NavigationTab,
} from './types';
import { INITIAL_WORKERS, INITIAL_TASKS, VETTING_QUESTIONS } from './mockData';
import { processClientAgentQuery, processWorkerAgentQuery } from './services/aiService';

import { Navbar } from './components/Navbar';
import { PostTaskModal } from './components/PostTaskModal';
import { WorkerDetailModal } from './components/WorkerDetailModal';
import { TaskDetailModal } from './components/TaskDetailModal';

import { ExploreView } from './views/ExploreView';
import { AssistantView } from './views/AssistantView';
import { MyTasksView } from './views/MyTasksView';
import { WorkerProfileView } from './views/WorkerProfileView';

export function App() {
  const [currentRole, setCurrentRole] = useState<UserRole>('CLIENT');
  const [selectedTab, setSelectedTab] = useState<NavigationTab>('EXPLORE');

  const [workers, setWorkers] = useState<GigWorker[]>(INITIAL_WORKERS);
  const [tasks, setTasks] = useState<ClientTask[]>(INITIAL_TASKS);
  const [myProfile, setMyProfile] = useState<GigWorker | null>(INITIAL_WORKERS[0]);

  const [activeInterview, setActiveInterview] = useState<VettingInterview | null>(null);

  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      sender: 'CLIENT_AGENT',
      senderTitle: 'Kivulu AI Agent',
      message: 'Welcome to Kivulu! I am your AI Assistant. I help Clients match and schedule vetted professionals, and help Gig Workers complete AI skill verification & Google Calendar sync.',
      timestamp: 'Just now',
      actionSuggestions: [
        'Post a plumbing repair task',
        'Find certified electrician nearby',
        'Start Worker AI Vetting Interview'
      ]
    }
  ]);
  const [isAiThinking, setIsAiThinking] = useState(false);

  // Modals state
  const [isPostTaskOpen, setIsPostTaskOpen] = useState(false);
  const [selectedWorkerDetails, setSelectedWorkerDetails] = useState<GigWorker | null>(null);
  const [selectedTaskDetails, setSelectedTaskDetails] = useState<ClientTask | null>(null);

  // Handlers
  const handlePostTask = (taskData: {
    title: string;
    description: string;
    category: string;
    location: string;
    budget: string;
    deadline: string;
    clientName: string;
    clientEmail: string;
  }) => {
    const newTask: ClientTask = {
      id: `t_${Date.now()}`,
      clientName: taskData.clientName || 'Client User',
      clientEmail: taskData.clientEmail || 'client@kivulu.com',
      title: taskData.title,
      description: taskData.description,
      category: taskData.category,
      location: taskData.location,
      deadline: taskData.deadline,
      budget: taskData.budget,
      status: 'OPEN',
      postedAt: 'Just now'
    };

    setTasks(prev => [newTask, ...prev]);

    // Send AI message
    const chatMsg: ChatMessage = {
      id: `msg_create_${newTask.id}`,
      sender: 'CLIENT_AGENT',
      senderTitle: 'gigClientAgent',
      message: `Task successfully posted! Title: '${newTask.title}'. Budget: ${newTask.budget}. I am matching nearby vetted ${newTask.category} workers now.`,
      timestamp: 'Just now',
      createdTask: newTask,
      actionSuggestions: ['View Task in My Tasks', 'Schedule Calendar Invite']
    };
    setChatMessages(prev => [...prev, chatMsg]);
  };

  const handleMatchWorkerToTask = (task: ClientTask, worker: GigWorker, scheduledTime: string = 'Tomorrow at 10:00 AM') => {
    const calendarLink = `https://calendar.google.com/calendar/r/eventedit?text=KivuluTask_${task.id.slice(0, 5)}&details=Scheduled+Gig+Task+with+${worker.name}`;

    setTasks(prev => prev.map(t => {
      if (t.id === task.id) {
        return {
          ...t,
          status: 'MATCHED',
          assignedWorkerId: worker.id,
          assignedWorkerName: worker.name,
          scheduledTime: scheduledTime,
          calendarEventLink: calendarLink
        };
      }
      return t;
    }));

    setWorkers(prev => prev.map(w => {
      if (w.id === worker.id) {
        return { ...w, completedTasksCount: w.completedTasksCount + 1 };
      }
      return w;
    }));

    const confirmMsg: ChatMessage = {
      id: `msg_match_${task.id}`,
      sender: 'CLIENT_AGENT',
      senderTitle: 'gigClientAgent',
      message: `Task '${task.title}' matched with ${worker.name}! Scheduled on Google Calendar for ${scheduledTime}. Location: ${task.location}.`,
      timestamp: 'Just now',
      recommendedWorker: worker,
      actionSuggestions: ['View Calendar Link', 'Open My Tasks']
    };
    setChatMessages(prev => [...prev, confirmMsg]);
  };

  const handleRateTask = (task: ClientTask, rating: number, reviewText: string) => {
    setTasks(prev => prev.map(t => {
      if (t.id === task.id) {
        return {
          ...t,
          status: 'COMPLETED',
          rating: rating,
          review: reviewText
        };
      }
      return t;
    }));
  };

  const handleSendMessage = async (userMsg: string) => {
    if (!userMsg.trim()) return;

    const userChat: ChatMessage = {
      id: `msg_u_${Date.now()}`,
      sender: 'USER',
      senderTitle: 'You',
      message: userMsg,
      timestamp: 'Just now'
    };

    setChatMessages(prev => [...prev, userChat]);
    setIsAiThinking(true);

    if (currentRole === 'GIG_WORKER') {
      const response = await processWorkerAgentQuery(userMsg, myProfile || undefined);
      setChatMessages(prev => [...prev, response]);
    } else {
      const response = await processClientAgentQuery(userMsg, workers);
      setChatMessages(prev => [...prev, response]);
    }

    setIsAiThinking(false);
  };

  const handleStartInterview = (category: string) => {
    const questions = VETTING_QUESTIONS[category] || VETTING_QUESTIONS['Electrical'];
    const interview: VettingInterview = {
      id: `inv_${Date.now()}`,
      workerId: myProfile?.id || 'w1',
      workerName: myProfile?.name || 'Worker',
      category: category,
      questions: questions,
      currentQuestionIndex: 0,
      userAnswers: {},
      score: 0,
      status: 'IN_PROGRESS',
      aiAssessmentSummary: ''
    };
    setActiveInterview(interview);
    setSelectedTab('PROFILE');
  };

  const handleSubmitAnswer = (qIndex: number, optionIndex: number) => {
    if (!activeInterview) return;

    const newAnswers = { ...activeInterview.userAnswers, [qIndex]: optionIndex };
    const nextIndex = qIndex + 1;
    const isFinished = nextIndex >= activeInterview.questions.length;

    let correctCount = 0;
    Object.entries(newAnswers).forEach(([idxStr, optIdx]) => {
      const q = activeInterview.questions[Number(idxStr)];
      if (q && q.correctOptionIndex === optIdx) correctCount++;
    });

    const score = Math.round((correctCount / activeInterview.questions.length) * 100);
    const isPassed = score >= 60;

    const summary = isFinished
      ? isPassed
        ? `AI Vetting Passed (${score}% score). Verified competence in ${activeInterview.category} safety & installation procedures.`
        : `AI Vetting Score: ${score}%. Recommend reviewing safety protocols before re-attempting.`
      : '';

    const updated: VettingInterview = {
      ...activeInterview,
      userAnswers: newAnswers,
      currentQuestionIndex: isFinished ? qIndex : nextIndex,
      score: score,
      status: isFinished ? (isPassed ? 'COMPLETED_PASSED' : 'NEEDS_REVIEW') : 'IN_PROGRESS',
      aiAssessmentSummary: summary
    };

    setActiveInterview(updated);

    if (isFinished && isPassed && myProfile) {
      setMyProfile({
        ...myProfile,
        verificationStatus: 'VETTED_APPROVED',
        aiVettingSummary: summary
      });
      setWorkers(prev => prev.map(w => w.id === myProfile.id ? { ...w, verificationStatus: 'VETTED_APPROVED', aiVettingSummary: summary } : w));
    }
  };

  const handleRegisterWorker = (data: {
    name: string;
    email: string;
    phone: string;
    location: string;
    category: string;
    skillsStr: string;
    equipmentsStr: string;
    rate: string;
  }) => {
    const newWorker: GigWorker = {
      id: `w_${Date.now()}`,
      name: data.name,
      email: data.email,
      phone: data.phone,
      location: data.location,
      category: data.category,
      skills: data.skillsStr.split(',').map(s => s.trim()).filter(Boolean),
      equipments: data.equipmentsStr.split(',').map(e => e.trim()).filter(Boolean),
      experienceYears: 2,
      rating: 5.0,
      reviewCount: 0,
      completedTasksCount: 0,
      verificationStatus: 'INTERVIEW_PENDING',
      aiVettingSummary: 'Registered. Pending AI Skill Vetting Quiz.',
      hourlyRate: data.rate,
      bio: `Independent ${data.category} specialist ready for Kivulu gigs.`,
      googleCalendarConnected: true
    };

    setWorkers(prev => [newWorker, ...prev]);
    setMyProfile(newWorker);
    handleStartInterview(data.category);
  };

  return (
    <div className="min-h-screen bg-gray-50 text-gray-900 flex flex-col font-sans">
      <Navbar
        currentRole={currentRole}
        selectedTab={selectedTab}
        onRoleChange={setCurrentRole}
        onTabChange={setSelectedTab}
      />

      <main className="flex-1">
        {selectedTab === 'EXPLORE' && (
          <ExploreView
            workers={workers}
            tasks={tasks}
            currentRole={currentRole}
            onWorkerClick={setSelectedWorkerDetails}
            onScheduleClick={setSelectedWorkerDetails}
            onTaskClick={setSelectedTaskDetails}
            onPostTaskClick={() => setIsPostTaskOpen(true)}
          />
        )}

        {selectedTab === 'ASSISTANT' && (
          <AssistantView
            chatMessages={chatMessages}
            currentRole={currentRole}
            isAiThinking={isAiThinking}
            onSendMessage={handleSendMessage}
            onRoleChange={setCurrentRole}
            onSelectWorker={setSelectedWorkerDetails}
          />
        )}

        {selectedTab === 'MY_TASKS' && (
          <MyTasksView
            tasks={tasks}
            onTaskClick={setSelectedTaskDetails}
            onPostTaskClick={() => setIsPostTaskOpen(true)}
          />
        )}

        {selectedTab === 'PROFILE' && (
          <WorkerProfileView
            myProfile={myProfile}
            activeInterview={activeInterview}
            onStartInterview={handleStartInterview}
            onSubmitAnswer={handleSubmitAnswer}
            onRegisterWorker={handleRegisterWorker}
          />
        )}
      </main>

      {/* Modals */}
      <PostTaskModal
        isOpen={isPostTaskOpen}
        onClose={() => setIsPostTaskOpen(false)}
        onSubmit={handlePostTask}
      />

      <WorkerDetailModal
        worker={selectedWorkerDetails}
        clientTasks={tasks}
        onClose={() => setSelectedWorkerDetails(null)}
        onScheduleWithTask={handleMatchWorkerToTask}
      />

      <TaskDetailModal
        task={selectedTaskDetails}
        workers={workers}
        onClose={() => setSelectedTaskDetails(null)}
        onMatchWorker={handleMatchWorkerToTask}
        onRateTask={handleRateTask}
      />
    </div>
  );
}
