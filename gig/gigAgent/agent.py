# Kivulu/gig/gigClientAgent/agent.py
from google.adk.agents import Agent
from gigClientAgent.agent import gig_client_agent
from gigWorkerAgent.agent import gig_worker_agent

# ---------------------------------------------------------------------------
# --- Configuration ---
# ---------------------------------------------------------------------------
# --- Model Endpoint Configuration ---
# ---------------------------------------------------------------------------

# Gemini Model
gemini_model = "gemini-2.5-pro"
# ---------------------------------------------------------------------------
# --- Agent Definition ---
# ---------------------------------------------------------------------------

root_agent = Agent(
    name="gig_agent",
    model=gemini_model,
    instruction=f"""You Are Kivulu User Assistant, a proactive assistance with the responsibility of coordinating  Gig Workers and people who need help with tasks, The Clients.
    Your main goal is to understand the Users Intention, Provide Customer Services and Delegate Tasks to sub_agents 'gigClientAgent' and 'gigWorkerAgent'.

    Here is your workflow:
    1. If new User, Introduce Yourself to the user, be polite, professional and communicate precisely and clearly.
    2. If a User needs help with a task, Client User, Then Delegate to the 'gigClientAgent' to handle the User.
    3. If a User do tasks for a living, Gig Worker, Then Delegate to the 'gigWorkerAgent' to handle the User.
    4. After the sub_agents respond, Respond to the user precisely,clearly and be professional. Do not ask for any more information after you have everything you need.
    """,
    tools=[
    ],
    sub_agents = [
        gig_client_agent,
        gig_worker_agent
    ],
)