import React, { useState } from 'react';
import { GigWorker, VettingInterview } from '../types';
import { VETTING_QUESTIONS } from '../mockData';
import { ShieldCheck, Calendar, Award, CheckCircle2, HelpCircle, Wrench, Sparkles } from 'lucide-react';

interface WorkerProfileViewProps {
  myProfile: GigWorker | null;
  activeInterview: VettingInterview | null;
  onStartInterview: (category: string) => void;
  onSubmitAnswer: (qIndex: number, optionIndex: number) => void;
  onRegisterWorker: (workerData: {
    name: string;
    email: string;
    phone: string;
    location: string;
    category: string;
    skillsStr: string;
    equipmentsStr: string;
    rate: string;
  }) => void;
}

export const WorkerProfileView: React.FC<WorkerProfileViewProps> = ({
  myProfile,
  activeInterview,
  onStartInterview,
  onSubmitAnswer,
  onRegisterWorker,
}) => {
  const [name, setName] = useState('Fredrick Anziko');
  const [email, setEmail] = useState('anzikofred@gmail.com');
  const [phone, setPhone] = useState('+256 772 102 938');
  const [location, setLocation] = useState('Kampala Central');
  const [category, setCategory] = useState('Electrical');
  const [skillsStr, setSkillsStr] = useState('Solar Wiring, Breaker Panel Repair, CCTV');
  const [equipmentsStr, setEquipmentsStr] = useState('Fluke Multimeter, Insulated Toolkit');
  const [rate, setRate] = useState('UGX 45,000 / hr');

  const handleRegister = (e: React.FormEvent) => {
    e.preventDefault();
    onRegisterWorker({
      name,
      email,
      phone,
      location,
      category,
      skillsStr,
      equipmentsStr,
      rate,
    });
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-6 pb-24">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-teal-900 flex items-center space-x-2">
          <ShieldCheck className="w-6 h-6 text-emerald-600" />
          <span>Worker Vetting & Skill Verification</span>
        </h1>
        <p className="text-xs text-gray-500">
          Kivulu AI assesses worker competence through interactive technical interviews & verifies Google Calendar links.
        </p>
      </div>

      {myProfile ? (
        <div className="space-y-6">
          {/* Profile Card */}
          <div className="bg-white rounded-2xl p-6 border border-gray-200/80 shadow-sm">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div className="flex items-center space-x-4">
                <div className="w-16 h-16 rounded-full bg-teal-800 text-white font-bold text-2xl flex items-center justify-center flex-shrink-0 shadow">
                  {myProfile.name.charAt(0)}
                </div>
                <div>
                  <div className="flex items-center space-x-2">
                    <h2 className="text-xl font-bold text-gray-900">{myProfile.name}</h2>
                    {myProfile.verificationStatus === 'VETTED_APPROVED' && (
                      <span className="bg-emerald-100 text-emerald-800 text-[11px] font-bold px-2.5 py-0.5 rounded-full flex items-center space-x-1">
                        <ShieldCheck className="w-3.5 h-3.5" />
                        <span>VETTED & APPROVED</span>
                      </span>
                    )}
                  </div>
                  <p className="text-xs font-semibold text-teal-800">{myProfile.category} • {myProfile.hourlyRate}</p>
                  <p className="text-xs text-gray-500">{myProfile.location} • {myProfile.phone}</p>
                </div>
              </div>

              <button
                onClick={() => onStartInterview(myProfile.category)}
                className="bg-amber-500 hover:bg-amber-600 text-white font-bold px-4 py-2.5 rounded-xl text-xs flex items-center justify-center space-x-1.5 shadow"
              >
                <Award className="w-4 h-4" />
                <span>Take Skill Quiz</span>
              </button>
            </div>

            <hr className="my-4 border-gray-100" />

            <div className="bg-teal-50 border border-teal-200 rounded-xl p-3.5 mb-4">
              <p className="text-xs font-bold text-teal-900 mb-1 flex items-center space-x-1">
                <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                <span>AI Vetting Summary</span>
              </p>
              <p className="text-xs text-teal-800">{myProfile.aiVettingSummary}</p>
            </div>

            <div className="flex items-center justify-between text-xs text-gray-600">
              <span className="flex items-center space-x-1 text-emerald-700 font-bold">
                <Calendar className="w-4 h-4" />
                <span>Google Calendar Sync Active</span>
              </span>
              <span className="font-semibold">{myProfile.completedTasksCount} Gigs Completed</span>
            </div>
          </div>

          {/* Active Interview Assessment */}
          {activeInterview && (
            <div className="bg-white rounded-2xl p-6 border border-gray-200/80 shadow-md">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-bold text-base text-teal-900 flex items-center space-x-2">
                  <HelpCircle className="w-5 h-5 text-amber-500" />
                  <span>AI Technical Interview: {activeInterview.category}</span>
                </h3>

                {activeInterview.status === 'COMPLETED_PASSED' && (
                  <span className="bg-emerald-100 text-emerald-800 text-xs font-bold px-3 py-1 rounded-full flex items-center space-x-1">
                    <CheckCircle2 className="w-4 h-4" />
                    <span>PASSED ({activeInterview.score}%)</span>
                  </span>
                )}
              </div>

              {activeInterview.status === 'IN_PROGRESS' || activeInterview.status === 'NOT_STARTED' ? (
                <div>
                  {activeInterview.questions[activeInterview.currentQuestionIndex] && (
                    <div className="space-y-4">
                      <p className="text-xs font-bold text-gray-400">
                        Question {activeInterview.currentQuestionIndex + 1} of {activeInterview.questions.length}
                      </p>
                      <p className="text-sm font-bold text-gray-800">
                        {activeInterview.questions[activeInterview.currentQuestionIndex].questionText}
                      </p>

                      <div className="space-y-2">
                        {activeInterview.questions[activeInterview.currentQuestionIndex].options.map((opt, optIdx) => {
                          const isSelected = activeInterview.userAnswers[activeInterview.currentQuestionIndex] === optIdx;
                          return (
                            <button
                              key={optIdx}
                              onClick={() => onSubmitAnswer(activeInterview.currentQuestionIndex, optIdx)}
                              className={`w-full text-left p-3.5 rounded-xl border text-xs font-medium transition-all ${
                                isSelected
                                  ? 'bg-teal-800 text-white border-teal-800 shadow-sm'
                                  : 'bg-gray-50 text-gray-800 border-gray-200 hover:bg-gray-100'
                              }`}
                            >
                              {opt}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4 text-emerald-900 text-xs leading-relaxed">
                  <p className="font-bold mb-1">Interview Assessment Complete!</p>
                  <p>{activeInterview.aiAssessmentSummary}</p>
                </div>
              )}
            </div>
          )}
        </div>
      ) : (
        /* Registration Form */
        <div className="bg-white rounded-2xl p-6 border border-gray-200/80 shadow-sm">
          <h2 className="text-lg font-bold text-teal-900 mb-1">Onboard as a Gig Worker</h2>
          <p className="text-xs text-gray-500 mb-5">
            Fill in your trade details to begin Kivulu's automated AI competence verification.
          </p>

          <form onSubmit={handleRegister} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Full Name</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3.5 py-2 border border-gray-300 rounded-xl text-xs focus:ring-2 focus:ring-teal-800 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Email Address</label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-3.5 py-2 border border-gray-300 rounded-xl text-xs focus:ring-2 focus:ring-teal-800 focus:outline-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">WhatsApp / Phone</label>
                <input
                  type="text"
                  required
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="w-full px-3.5 py-2 border border-gray-300 rounded-xl text-xs focus:ring-2 focus:ring-teal-800 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Location</label>
                <input
                  type="text"
                  required
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  className="w-full px-3.5 py-2 border border-gray-300 rounded-xl text-xs focus:ring-2 focus:ring-teal-800 focus:outline-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Trade Category</label>
                <input
                  type="text"
                  required
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-3.5 py-2 border border-gray-300 rounded-xl text-xs focus:ring-2 focus:ring-teal-800 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Hourly Rate</label>
                <input
                  type="text"
                  required
                  value={rate}
                  onChange={(e) => setRate(e.target.value)}
                  className="w-full px-3.5 py-2 border border-gray-300 rounded-xl text-xs focus:ring-2 focus:ring-teal-800 focus:outline-none"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1">Skills (comma separated)</label>
              <input
                type="text"
                value={skillsStr}
                onChange={(e) => setSkillsStr(e.target.value)}
                className="w-full px-3.5 py-2 border border-gray-300 rounded-xl text-xs focus:ring-2 focus:ring-teal-800 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1">Tools & Equipment</label>
              <input
                type="text"
                value={equipmentsStr}
                onChange={(e) => setEquipmentsStr(e.target.value)}
                className="w-full px-3.5 py-2 border border-gray-300 rounded-xl text-xs focus:ring-2 focus:ring-teal-800 focus:outline-none"
              />
            </div>

            <button
              type="submit"
              className="w-full bg-teal-800 hover:bg-teal-900 text-white font-bold py-2.5 rounded-xl text-xs transition-all shadow flex items-center justify-center space-x-2"
            >
              <ShieldCheck className="w-4 h-4 text-amber-400" />
              <span>Save & Begin AI Vetting Assessment</span>
            </button>
          </form>
        </div>
      )}
    </div>
  );
};
