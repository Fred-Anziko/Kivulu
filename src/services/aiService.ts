import { GoogleGenerativeAI } from '@google/generative-ai';
import { ChatMessage, GigWorker, ClientTask } from '../types';

export async function processClientAgentQuery(
  userPrompt: string,
  workers: GigWorker[],
  apiKey?: string
): Promise<ChatMessage> {
  const lower = userPrompt.toLowerCase();
  const matchedWorker = workers.find(w => 
    lower.includes(w.category.toLowerCase()) || 
    lower.includes(w.name.toLowerCase()) ||
    w.skills.some(s => lower.includes(s.toLowerCase()))
  ) || workers[0];

  let text = '';

  if (apiKey) {
    try {
      const genAI = new GoogleGenerativeAI(apiKey);
      const model = genAI.getGenerativeModel({ model: 'gemini-1.5-flash' });
      const prompt = `You are Kivulu Client Assistant (gigClientAgent). Your task is to help clients post tasks, discover nearby vetted workers, and schedule Google Calendar tasks.
      Available Workers: ${workers.map(w => `${w.name} (${w.category}, ${w.rating}★)`).join(', ')}.
      User query: "${userPrompt}". Provide a helpful concise response recommending workers or scheduling tasks.`;
      
      const result = await model.generateContent(prompt);
      text = result.response.text();
    } catch (e) {
      text = '';
    }
  }

  if (!text) {
    if (lower.includes('plumb') || lower.includes('leak') || lower.includes('pipe')) {
      const w = workers.find(x => x.category === 'Plumbing') || matchedWorker;
      text = `Hello! I am Kivulu Client Assistant. For your plumbing task, I recommend ${w.name} (${w.location}, ${w.rating}★). They specialize in high-pressure leak stoppage and pipe repairs. Would you like me to schedule a task into their Google Calendar?`;
    } else if (lower.includes('electric') || lower.includes('power') || lower.includes('breaker') || lower.includes('wire')) {
      const w = workers.find(x => x.category === 'Electrical') || matchedWorker;
      text = `Hello! I am Kivulu Client Assistant. I've matched your request with certified electrician ${w.name} (${w.rating}★). They carry certified multimeters and safety gear. Shall we lock in a schedule for tomorrow at 10:00 AM?`;
    } else if (lower.includes('tech') || lower.includes('wifi') || lower.includes('router') || lower.includes('network')) {
      const w = workers.find(x => x.category === 'IT & Tech') || matchedWorker;
      text = `I can help you with IT & network infrastructure! ${w.name} is our top-rated tech technician in ${w.location}. Would you like to post this task with a budget of UGX 150,000?`;
    } else {
      text = `Hello! I am Kivulu Client Assistant. Describe the task you need help with (e.g. electrical wiring, plumbing leak, router setup, deep cleaning), and I will match you with a vetted local professional and sync your Google Calendar.`;
    }
  }

  return {
    id: `msg_${Date.now()}`,
    sender: 'CLIENT_AGENT',
    senderTitle: 'gigClientAgent (Client Assistant)',
    message: text,
    timestamp: 'Just now',
    recommendedWorker: matchedWorker,
    actionSuggestions: [
      'Post New Task with AI',
      `Schedule with ${matchedWorker?.name || 'Worker'}`,
      'Browse All Vetted Workers'
    ]
  };
}

export async function processWorkerAgentQuery(
  userPrompt: string,
  worker?: GigWorker,
  apiKey?: string
): Promise<ChatMessage> {
  const lower = userPrompt.toLowerCase();
  let text = '';

  if (apiKey) {
    try {
      const genAI = new GoogleGenerativeAI(apiKey);
      const model = genAI.getGenerativeModel({ model: 'gemini-1.5-flash' });
      const prompt = `You are Kivulu Gig Worker Assistant (gigWorkerAgent). Your job is to onboard gig workers, assess and verify their skills and reliability, conduct interactive vetting interviews, and assist with calendar synchronization.
      Worker Name: ${worker?.name || 'New Worker'}.
      User query: "${userPrompt}". Guide the worker on getting vetted and earning badges.`;

      const result = await model.generateContent(prompt);
      text = result.response.text();
    } catch (e) {
      text = '';
    }
  }

  if (!text) {
    if (lower.includes('vet') || lower.includes('quiz') || lower.includes('test') || lower.includes('interview')) {
      text = `Welcome to Kivulu Worker Vetting! I will conduct a 3-question interactive skill assessment for ${worker?.category || 'your trade'}. Scoring above 60% unlocks the 'Kivulu Vetted & Approved' badge and direct client bookings!`;
    } else if (lower.includes('calendar') || lower.includes('auth') || lower.includes('google')) {
      text = `Google Calendar synchronization enabled! When clients book your services, Kivulu automatically reserves time slots on your calendar to prevent double bookings.`;
    } else {
      text = `Hello ${worker?.name || 'Worker'}! I am your Kivulu Worker Assistant. I am here to help you get vetted, manage task interest, verify tools, and boost your earnings.`;
    }
  }

  return {
    id: `msg_${Date.now()}`,
    sender: 'WORKER_AGENT',
    senderTitle: 'gigWorkerAgent (Worker Assistant)',
    message: text,
    timestamp: 'Just now',
    actionSuggestions: [
      'Start AI Vetting Interview',
      'Link Google Calendar',
      'Update Skill Matrix'
    ]
  };
}
