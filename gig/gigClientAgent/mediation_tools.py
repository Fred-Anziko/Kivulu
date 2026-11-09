import logging
from typing import Annotated, Any

from google.api_core.exceptions import GoogleAPICallError
from google.auth.exceptions import DefaultCredentialsError
from google.adk.tools import ToolContext
from pydantic import Field

from gigAgent.task_matching import get_task_mediation_context as load_mediation_context

logger = logging.getLogger(__name__)


def get_task_mediation_context(
    task_id: Annotated[str, Field(description="ID of the task with an open or resolved mediation case.")],
    tool_context: ToolContext,
) -> dict[str, Any]:
    """Load both participants' mediation statements for a neutral, evidence-grounded summary."""
    try:
        return {
            "schema_version": 1,
            "status": "success",
            "case": load_mediation_context(task_id, tool_context.user_id),
        }
    except ValueError as error:
        return {
            "schema_version": 1,
            "status": "error",
            "error_code": "INVALID_ARGUMENT",
            "message": str(error),
        }
    except (DefaultCredentialsError, GoogleAPICallError, RuntimeError):
        logger.exception("Could not load mediation context for task %s.", task_id)
        return {
            "schema_version": 1,
            "status": "error",
            "error_code": "PERSISTENCE_UNAVAILABLE",
            "message": "Mediation details are temporarily unavailable.",
        }
