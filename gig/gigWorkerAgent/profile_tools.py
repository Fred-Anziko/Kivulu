from typing import Annotated, Any

from pydantic import Field, ValidationError

from persistence_models import WorkerProfileInput, tool_error, validation_errors


def prepare_worker_profile_draft(
    full_name: Annotated[str, Field(description="Worker's full name.")],
    email: Annotated[str, Field(description="Worker's contact email.")],
    whatsapp: Annotated[
        str, Field(description="Worker's WhatsApp number, or empty string if unavailable.")
    ],
    location: Annotated[str, Field(description="Worker's city or service area.")],
    skills: Annotated[list[str], Field(description="Worker's self-reported skills.")],
    experience: Annotated[str, Field(description="Worker's relevant experience.")],
    interested_tasks: Annotated[
        list[str], Field(description="Types of tasks the worker wants.")
    ],
    equipment: Annotated[list[str], Field(description="Equipment the worker has; [] if none.")],
    task_notifications_opt_in: Annotated[
        bool,
        Field(
            description=(
                "Whether the worker explicitly consents to receive task "
                "invitations in the Kivulu web inbox. Default to false unless "
                "they explicitly opt in."
            )
        ),
    ],
) -> dict[str, Any]:
    """Validate a proposed worker profile without writing anything to a database."""
    try:
        draft = WorkerProfileInput.model_validate(
            {
                "full_name": full_name,
                "email": email,
                "whatsapp": whatsapp,
                "location": location,
                "skills": skills,
                "experience": experience,
                "interested_tasks": interested_tasks,
                "equipment": equipment,
                "task_notifications_opt_in": task_notifications_opt_in,
            }
        )
    except ValidationError as error:
        return tool_error(
            "INVALID_ARGUMENT",
            "Worker profile draft is incomplete or invalid. Correct the listed fields before showing it for confirmation.",
            validation_errors(error),
        )
    return {
        "schema_version": 1,
        "status": "draft",
        "draft_type": "worker_profile",
        "draft": draft.model_dump(mode="json"),
        "message": "Draft validated only. Nothing has been saved.",
    }
