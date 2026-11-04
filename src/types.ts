export type VerificationStatus = 'UNVERIFIED' | 'INTERVIEW_PENDING' | 'VETTED_APPROVED';

export interface GigWorker {
  id: string;
  name: string;
  email: string;
  phone: string;
  location: string;
  category: string;
  skills: string[];
  equipments: string[];
  experienceYears: number;
  rating: number;
  reviewCount: number;
  completedTasksCount: number;
  verificationStatus: VerificationStatus;
  aiVettingSummary: string;
  hourlyRate: string;
  bio: string;
  avatarUrl?: string;
  googleCalendarConnected: boolean;
}

export type TaskStatus = 'OPEN' | 'MATCHED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface ClientTask {
  id: string;
  clientName: string;
  clientEmail: string;
  clientPhone?: string;
  title: string;
  description: string;
  category: string;
  location: string;
  deadline: string;
  budget: string;
  status: TaskStatus;
  assignedWorkerId?: string;
  assignedWorkerName?: string;
  scheduledTime?: string;
  calendarEventLink?: string;
  postedAt: string;
  rating?: number;
  review?: string;
}

export interface InterviewQuestion {
  id: number;
  questionText: string;
  options: string[];
  correctOptionIndex: number;
  explanation: string;
}

export interface VettingInterview {
  id: string;
  workerId: string;
  workerName: string;
  category: string;
  questions: InterviewQuestion[];
  currentQuestionIndex: number;
  userAnswers: Record<number, number>;
  score: number;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED_PASSED' | 'NEEDS_REVIEW';
  aiAssessmentSummary: string;
}

export type ChatSender = 'USER' | 'CLIENT_AGENT' | 'WORKER_AGENT' | 'SYSTEM';

export interface ChatMessage {
  id: string;
  sender: ChatSender;
  senderTitle: string;
  message: string;
  timestamp: string;
  recommendedWorker?: GigWorker;
  createdTask?: ClientTask;
  actionSuggestions?: string[];
}

export type UserRole = 'CLIENT' | 'GIG_WORKER';
export type NavigationTab = 'EXPLORE' | 'ASSISTANT' | 'MY_TASKS' | 'PROFILE';
