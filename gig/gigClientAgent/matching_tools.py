import logging
from typing import Annotated, Any

from google.api_core.exceptions import GoogleAPICallError
from google.auth.exceptions import DefaultCredentialsError
from google.adk.tools import ToolContext
from pydantic import Field

from gigAgent.task_matching import list_task_invitations

logger = logging.getLogger(__name__)


def get_task_invitations(
    task_id: Annotated[str, Field(description="ID of the client's posted task.")],
    tool_context: ToolContext,
) -> dict[str, Any]:
    """Read the status of invited and responding workers without changing task data."""
    try:
        invitations = list_task_invitations(task_id, tool_context.user_id)
        return {
            "schema_version": 1,
            "status": "success",
            "task_id": task_id,
            "invitations": invitations,
        }
    except ValueError as error:
        return {
            "schema_version": 1,
            "status": "error",
            "error_code": "INVALID_ARGUMENT",
            "message": str(error),
        }
    except (DefaultCredentialsError, GoogleAPICallError, RuntimeError):
        logger.exception("Could not load worker invitations for task %s.", task_id)
        return {
            "schema_version": 1,
            "status": "error",
            "error_code": "PERSISTENCE_UNAVAILABLE",
            "message": "Worker invitation status is temporarily unavailable.",
        }
