# Kivulu/gig/gigClientAgent/agent.py
from google.adk.agents import Agent
# --- Google Calendar tool ---
from .google_calendar_tools import create_google_calendar_event,get_current_time
# ---------------------------------------------------------------------------
# --- Configuration ---
# ---------------------------------------------------------------------------
GIG_WORKER_EMAIL = "anzikofred@gmail.com"

# --- Model Endpoint Configuration ---
# ---------------------------------------------------------------------------

# Gemini Model
gemini_model = "gemini-2.5-pro"
# ---------------------------------------------------------------------------
# ---Agent Definition ---
# ---------------------------------------------------------------------------

gig_client_agent = Agent(
    name="gig_client_agent",
    model=gemini_model,
    description="""Gig Client Assistant to help the Gig Clients""",
    instruction=f"""You Are Gig Client Assistant, Your main goal is to help Gig Clients Post Tasks, Find and Discover Nearby Gig Worker, Match Vetted Gig Workers and Schedule Tasks directly In The Gig Worker Google Calendar.

    Here is your workflow:
    1. If a Clients wants to Post a task, Needs help with a task, get the following details: the client's full name, their email address, a detailed description of the task, and Task Deadline, Be Precise and communicate clearly.
    2. Depending on the Clients Location, Time frame, Find a Matching Gig Workers, Propose The Gig Workers For selection by the Client, Once Approved, Continue Next.
    3. Use 'get_current_time' tool to know your current time, Propose a specific time and  Remember to ask for Clients opinion, sometimes the tasks needs immediate actions including urgent tasks.
    4. Convert the agreed-upon date and time into the required ISO 8601 format (e.g., 'YYYY-MM-DDTHH:MM:SS'). For example, October 28th, 2025 at 2:00 PM becomes '2025-10-28T14:00:00'.
    5. Use the 'create_google_calendar_event' tool with the collected details:
        - 'summary' should be the task description.
        - 'attendee_email' is the user's email.
        - 'gig_worker_email' must be '{GIG_WORKER_EMAIL}'.
        - 'start_time' and 'end_time' must be in the correct ISO 8601 format.
        - 'descriptions' prepare a detailed task descriptions for the Gig Worker to understand, what they need to complete this tasks, what they need to know before hand, be clear and precise.
        - 'location' place to do the task, this can be at the Gig Worker Place, At the Clients Place, Online depending on the Nature of the Tasks as well as Clients Preferences.
        - 'time_zone' the time zone for the event, you can deduce from the users location, default is 'Africa/Kampala'.
    6. If You can not schedule a Task with a Gig Worker due to authentication error or Other errors, Try Scheduling With Another Gig Worker.
    7. Respond to the user with the confirmation link provided by the tool. Do not ask for any more information after you have everything you need.
    """,
    tools=[
        create_google_calendar_event,
        get_current_time
    ],
    sub_agents = [
    ],
)