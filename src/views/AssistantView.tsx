import React, { useState, useRef, useEffect } from 'react';
import { ChatMessage, GigWorker, UserRole } from '../types';
import { ChatBubble } from '../components/ChatBubble';
import { Bot, Send, Sparkles, UserCheck, ShieldCheck } from 'lucide-react';

interface AssistantViewProps {
  chatMessages: ChatMessage[];
  currentRole: UserRole;
  isAiThinking: Boolean;
  onSendMessage: (msg: string) => void;
  onRoleChange: (role: UserRole) => void;
  onSelectWorker: (worker: GigWorker) => void;
}

export const AssistantView: React.FC<AssistantViewProps> = ({
  chatMessages,
  currentRole,
  isAiThinking,
  onSendMessage,
  onRoleChange,
  onSelectWorker,
}) => {
  const [input, setInput] = useState('');
  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages, isAiThinking]);

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim()) return;
    onSendMessage(input);
    setInput('');
  };

  const agentTitle = currentRole === 'CLIENT' ? 'gigClientAgent' : 'gigWorkerAgent';
  const agentDesc = currentRole === 'CLIENT'
    ? 'Posting tasks, discovering vetted workers & scheduling Google Calendar events'
    : 'Worker onboarding, skill verification quiz & Google Calendar synchronization';

  return (
    <div className="max-w-4xl mx-auto px-4 py-4 h-[calc(100vh-5rem)] flex flex-col">
      {/* Agent Banner */}
      <div className="bg-teal-800 text-white p-4 rounded-2xl shadow-md flex items-center justify-between mb-4">
        <div className="flex items-center space-x-3">
          <div className="p-2.5 bg-amber-500 rounded-xl text-white shadow">
            <Bot className="w-6 h-6" />
          </div>
          <div>
            <h2 className="font-bold text-base flex items-center space-x-2">
              <span>Kivulu AI • {agentTitle}</span>
              <Sparkles className="w-4 h-4 text-amber-300" />
            </h2>
            <p className="text-xs text-teal-100/90">{agentDesc}</p>
          </div>
        </div>

        <button
          onClick={() => onRoleChange(currentRole === 'CLIENT' ? 'GIG_WORKER' : 'CLIENT')}
          className="bg-teal-900/80 hover:bg-teal-900 text-amber-300 border border-amber-400/30 text-xs font-bold px-3 py-1.5 rounded-xl transition-all"
        >
          {currentRole === 'CLIENT' ? 'Switch to Worker Agent' : 'Switch to Client Agent'}
        </button>
      </div>

      {/* Messages */}
      <div className="flex-1 bg-gray-50/80 border border-gray-200/80 rounded-2xl p-4 overflow-y-auto space-y-2 mb-3 shadow-inner">
        {chatMessages.map((msg) => (
          <ChatBubble
            key={msg.id}
            message={msg}
            onSuggestionClick={(suggestion) => onSendMessage(suggestion)}
            onSelectWorker={onSelectWorker}
          />
        ))}

        {isAiThinking && (
          <div className="flex items-center space-x-2 text-teal-800 text-xs font-semibold p-2">
            <div className="w-2 h-2 bg-teal-800 rounded-full animate-ping" />
            <span>{agentTitle} is analyzing request...</span>
          </div>
        )}
        <div ref={chatEndRef} />
      </div>

      {/* Quick Prompt Suggestions */}
      <div className="flex items-center space-x-2 overflow-x-auto pb-2 scrollbar-none mb-2">
        {currentRole === 'CLIENT' ? (
          <>
            <button
              onClick={() => onSendMessage('I need an emergency electrician to fix a tripping main breaker')}
              className="bg-white hover:bg-gray-100 text-gray-700 border border-gray-200 text-xs font-semibold px-3 py-1 rounded-full whitespace-nowrap shadow-sm"
            >
              ⚡ Fix Tripping Breaker
            </button>
            <button
              onClick={() => onSendMessage('Schedule overhead water tank float valve repair with Moses')}
              className="bg-white hover:bg-gray-100 text-gray-700 border border-gray-200 text-xs font-semibold px-3 py-1 rounded-full whitespace-nowrap shadow-sm"
            >
              🔧 Schedule Plumbing
            </button>
          </>
        ) : (
          <>
            <button
              onClick={() => onSendMessage('Start my AI skill assessment and vetting quiz')}
              className="bg-white hover:bg-gray-100 text-gray-700 border border-gray-200 text-xs font-semibold px-3 py-1 rounded-full whitespace-nowrap shadow-sm"
            >
              🛡️ Take Worker Vetting Quiz
            </button>
            <button
              onClick={() => onSendMessage('Link my Google Calendar for automatic client booking')}
              className="bg-white hover:bg-gray-100 text-gray-700 border border-gray-200 text-xs font-semibold px-3 py-1 rounded-full whitespace-nowrap shadow-sm"
            >
              📅 Connect Google Calendar
            </button>
          </>
        )}
      </div>

      {/* Chat Input */}
      <form onSubmit={handleSend} className="flex items-center space-x-2 bg-white p-2 rounded-2xl border border-gray-200 shadow-sm">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder={`Message ${agentTitle}...`}
          className="flex-1 px-4 py-2 text-sm text-gray-800 bg-transparent focus:outline-none"
        />
        <button
          type="submit"
          className="bg-teal-800 hover:bg-teal-900 text-white p-2.5 rounded-xl transition-all shadow"
        >
          <Send className="w-4 h-4" />
        </button>
      </form>
    </div>
  );
};
