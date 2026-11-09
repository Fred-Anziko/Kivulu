# Kivulu/gigAgent/whatsapp_bidi_server.py
import os
import json
import logging
from fastapi import FastAPI, Request, Response
from twilio.rest import Client as TwilioClient
from twilio.twiml.messaging_response import MessagingResponse
from dotenv import load_dotenv
from google.adk.runners import Runner
from google.adk.sessions import InMemorySessionService
from google.genai.types import Content, Part
from .agent import root_agent
from .web_bidirectional_server import router as web_router
import auth_server 

# Load environment variables
load_dotenv()
# --- Basic Logging Configuration ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# --- Environment Variables & Twilio Client ---
# IMPORTANT: Use os.getenv() for all environment variables for Cloud Run
TWILIO_ACCOUNT_SID = os.getenv("TWILIO_ACCOUNT_SID")
TWILIO_AUTH_TOKEN = os.getenv("TWILIO_AUTH_TOKEN")
TWILIO_PHONE_NUMBER = os.getenv("TWILIO_PHONE_NUMBER")

if not TWILIO_ACCOUNT_SID or not TWILIO_AUTH_TOKEN:
    logging.error("Twilio credentials not found in environment variables.")
twilio_client = TwilioClient(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)

app = FastAPI(title="Gig Agent WhatsApp Server")

# Include the router from auth_server.py to mount /authorize and /oauth2callback
app.include_router(auth_server.router)
app.include_router(web_router)

# --- ADK GLOBAL INSTANCES (INITIALIZED ONCE) ---
SESSION_SERVICE = InMemorySessionService()
APP_NAME = root_agent.name
RUNNER = Runner(
    agent=root_agent,
    app_name=APP_NAME,
    session_service=SESSION_SERVICE
)
# --- MANUALLY TRACK CREATED SESSIONS ---
CREATED_SESSIONS = set()


# --- Response Function  ---
def send_whatsapp_reply(to_number: str, agent_payload: str):
    try:
        data = json.loads(agent_payload)
        message_type = data.get("type")
        if message_type == "template":
            content_sid, content_variables = data.get("content_sid"), data.get("content_variables")
            twilio_client.messages.create(from_=TWILIO_PHONE_NUMBER, content_sid=content_sid, content_variables=json.dumps(content_variables), to=to_number)
            logging.info(f"Sent template message to {to_number}")
        elif message_type == "text":
            body_text = data.get("body")
            twilio_client.messages.create(from_=TWILIO_PHONE_NUMBER, body=body_text, to=to_number)
            logging.info(f"Sent text message to {to_number}")
    except (json.JSONDecodeError, AttributeError):
        twilio_client.messages.create(from_=TWILIO_PHONE_NUMBER, body=agent_payload, to=to_number)
        logging.info(f"Sent raw text message to {to_number}")
    except Exception as e:
        logging.error(f"Error sending WhatsApp reply: {e}")

@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "ok", "message": "Kivulu Gig Agent is running. WhatsApp webhook is ready."}


# --- Webhook Endpoint --
@app.post("/whatsapp_webhook")
async def whatsapp_webhook(request: Request):
    form_data = await request.form()
    incoming_message = form_data.get('Body', '').strip()
    from_number = form_data.get('From', '')

    logging.info(f"Received message: '{incoming_message}' from: {from_number}")
    if not incoming_message or not from_number:
        return Response(status_code=204)

    SSESSION_ID = from_number
    USER_ID = from_number
    if SSESSION_ID not in CREATED_SESSIONS:
        logging.info(f"First message from {SSESSION_ID}. Creating a new session.")
        await SESSION_SERVICE.create_session(
            app_name=APP_NAME,
            user_id=USER_ID,
            session_id=SSESSION_ID
        )
        CREATED_SESSIONS.add(SSESSION_ID)

    user_message_as_content = Content(parts=[Part(text=incoming_message)])

    logging.info(f"Calling RUNNER.run_async for session {SSESSION_ID}...")
    async for event in RUNNER.run_async(
        user_id=USER_ID,
        session_id=SSESSION_ID,
        new_message=user_message_as_content
    ):
        if event.is_final_response():
            if event.content and event.content.parts and event.content.parts[0].text:
                response_payload = event.content.parts[0].text
                logging.info(f"Agent final response for {USER_ID}: {response_payload}")
                send_whatsapp_reply(SSESSION_ID, response_payload)
                break

    return Response(content=str(MessagingResponse()), media_type="application/xml")

if __name__ == "__main__":
    import uvicorn
    # default for PORT when running locally
    port = int(os.environ.get("PORT", 8085))
    logging.info(f"Starting server on port {port}")
    uvicorn.run(app, host="0.0.0.0", port=port)