# Kivulu: Efficient Gig Marketplace & Multi-AI Agent Platform

Kivulu is a multi-agent gig marketplace platform connecting clients who need tasks done with vetted, skilled gig workers.

## Key Features

1. **AI Worker Vetting**:
   - Assesses and verifies gig worker skills through interactive technical interviews.
   - Generates AI Vetting Certificates with score badges and skill summaries.
2. **Precision Task Matching**:
   - Helps clients articulate detailed task descriptions.
   - Matches nearby vetted workers (Electrical, Plumbing, IT & Tech, Cleaning, Carpentry, Delivery) based on skills, location, rating, and tools.
3. **Google Calendar Synchronization**:
   - Automatically prepares event invites for matched tasks with start/end time, description, location, and attendee emails.
4. **Dual AI Agent System**:
   - `gigClientAgent`: Dedicated assistant helping clients refine requirements, discover top workers, and schedule appointments.
   - `gigWorkerAgent`: Dedicated assistant guiding worker onboarding, conducting skill quizzes, and managing calendar links.
5. **My Tasks & Ratings**:
   - Track active tasks, completed gigs, and submit client ratings & reviews.

## Architecture & Framework

- **Frontend**: React 18, TypeScript, Tailwind CSS, Lucide Icons
- **Build System**: Vite (Port 3000)
- **AI Integration**: Google Generative AI (Gemini API) & Intelligent Kivulu Multi-Agent Fallback
