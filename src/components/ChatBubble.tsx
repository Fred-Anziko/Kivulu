import React from 'react';
import { ChatMessage, GigWorker } from '../types';
import { Bot, User, Star, Calendar } from 'lucide-react';

interface ChatBubbleProps {
  message: ChatMessage;
  onSuggestionClick: (suggestion: string) => void;
  onSelectWorker: (worker: GigWorker) => void;
}

export const ChatBubble: React.FC<ChatBubbleProps> = ({ message, onSuggestionClick, onSelectWorker }) => {
  const isUser = message.sender === 'USER';

  return (
    <div className={`flex flex-col mb-4 ${isUser ? 'items-end' : 'items-start'}`}>
      <div className="flex items-start space-x-2.5 max-w-[85%]">
        {!isUser && (
          <div className="w-8 h-8 rounded-full bg-teal-800 text-amber-400 flex items-center justify-center flex-shrink-0 shadow">
            <Bot className="w-4 h-4" />
          </div>
        )}

        <div>
          <span className="text-[11px] font-semibold text-gray-400 mb-1 block px-1">
            {message.senderTitle}
          </span>

          <div
            className={`p-4 rounded-2xl text-sm leading-relaxed shadow-sm ${
              isUser
                ? 'bg-teal-800 text-white rounded-tr-none'
                : 'bg-white text-gray-800 border border-gray-200 rounded-tl-none'
            }`}
          >
            <p className="whitespace-pre-line">{message.message}</p>

            {/* Recommended Worker Card inline in chat */}
            {message.recommendedWorker && (
              <div className="mt-3 bg-gray-50 border border-gray-200 rounded-xl p-3 flex items-center justify-between text-gray-900">
                <div className="flex items-center space-x-2.5">
                  <div className="w-9 h-9 rounded-full bg-teal-800 text-white font-bold text-xs flex items-center justify-center">
                    {message.recommendedWorker.name.charAt(0)}
                  </div>
                  <div>
                    <h4 className="font-bold text-xs">{message.recommendedWorker.name}</h4>
                    <p className="text-[11px] text-gray-500">{message.recommendedWorker.category} • {message.recommendedWorker.location}</p>
                    <div className="flex items-center space-x-1 text-[11px] text-amber-600 font-bold">
                      <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
                      <span>{message.recommendedWorker.rating}</span>
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => onSelectWorker(message.recommendedWorker!)}
                  className="bg-amber-500 hover:bg-amber-600 text-white text-xs font-bold px-3 py-1.5 rounded-lg flex items-center space-x-1 shadow-sm"
                >
                  <Calendar className="w-3 h-3" />
                  <span>Book</span>
                </button>
              </div>
            )}
          </div>

          {/* Action suggestion pills */}
          {!isUser && message.actionSuggestions && message.actionSuggestions.length > 0 && (
            <div className="flex flex-wrap gap-1.5 mt-2">
              {message.actionSuggestions.map((suggestion, idx) => (
                <button
                  key={idx}
                  onClick={() => onSuggestionClick(suggestion)}
                  className="bg-teal-50 hover:bg-teal-100 text-teal-800 text-xs font-semibold px-3 py-1 rounded-full border border-teal-200 transition-all"
                >
                  {suggestion}
                </button>
              ))}
            </div>
          )}
        </div>

        {isUser && (
          <div className="w-8 h-8 rounded-full bg-amber-500 text-white flex items-center justify-center flex-shrink-0 shadow">
            <User className="w-4 h-4" />
          </div>
        )}
      </div>
    </div>
  );
};
