# Kivulu: Efficient Gig Market Place
## Inspiration

The inspiration for Kivulu stems from my personal experiences, as Gig worker, the frustrations and inefficiencies within the gig economy for both workers and clients. For gig workers, the process of finding daily tasks is often a manual, physical search, leading to missed opportunities because tasks are not easily discoverable, I needed a system that can easily handle for me clients such that i can focus on doing my work. For individuals or businesses needing help, they face delays, unfinished tasks, being cheated, or spending excessive time and money to find expert gig workers.

## What it does

Kivulu is a multi-AI agent system designed to solve these problems. Its primary functions are:
1. Vetting Gig Workers: It assesses and verifies the skills and reliability of gig workers.
2. Task Matching: It automatically matches vetted workers to existing tasks, Most Important helps users with precise and clear tasks descriptions for the gig workers to easily understand.
3. Worker Discovery: It helps people who need services to easily find and connect with qualified gig workers.

## How we built it

Kivulu is built as a service  for deployment on Google Cloud Run. The technical architecture involves:
•Backend Service: A core agent built to be deployed on Google Cloud Run, which handles the main logic Using Google ADK with python3 , Gemini API, Google Workspace API.
•Communication Channel: Using The FastAPI webhook to integrates with Twilio to use WhatsApp as a user interface. Incoming messages to a designated Twilio WhatsApp number are sent to the Kivulu agent via a webhook for processing and The Agent Respond Via WhatsApp.
•Deployment Tools: The project utilizes the gcloud cloud run tools for packaging and deploying the agent to GCP(Google Cloud Platform).

## Challenges
The Challenges we ran into includes authorising the AI Agents to act on behalf of the users. This was solved easily by google authentication system, giving the User Control Over The Agent by authorising the Agents Using Google Authentication Systems.

## What Next For Kivulu
1. Testing with real users and customer acquisition
2. Iterative design and development from the users feedback
3. Adding the Property and Sales Agent Systems to Support the Property Owners and Small Shop Owners including small grocery stores to interact with their suppliers; The Farmers, helping Them Source Products From The Farmers and Make Sales Of these Products To The Buyers Nearby.

# Code Structure
The core application logic resides in the gig folder..
`cd gig`
'
Kivulu/gig
├── gigAgent/
│   ├── __init__.py
│   ├── agent.py (ADK Agent definition and prompt)
│   └── whatsapp_bidi_server.py (FastAPI entry point, Twilio webhook,and ADK Runner)
├── gigClientAgent/
│   ├── __init__.py
│   ├── agent.py (ADK Agent definition and prompt)
│   ├── google_calendar_tools.py (Google Calendar API interaction logic)
├── gigWorkerAgent/
│   ├── __init__.py
│   ├── agent.py (ADK Agent definition and prompt)
├── auth_server.py (Defines the FastAPI APIRouter for the Google OAuth flow)
├── requirements.txt
├── README.md
└── Dockerfile (Defines the production deployment environment)
'
# Setup and Deployment
This application is designed to be deployed using a Dockerfile to a platform like Google Cloud Run.
**1. Prerequisites**
`Twilio Account:` A Twilio account, phone number, and a WhatsApp sandbox or production number set up to receive webhooks.

`Google Cloud Project:` A project with the Google Calendar API enabled.
`Google OAuth Credentials:` An OAuth 2.0 Client ID for a "Web application" from the Google Cloud Console.

`Environment Variables:` You will need to set the following environment variables during deployment;
`TWILIO_ACCOUNT_SID`
`TWILIO_AUTH_TOKEN`
`TWILIO_PHONE_NUMBER` (e.g., whatsapp:+1415...)
`GOOGLE_API_KEY` (Gemini API Key)
`GOOGLE_CLIENT_ID` (From your OAuth Web application credentials)
`GOOGLE_CLIENT_SECRET` (From your OAuth Web application credentials)
`YOUR_PUBLIC_BASE_URL` (The public URL of your deployed Cloud Run service, e.g., https://gig-agent-xyz.a.run.app)
`GCP_PROJECT_ID`


**2. First Deployment Without `YOUR_PUBLIC_BASE_URL` to Google Cloud Run (Docker-based) to set up `YOUR_PUBLIC_BASE_URL`**
Use the following command to build the Docker image and deploy the service to Cloud Run.

Before First Deployment, Ensure the enviromental variables are available
**export enviromental variable on dev machine**
**Set Google Cloud Project eg `gi...-20..` and creat Gemini Studio API**
`export GOOGLE_GENAI_USE_VERTEXAI="True"`
`export GOOGLE_API_KEY="AIzaSyDBjOumTOVf10..."`
**set up Twilio account and obtain the details then export to env**
`export TWILIO_ACCOUNT_SID="AC6b4c6b4313043189....."`
`export TWILIO_AUTH_TOKEN="cd20871878bba8319....."`
`export TWILIO_PHONE_NUMBER="whatsapp:+141552...."`
**Enable Google Calendar API on GCP**
**set up Google Cloud Autho Screen for Web app service account and get the client details as below**
`export GOOGLE_CLIENT_ID=".....kqdvfr3s2h7c5g7a3db3k7h.apps.googleusercontent.com"`
`export GOOGLE_CLIENT_SECRET="......_fvdrr6Bef"`
`export GCP_PROJECT_ID="gi...-20.."`

**Then Deployment To Cloud Run On GCP**
Check if the env variables are available
`echo "GOOGLE_API_KEY: ${GOOGLE_API_KEY}"`
`echo "YOUR_PUBLIC_BASE_URL: ${YOUR_PUBLIC_BASE_URL}"`
If Empty, Please first export the enviromental variable before deployment
gcloud run deploy kivulu-gig-agent \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8085 \
  --set-env-vars TWILIO_ACCOUNT_SID="${TWILIO_ACCOUNT_SID}",TWILIO_AUTH_TOKEN="${TWILIO_AUTH_TOKEN}",TWILIO_PHONE_NUMBER="${TWILIO_PHONE_NUMBER}",GOOGLE_API_KEY="${GOOGLE_API_KEY}",GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID}",GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET}",YOUR_PUBLIC_BASE_URL="${YOUR_PUBLIC_BASE_URL}",GCP_PROJECT_ID="kivulu-beatrice"

**3. On Google Cloud Console IAM Setup**
**Enable Secret Manager API on GCP**
For the Roles of service account`5687...-compute@developer.gserviceaccount.com` field, grant the following two roles (even if you have "Admin," ensure "Accessor" is explicitly present):

`Secret Manager Secret Accessor` (This grants read-only access to the secret content, which is what get_google_calendar_credentials() needs).

`Secret Manager Admin` (If not already present).

Click Save.
**4.Then Authentication (CRITICAL ONE-TIME STEP)**
The agent needs permanent access to the gig worker's Google Calendar. This requires a one-time authorization step that generates the token.json file.
Ensure `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `YOUR_PUBLIC_BASE_URL` are exported in your local environment.
Ensure your `Google OAuth client` is configured to accept the redirect URI: 
`YOUR_PUBLIC_BASE_URL/oauth2callback`.
Open your browser and navigate to the public endpoint: `YOUR_PUBLIC_BASE_URL/authorize`.

**5. Lastly Configure Twilio Webhook**
After deployment:Obtain the Service URL of your Cloud Run service (e.g., https://gig-agent-xyz.a.run.app).

In your Twilio Console, set the WhatsApp Message Webhook URL for your number to:[Service URL]/whatsapp_webhook
Set the HTTP method to POST.

# Test Your Agent By Sending WhatsApp Message `Hello Agent`
The agent is now live and will respond to incoming WhatsApp messages, scheduling tasks using Google Calendar just like your PA.
