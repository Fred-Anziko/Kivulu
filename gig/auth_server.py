# Kivulu/gig/auth_server.py (Authorisation and Secret Management On GCP)
import os
import json
import logging
from fastapi import APIRouter, Request, Response
from fastapi.responses import RedirectResponse
from dotenv import load_dotenv
from google_auth_oauthlib.flow import Flow
from google.oauth2.credentials import Credentials
from google.cloud import secretmanager
from google.api_core import exceptions as gcp_exceptions

load_dotenv()
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# --- OAUTH CONFIGURATION ---
GOOGLE_CLIENT_ID = os.getenv("GOOGLE_CLIENT_ID", "YOUR_CLIENT_ID_NOT_SET")
GOOGLE_CLIENT_SECRET = os.getenv("GOOGLE_CLIENT_SECRET", "YOUR_SECRET_NOT_SET")
YOUR_PUBLIC_BASE_URL = os.getenv("YOUR_PUBLIC_BASE_URL", "http://localhost:8001")

if YOUR_PUBLIC_BASE_URL:
    YOUR_PUBLIC_BASE_URL = YOUR_PUBLIC_BASE_URL.strip()

REDIRECT_URI = f"{YOUR_PUBLIC_BASE_URL}/oauth2callback"
SCOPES = ["https://www.googleapis.com/auth/calendar"]
# --- SECRET MANAGER CONFIGURATION ---
PROJECT_ID = os.getenv("GCP_PROJECT_ID")
TOKEN_SECRET_NAME = "google_calendar_agent_token" 
SECRET_MANAGER_CLIENT = secretmanager.SecretManagerServiceClient()
TOKEN_SECRET_PATH = f"projects/{PROJECT_ID}/secrets/{TOKEN_SECRET_NAME}"

router = APIRouter()

# --- HELPER FUNCTIONS FOR SECRET MANAGER ---
def load_credentials_from_secret():
    """Loads credentials from Google Secret Manager."""
    if not PROJECT_ID:
        logging.error("GCP_PROJECT_ID is not set.")
        return None
    try:
        # Access the latest version of the secret
        response = SECRET_MANAGER_CLIENT.access_secret_version(request={"name": f"{TOKEN_SECRET_PATH}/versions/latest"})
        token_json = response.payload.data.decode("UTF-8")
        if not token_json:
             return None
             
        # Initialize the Credentials object from the stored JSON.
        creds = Credentials.from_authorized_user_info(json.loads(token_json), SCOPES)


        return creds
    except gcp_exceptions.NotFound:
        # Expected if the secret doesn't exist yet (first run)
        return None
    except Exception as e:
        # NOTE: A refresh failure due to an invalid/revoked refresh_token 
        # will now bubble up from the tool, correctly triggering a manual 
        # re-authorization via the /authorize endpoint.
        logging.error(f"Error loading credentials from Secret Manager: {e}")
        return None
        

def save_credentials_to_secret(creds: Credentials):
    """Saves credentials to Google Secret Manager."""
    if not PROJECT_ID:
        logging.error("GCP_PROJECT_ID is not set. Cannot save credentials.")
        return False
    
    token_json = creds.to_json()
    
    # Check if secret exists, create if not
    try:
        SECRET_MANAGER_CLIENT.get_secret(request={"name": TOKEN_SECRET_PATH})
    except gcp_exceptions.NotFound:
        logging.info(f"Secret {TOKEN_SECRET_NAME} not found. Creating it...")
        # Create the secret with automatic replication
        SECRET_MANAGER_CLIENT.create_secret(
            request={
                "parent": f"projects/{PROJECT_ID}",
                "secret_id": TOKEN_SECRET_NAME,
                "secret": {"replication": {"automatic": {}}},
            }
        )

    # Add a new version with the updated token content (saves the token)
    SECRET_MANAGER_CLIENT.add_secret_version(
        request={
            "parent": TOKEN_SECRET_PATH,
            "payload": {"data": token_json.encode("UTF-8")},
        }
    )
    logging.info("Credentials successfully saved to Google Secret Manager.")
    return True

# --- OAUTH ENDPOINTS ---

@router.get("/authorize")
async def authorize(request: Request):
    """
    Step 1: Initiates the Google OAuth 2.0 flow.
    Checks Secret Manager for existing credentials.
    """
    if GOOGLE_CLIENT_ID == "YOUR_CLIENT_ID_NOT_SET":
        return Response(content="ERROR: GOOGLE_CLIENT_ID not configured...", status_code=500)
    
    # --- CHECK PERSISTENT STORAGE ---
    creds = load_credentials_from_secret()
    if creds:
        logging.info("Credentials found in Secret Manager. Skipping authorization.")
        return Response(content="Agent is already authorized. Credentials found in Secret Manager.", media_type="text/plain", status_code=200)

    client_config = {
        "web": {
            "client_id": GOOGLE_CLIENT_ID,
            "client_secret": GOOGLE_CLIENT_SECRET,
            "auth_uri": "https://accounts.google.com/o/oauth2/auth",
            "token_uri": "https://oauth2.googleapis.com/token",
            "redirect_uris": [REDIRECT_URI]
        }
    }

    flow = Flow.from_client_config(
        client_config=client_config,
        scopes=SCOPES,
        redirect_uri=REDIRECT_URI
    )

    authorization_url, _ = flow.authorization_url(
        access_type='offline',
        include_granted_scopes='true',
        prompt='consent'
    )
    
    return RedirectResponse(authorization_url)

@router.get("/oauth2callback")
async def oauth2callback(request: Request):
    """
    Step 2: Receives the authorization code, exchanges it for a token, and saves it to Secret Manager.
    """
    code = request.query_params.get('code')
    error = request.query_params.get('error')

    if error:
        return Response(content=f"OAuth Authorization failed: {error}", status_code=400)
    
    if not code:
        return Response(content="Missing authorization code.", status_code=400)
        
    # Reconstruct the final secure URL for the token exchange
    query_params = str(request.url).split('?', 1)[1]
    authorization_response = f"{REDIRECT_URI}?{query_params}"
    # Recreate the Flow instance to fetch the token
    client_config = {
        "web": {
            "client_id": GOOGLE_CLIENT_ID,
            "client_secret": GOOGLE_CLIENT_SECRET,
            "auth_uri": "https://accounts.google.com/o/oauth2/auth",
            "token_uri": "https://oauth2.googleapis.com/token",
            "redirect_uris": [REDIRECT_URI]
        }
    }

    flow = Flow.from_client_config(
        client_config=client_config,
        scopes=SCOPES,
        redirect_uri=REDIRECT_URI
    )

    try:
        # Exchange the authorization code for tokens using the SECURE URL
        flow.fetch_token(authorization_response=authorization_response)
        
        creds = flow.credentials
        
        # --- NEW PERSISTENCE LOGIC: SAVE TO SECRET MANAGER ---
        if save_credentials_to_secret(creds):
            return Response(content="Authentication successful! Credentials securely saved to Secret Manager.", media_type="text/plain", status_code=200)
        else:
             return Response(content="Authentication successful, but failed to save credentials to Secret Manager. Check Cloud Run IAM permissions.", media_type="text/plain", status_code=500)

    except Exception as e:
        logging.error(f"Error during token exchange: {e}")
        return Response(content=f"Authentication failed during token exchange. Error: {e}", media_type="text/plain", status_code=500)