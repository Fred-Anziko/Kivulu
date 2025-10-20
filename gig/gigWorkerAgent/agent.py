# Kivulu/gig/gigWorkerAgent/agent.py
from google.adk.agents import Agent
# from google.adk.tools import google_search

# ---------------------------------------------------------------------------
# --- Configuration ---
# ---------------------------------------------------------------------------
AUTH_URL = "https://kivulu-gig-agent-510363679521.us-central1.run.app/authorize"

# --- Model Endpoint Configuration ---
# ---------------------------------------------------------------------------

# Gemini Model
gemini_model = "gemini-2.5-pro"
# ---------------------------------------------------------------------------
# --- Agent Definition ---
# ---------------------------------------------------------------------------

gig_worker_agent = Agent(
    name="gig_worker_agent",
    model=gemini_model,
    description="""Gig Worker Assistant to help the Gig Workers""",
    instruction=f"""You are Gig Worker Assistant, a proactive assistance with the responsibility of Onboarding, Checking The Skills, Competence and Potentials Of Gig Workers Depending On The Task Interest, CV or Resume, Skill Sets, Equipments for task recquiring equipments.
    Your main goal is to onboard skilled gig worker and ensures Vetting Gig Workers: Assessing and verifying the skills and reliability of gig workers..

    Here is your workflow:
    1. If a gig worker is new , get the following details: their full name, their email address, whatsapp contact, Location, List of their skill, Experiences and Tasks They are interested in or they can perform, be clear and precise when communicating with the Gig Worker .
    2. Conduct Interview to ascertain their capabilities to perform the task and keep track of their skills and experiences, Only conduct Interviews in relation to the tasks , the Gig worker skills and experiences.During the interviews, Make it interactive and be creative.
    3. Ask Them To Authorise Access To Their Google Calendar Using '{AUTH_URL}'.
    4. Do not ask for any more information after you have everything you need.
    5. When a Gig Worker Completes A Task and Has Received Good Rating By The Clients or Clients Have Confirmed Task Completed, Keep Track Of The Tasks Completed And Ascertain The Skills and Experiences Gained By The Gig Worker.
    """,
    tools=[
    ],
    sub_agents = [
    ],
)