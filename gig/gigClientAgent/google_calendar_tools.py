import logging
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
import time
import auth_server
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
def create_google_calendar_event(
    summary: str,
    start_time: str,
    end_time: str,
    attendee_email: str,
    gig_worker_email: str,
    description: str = "Task scheduling confirmation.",
    location: str = "Online",
    time_zone: str = "Africa/Kampala",
) -> str:
    """
    Creates a Google Calendar event with the specified details.
    """
    
    try:
        creds = auth_server.load_credentials_from_secret()
    except ValueError as e:
        # This error message directs the owner to the separate auth flow
        return f"Authentication failed: {e}. **Action required:** The Gig Worker must visit the web application's /authorize endpoint to grant calendar access."
    except Exception as e:
        return f"Authentication failed: Error during credential loading/refresh. Error: {e}"
    if not creds:
        # if load_credentials_from_secret returns None 
        # (e.g., if GCP_PROJECT_ID is missing or the secret is NotFound)
        return "**Action required:** The Gig Worker must visit the web application's /authorize endpoint to grant calendar access."
    try:
        service = build("calendar", "v3", credentials=creds)
        event = {
            "summary": summary,
            "location": location,
            "description": description,
            "start": {
                "dateTime": start_time,
                "timeZone": time_zone,
            },
            "end": {
                "dateTime": end_time,
                "timeZone": time_zone,
            },
            "attendees": [
                {"email": attendee_email},
                {"email": gig_worker_email},
            ],
            "reminders": {
                "useDefault": False,
                "overrides": [
                    {"method": "email", "minutes": 24 * 60},
                    {"method": "popup", "minutes": 10},
                ],
            },
        }

        event = service.events().insert(calendarId="primary", body=event).execute()
        return f"Event created successfully. View it here: {event.get('htmlLink')}"

    except HttpError as error:
        return f"An error occurred: {error}"

def get_current_time():
    """Output Todays Date, Current Time and Year eg Sun Oct 12 06:49:12 2025
    You Can Use It to Know Todays Date and Current Time
    """
    return time.ctime()