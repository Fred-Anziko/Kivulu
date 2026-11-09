# Kivulu: An Efficient Local Gig Marketplace

## Purpose, mission, and vision

### Purpose
Help clients find a suitable worker for their task with less time and effort, at a cost they can afford, while helping workers discover decent, relevant gigs quickly and build meaningful income.

### Mission
Make local gig work easier to find, agree on, and complete. Kivulu turns client requests into clear task requirements, helps clients connect with opted-in workers whose self-reported skills and service areas fit those requirements, and makes expectations and next steps visible to both sides.

### Vision
A fair, dependable local marketplace where clients can find the right help, receive quality work at a fair price, and save time—and where workers can choose worthwhile jobs, complete them with clear expectations, and earn a meaningful income.

Kivulu supports these goals with clear task briefs, relevant worker invitations, user-confirmed decisions, and an accountable completion process. It aims to improve the chance of a good match; it does not guarantee worker availability, a particular price, income, or work quality.

## For clients, Kivulu can:

- Turn text and photos into a clear task brief, asking about scope, deliverables, deadline, budget, access, and safety details.
- Help create a task draft for the client to review and edit before posting.
- Find opted-in workers who meet the task’s stated skill, equipment, and service-area requirements, then explain the match.
- Let the client invite workers and confirm who gets the task.
- Support completion review: the client can accept the work or raise a specific concern.

## For workers, Kivulu can:

- Help prepare and review a worker profile before the worker chooses to save it.
- Show relevant task invitations in a web inbox, where workers can accept or decline.
- Make the agreed task requirements available so workers can understand the scope before accepting.
- Let workers submit completion details and respond to client concerns.

## For both sides, Kivulu can:

- Collect reciprocal ratings after the client confirms completion.
- Organize a task disagreement into a voluntary, AI-assisted mediation process, without deciding who is at fault.
- Record private safety reports. If someone is in immediate danger, users should contact local emergency services.

Agents can draft and explain information, but users confirm changes through dashboard actions; the agents themselves do not directly commit these records. Matching and task-state changes are handled by deterministic server logic.

## Important limits:
Kivulu does not currently run background checks, independently verify skills or completed work, certify workplace safety, provide binding dispute rulings, or send push/email notifications. Photos can help describe visible details, but cannot prove hidden conditions or that a worksite is safe.

## Inspiration

The inspiration for Kivulu stems from my personal experiences, as Gig worker, the frustrations and inefficiencies within the gig economy for both workers and clients. For gig workers, the process of finding daily tasks is often a manual, physical search, leading to missed opportunities because tasks are not easily discoverable, I needed a system that can easily handle for me clients such that i can focus on doing my work. For individuals or businesses needing help, they face delays, unfinished tasks, being cheated, or spending excessive time and money to find expert gig workers.

## What it does

Kivulu is a multi-AI agent system designed to solve these problems. Its primary functions are:
1. Task quality and safety: It helps clients turn text and optional photos into clear, worker-ready task briefs that specify scope, deliverables, acceptance criteria, constraints, and known safety requirements. Photos are used for visible observations only; they are not saved in chat history and cannot establish hidden conditions or site safety.
2. Clear worker communication: It prepares confirmed requirements in a worker-ready brief and provides in-web task invitations, worker acceptance/decline, client-confirmed assignment, and a completion-review loop. Web push/email notifications and formal adjudication are not implemented; voluntary AI-assisted mediation is available for task disputes.
3. Worker trust: It collects self-reported profiles and guides workers through a future consent-based verification process. Identity, reference, and background checks are not currently integrated; profiles remain pending until an authorized verification process is implemented and reviewed.
4. Quality follow-up: Task posting requires a reviewed brief with separate scope, deliverables, acceptance criteria, timing/access, and safety requirements. Workers see these confirmed requirements in their invitation and task dashboard. Before deciding on completion, clients can request an on-demand AI comparison of the worker's text summary and evidence notes with each acceptance criterion. This one-shot review uses the lower-cost Gemini Flash model by default and sends only the task brief and submitted completion—not the user's broader chat history. The client still decides whether to accept or raise a concern; the AI does not verify work or certify quality.

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

Kivulu/gig

firestore_store.py (Firebase Admin SDK, Firestore persistence, user records)

persistence_models.py (Strict task/profile inputs and versioned tool results)

gigAgent/

  __init__.py

  agent.py (ADK Agent definition and prompt)

  whatsapp_bidi_server.py (FastAPI entry point, Twilio webhook,and ADK Runner)

gigClientAgent/

  __init__.py

  agent.py (ADK Agent definition and prompt)

  task_tools.py (Validates and persists confirmed client tasks)

  google_calendar_tools.py (Google Calendar API interaction logic)

gigWorkerAgent/

  __init__.py

  agent.py (ADK Agent definition and prompt)

  profile_tools.py (Validates and persists confirmed worker profiles)

  auth_server.py (Defines the FastAPI APIRouter for the Google OAuth flow)

  requirements.txt

  Dockerfile (Defines the production deployment environment)

Kivulu architecture diagram.png

README.md


# Setup and Deployment

Kivulu's combined Cloud Run entry point serves the web application, ADK web chat, WhatsApp webhook, and optional Google Calendar OAuth routes. The container uses `Kivulu/gig/Dockerfile` and listens on Cloud Run's injected `PORT`.

## Prerequisites

- A Google Cloud project with billing enabled, `gcloud` configured, and permissions to enable APIs, create a Cloud Run service, and configure its runtime service account.
- A Firebase project linked to the same Google Cloud project, with Google sign-in enabled and a Cloud Firestore database created.
- A Gemini API key for the Google AI Developer API. This guide uses the API-key configuration (`GOOGLE_GENAI_USE_VERTEXAI=FALSE`).
- A Twilio WhatsApp sandbox or production sender and its account credentials. The combined entry point imports the Twilio client, so configure these before deploying it.
- A Google OAuth web client and the Google Calendar API only if using calendar integration.
- The Firebase CLI (`npm install -g firebase-tools`) for deploying Firestore rules and indexes.

Do not put API keys, OAuth client secrets, Twilio credentials, service-account keys, or access tokens in source control or command-line arguments. Store runtime secrets in Google Secret Manager.

### Web chat, Google sign-in, and Firestore

The web chat is served from the same Cloud Run service as the WhatsApp webhook. Its landing page is available at `/`; the WhatsApp webhook remains at `/whatsapp_webhook`, and the health check is at `/health`.

1. In Firebase Console, create or select a Firebase project, enable **Authentication → Google**, create a Cloud Firestore database, and register a web app.
2. Record the web app's public configuration and use it for these Cloud Run environment variables:
   - `FIREBASE_API_KEY`
   - `FIREBASE_AUTH_DOMAIN`
   - `FIREBASE_PROJECT_ID`
   - `FIREBASE_APP_ID`
3. From `Kivulu/gig`, deploy the included Firestore rules and indexes to the intended project:

   ```bash
   firebase deploy --project YOUR_PROJECT_ID --only firestore:rules,firestore:indexes
   ```

   The rules allow users to read only their own chat history and deny browser writes; the server writes through the Firebase Admin SDK. The index supports listing a user's tasks by newest first.
4. After deployment, add the Cloud Run service domain to Firebase Authentication's authorized domains. The browser Firebase configuration is public; never put service-account credentials or private keys in it.

The Cloud Run runtime service account uses Application Default Credentials to access Firestore. Grant it **Cloud Datastore User** (`roles/datastore.user`); Firebase ID-token signature verification does not require downloading a service-account key or granting the service account Firebase Authentication user-management access.

The web client signs in with Google through Firebase Authentication. It sends its Firebase ID token to the authenticated WebSocket at `/ws/chat`; the server verifies the token, runs the existing Kivulu ADK agent, and stores chat messages under `users/{uid}/conversations/{conversationId}/messages` in Firestore.

Agents can prepare validated task and worker-profile drafts from conversation details, but they cannot write them to Firestore. The server validates the draft and returns an editable dashboard form; only the signed-in user can explicitly confirm it through the authenticated task/profile API. Task creation uses an idempotency key so a safe retry does not create duplicates. Other task, invitation, completion, mediation, safety-report, and rating changes are committed through authenticated dashboard actions with server-side validation and state checks. Writes use the Firebase Admin SDK, never direct browser database writes. The authenticated `GET /api/me` endpoint returns the signed-in user's saved profile, up to 50 client tasks, and up to 50 assigned worker tasks. Worker skills remain marked as self-reported (`vettingStatus: pending`) until verification is implemented.

Draft tools require every field in their ADK argument schema, validate with strict Pydantic models, and return versioned JSON (`schema_version: 1`) without persisting. The server validates the draft again at commit time. The task deadline must be an ISO date or a timezone-aware ISO datetime.

Task matching uses deterministic hard filters: a worker must have explicitly opted in, match every client-confirmed required skill and must-have equipment item, and declare an overlapping service area. Stated task interest is used only as a ranking tie-breaker. The system invites up to five workers in the signed-in web inbox; a worker's acceptance is not an assignment, and the client must confirm the specific worker. Skills and availability are self-reported; no background check, live availability, travel distance, or quality score is used. Matches and invitation reasons are recorded in Firestore. Web inbox updates poll every 30 seconds; push/email notifications are not configured.

The web chat accepts one PNG, JPEG, or WebP photo up to 2 MB with an optional text message. Photos are analyzed with the current message but are not stored in Firestore chat history. The task form and server require separate, non-empty fields for scope, deliverables, one or more observable acceptance criteria, timing/access, and safety requirements before a task can be posted. If safety details are unknown, the client can record that they are unknown and must be checked before work starts. The client reviews and edits the brief before confirming. Confirmed task requirements are included in web invitations and shown as a checklist on invitation and assigned-task cards; workers can accept or decline, and clients confirm assignments in their web dashboard.

The worker inbox and client task controls are authenticated web endpoints (`/api/worker/invitations`, `/api/worker/invitations/{id}/response`, `/api/tasks/{id}/match`, `/api/client/invitations`, `/api/tasks/{id}/assignment`, `/api/tasks/{id}/completion`, `/api/tasks/{id}/completion-review`, `/api/tasks/{id}/ratings`, `/api/tasks/{id}/mediation`, and `/api/tasks/{id}/safety-reports`, plus mediation response, message, proposal, reopen, and context routes). The matching engine is deterministic and explainable; use Gemini to clarify free-form tasks and photos, not to decide eligibility, verify workers, or override the explicit matching constraints.

After assignment, the worker can submit a completion summary and optional text evidence notes in the web dashboard. While the client reviews a submission, they can request an on-demand AI organizer to compare the submitted text with every saved acceptance criterion and label each one “addressed,” “not addressed,” or “unclear,” with a reason and evidence limitations. The dedicated authenticated endpoint rechecks task ownership and review status, loads only the agreed criteria and completion submission, and validates that the model returned every exact criterion in order. The organizer is advisory: it cannot inspect the work, verify claims, or decide whether the task passes. Its model defaults to `gemini-2.5-flash`; set `KIVULU_COMPLETION_REVIEW_MODEL` to use another supported model. The client can accept completion or record a concern; a concern allows the worker to address it and resubmit, while acceptance marks the task completed. If the parties disagree about completion or quality, either can open mediation from the task card, submit their account and preferred outcome, clarify the record, and propose a practical resolution. Kivulu's AI mediator can summarize the agreed brief, both accounts, evidence actually provided, disagreements, and unknowns, then suggest voluntary next steps. It must not decide fault, certify work, or treat statements as verified facts. A resolution closes a case only when both participants accept it; either may reopen it within 30 days. This is a voluntary platform mediation aid, not legal advice or formal adjudication.

Safety concerns are logged separately as private reports and do not change task status or alert a reviewer. The dashboard warns that if anyone is in immediate danger, they should stop unsafe work if safe and contact local emergency services. Kivulu does not monitor these reports or certify that a worksite is safe.

After accepted completion, each participant can submit one 1-5 rating with optional task-related feedback within 14 days. Ratings are hidden until both participants submit or the 14-day window closes. Ratings cannot be submitted during unresolved mediation, but the original deadline continues to run. Published ratings and feedback are participant opinions, not verified skills or an objective quality certification. The dashboard displays separate client and worker rating averages based on up to 50 recent tasks in each role.

### Run Kivulu locally

The backend and web chat run together from `Kivulu/gig`. Use Python 3.11 to match the Docker image:

```bash
cd Kivulu/gig
python3.11 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
```

Configure Gemini and Firebase web-client settings in your shell (use your own project values; do not commit credentials):

```bash
export GOOGLE_GENAI_USE_VERTEXAI="FALSE"
export GOOGLE_API_KEY="your-gemini-api-key"
export FIREBASE_PROJECT_ID="your-firebase-project-id"
export FIREBASE_API_KEY="your-firebase-web-api-key"
export FIREBASE_AUTH_DOMAIN="your-firebase-project.firebaseapp.com"
export FIREBASE_APP_ID="your-firebase-web-app-id"
```

Firestore and Firebase ID-token verification use Google Application Default Credentials. For local development, sign in with `gcloud auth application-default login` and set the intended project with `gcloud config set project YOUR_PROJECT_ID`. Ensure the local identity has the required Firebase Authentication and Firestore permissions. Add `localhost` to Firebase Authentication's authorized domains.

Twilio credentials are only needed to send WhatsApp replies. Set `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, and `TWILIO_PHONE_NUMBER` to test WhatsApp; configure the Twilio webhook to your publicly reachable `/whatsapp_webhook` URL. Calendar authorization additionally needs `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GCP_PROJECT_ID`, and a reachable `YOUR_PUBLIC_BASE_URL`.

Start the web-chat server directly:

```bash
uvicorn gigAgent.web_bidirectional_server:app --host 127.0.0.1 --port 8085 --reload
```

Run that command from `Kivulu/gig`. If your terminal is at the repository root instead, use:

```bash
uvicorn --app-dir Kivulu/gig gigAgent.web_bidirectional_server:app --host 127.0.0.1 --port 8085 --reload
```

Open `http://localhost:8085` for the landing page and web chat. Check `http://localhost:8085/health` for service readiness. This entry point serves the web chat only; run `uvicorn gigAgent.whatsapp_bidi_server:app --host 127.0.0.1 --port 8085 --reload` when you also need the WhatsApp webhook and calendar OAuth routes. The graph-based `kivulu_trust_and_quality_workflow` routes conversations to worker-trust or client/task-quality specialists; it does not itself run background checks or record task completion. Set `PORT` to change the listening port.

Run the backend tests from `Kivulu/gig` with:

```bash
python -m unittest discover -s tests -v
```

## Deploy to Google Cloud Run

Run the commands below from the repository root unless a step says otherwise. Replace placeholder project IDs and regions with your own values. The `--source` build uses the Dockerfile in `Kivulu/gig`; do not deploy from the repository root.

### 1. Configure the Google Cloud project

```bash
PROJECT_ID="your-gcp-project-id"
REGION="us-central1"
SERVICE_NAME="kivulu-gig-agent"
SERVICE_ACCOUNT="${SERVICE_NAME}-runtime@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud config set project "$PROJECT_ID"
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  --project="$PROJECT_ID"
```

If using Google Calendar integration, also enable `calendar-json.googleapis.com`. The Firebase project, Firestore database, and `FIREBASE_PROJECT_ID` should refer to this same project.

Create a dedicated runtime service account and grant it only the access the application needs:

```bash
gcloud iam service-accounts create "${SERVICE_NAME}-runtime" \
  --project="$PROJECT_ID" \
  --display-name="Kivulu Cloud Run runtime"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/datastore.user"
```

### 2. Store credentials in Secret Manager

Create secrets in Secret Manager and add their values there. Do not paste credentials into this README, source code, deployment command arguments, or shell history.

Required secrets for the combined web and WhatsApp service:

- `kivulu-gemini-api-key` — Gemini API key.
- `kivulu-twilio-account-sid` — Twilio Account SID.
- `kivulu-twilio-auth-token` — Twilio Auth Token.

For Google Calendar integration, also create `kivulu-google-oauth-client-secret`. The OAuth Client ID is not a secret and is configured as an environment variable below.

Grant the Cloud Run runtime service account **Secret Manager Secret Accessor** on each secret the service reads. If using Calendar OAuth, pre-create a `google_calendar_agent_token` secret and grant the runtime service account **Secret Manager Secret Accessor** and **Secret Manager Secret Version Adder** on that secret. Pre-creating it avoids granting broad Secret Manager administration permissions to the running service.

### 3. Deploy the service

Set the Firebase web app values from Firebase Console. Set the Twilio sender in WhatsApp address format, for example `whatsapp:+<country-code><number>`.

```bash
cd Kivulu/gig

gcloud run deploy "$SERVICE_NAME" \
  --source . \
  --project="$PROJECT_ID" \
  --region="$REGION" \
  --service-account="$SERVICE_ACCOUNT" \
  --allow-unauthenticated \
  --port=8085 \
  --timeout=3600 \
  --set-env-vars="GOOGLE_GENAI_USE_VERTEXAI=FALSE,GCP_PROJECT_ID=${PROJECT_ID},FIREBASE_PROJECT_ID=${PROJECT_ID},FIREBASE_API_KEY=YOUR_FIREBASE_WEB_API_KEY,FIREBASE_AUTH_DOMAIN=YOUR_FIREBASE_AUTH_DOMAIN,FIREBASE_APP_ID=YOUR_FIREBASE_WEB_APP_ID,TWILIO_PHONE_NUMBER=whatsapp:+YOUR_TWILIO_NUMBER" \
  --set-secrets="GOOGLE_API_KEY=kivulu-gemini-api-key:latest,TWILIO_ACCOUNT_SID=kivulu-twilio-account-sid:latest,TWILIO_AUTH_TOKEN=kivulu-twilio-auth-token:latest"
```

Replace the `YOUR_FIREBASE_*` and `YOUR_TWILIO_NUMBER` placeholders before running the command. Do not include real secret values in `--set-env-vars`; secret values are injected from Secret Manager. The Cloud Run URL is assigned after the first deployment.

If enabling Google Calendar integration, add `GOOGLE_CLIENT_ID` as an environment variable and map `GOOGLE_CLIENT_SECRET` to `kivulu-google-oauth-client-secret` with `--set-secrets`. Configure an OAuth client of type **Web application**; the Google OAuth client is for the Kivulu Calendar integration, not the Cloud Run service account.

### 4. Configure URLs and verify the deployment

Get the service URL and set it as `YOUR_PUBLIC_BASE_URL`:

```bash
SERVICE_URL="$(gcloud run services describe "$SERVICE_NAME" \
  --project="$PROJECT_ID" \
  --region="$REGION" \
  --format='value(status.url)')"

gcloud run services update "$SERVICE_NAME" \
  --project="$PROJECT_ID" \
  --region="$REGION" \
  --update-env-vars="YOUR_PUBLIC_BASE_URL=${SERVICE_URL}"

curl --fail "${SERVICE_URL}/health"
```

For Calendar integration, add `${SERVICE_URL}/oauth2callback` as an authorized redirect URI in the Google OAuth client, set `GOOGLE_CLIENT_ID` and the `GOOGLE_CLIENT_SECRET` secret, then visit `${SERVICE_URL}/authorize` and complete the consent flow. Add the Cloud Run domain to Firebase Authentication's **Authorized domains**. In Twilio Console, set the WhatsApp incoming-message webhook to `${SERVICE_URL}/whatsapp_webhook` with HTTP method `POST`.

The public Cloud Run ingress is required for the browser and Twilio webhooks; authenticated application APIs still require Firebase ID tokens. Check startup and request logs with:

```bash
gcloud run services logs read "$SERVICE_NAME" \
  --project="$PROJECT_ID" \
  --region="$REGION" \
  --limit=50
```

### Deployment readiness limitations

This deployment runs the combined web/WhatsApp application, but it does not make all agent state durable. ADK sessions use `InMemorySessionService`; WhatsApp conversation state is lost when an instance restarts and is not shared across instances. Web chat messages are stored in Firestore and used to reconstruct chat history, but an interrupted agent run is not checkpointed or resumed. Before relying on WhatsApp continuity or multi-step agent work in production, use a persistent ADK session store and add webhook idempotency/retry handling.

Before accepting public production traffic, add and verify Twilio webhook signature validation. Also test authentication, Firestore rules, OAuth consent, secret access, and the task confirmation flow in a staging project. The health endpoint confirms the web process is responding; it does not validate Gemini, Firestore, Twilio, or Secret Manager connectivity.
