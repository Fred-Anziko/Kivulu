from typing import Annotated, Any

from pydantic import Field, ValidationError

from persistence_models import ClientTaskInput, tool_error, validation_errors


def prepare_gig_task_draft(
    client_name: Annotated[str, Field(description="Client's full name.")],
    client_email: Annotated[str, Field(description="Client's contact email.")],
    title: Annotated[str, Field(description="Short title for the gig task.")],
    description: Annotated[
        str,
        Field(
            description="Short summary of the client-confirmed task."
        ),
    ],
    scope: Annotated[str, Field(description="Exact work included and excluded.")],
    deliverables: Annotated[
        list[str],
        Field(description="Concrete outputs the worker must deliver, one item per deliverable."),
    ],
    acceptance_criteria: Annotated[
        list[str],
        Field(description="Observable client-confirmed conditions for accepting completion, one criterion per item."),
    ],
    timing_access: Annotated[
        str, Field(description="Timing, site access, and any access constraints.")
    ],
    safety_requirements: Annotated[
        str,
        Field(
            description=(
                "Client-confirmed known hazards and precautions, or state what is "
                "unknown and must be checked before work starts."
            )
        ),
    ],
    location: Annotated[str, Field(description="Where the task will be performed.")],
    required_skills: Annotated[
        list[str],
        Field(
            description=(
                "Client-confirmed must-have skills. Use specific, plain-language "
                "skills; include at least one."
            )
        ),
    ],
    required_equipment: Annotated[
        list[str],
        Field(description="Must-have equipment; use [] if no specific equipment is required."),
    ],
    deadline: Annotated[
        str,
        Field(
            description="ISO date YYYY-MM-DD or timezone-aware ISO datetime."
        ),
    ],
    budget: Annotated[str, Field(description="Budget or empty string if undecided.")],
) -> dict[str, Any]:
    """Validate a proposed task form without writing anything to a database."""
    try:
        draft = ClientTaskInput.model_validate(
            {
                "client_name": client_name,
                "client_email": client_email,
                "title": title,
                "description": description,
                "scope": scope,
                "deliverables": deliverables,
                "acceptance_criteria": acceptance_criteria,
                "timing_access": timing_access,
                "safety_requirements": safety_requirements,
                "location": location,
                "required_skills": required_skills,
                "required_equipment": required_equipment,
                "deadline": deadline,
                "budget": budget,
            }
        )
    except ValidationError as error:
        return tool_error(
            "INVALID_ARGUMENT",
            "Task draft is incomplete or invalid. Correct the listed fields before showing it for confirmation.",
            validation_errors(error),
        )
    return {
        "schema_version": 1,
        "status": "draft",
        "draft_type": "client_task",
        "draft": draft.model_dump(mode="json"),
        "message": "Draft validated only. Nothing has been saved or posted.",
    }
