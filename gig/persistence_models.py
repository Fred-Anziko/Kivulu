import re
from datetime import date, datetime
from typing import Any, Literal

from pydantic import (
    BaseModel,
    ConfigDict,
    Field,
    ValidationError,
    field_validator,
    model_validator,
)


class PersistenceInput(BaseModel):
    model_config = ConfigDict(
        extra="forbid", strict=True, str_strip_whitespace=True
    )


class ToolFieldError(BaseModel):
    field: str
    message: str


class PersistenceToolError(BaseModel):
    schema_version: Literal[1] = 1
    status: Literal["error"] = "error"
    error_code: Literal["INVALID_ARGUMENT", "PERSISTENCE_UNAVAILABLE"]
    message: str
    field_errors: list[ToolFieldError] | None = None


class WorkerProfileSaved(BaseModel):
    schema_version: Literal[1] = 1
    status: Literal["success"] = "success"
    record_type: Literal["worker_profile"] = "worker_profile"
    record_id: str
    vetting_status: Literal["pending"] = "pending"
    message: str


class ClientTaskSaved(BaseModel):
    schema_version: Literal[1] = 1
    status: Literal["success"] = "success"
    record_type: Literal["task"] = "task"
    record_id: str
    task_status: Literal["posted"] = "posted"
    message: str


class WorkerProfileInput(PersistenceInput):
    full_name: str = Field(min_length=2, max_length=120)
    email: str = Field(min_length=3, max_length=254)
    whatsapp: str = Field(default="", max_length=40)
    location: str = Field(min_length=2, max_length=240)
    skills: list[str] = Field(min_length=1, max_length=30)
    experience: str = Field(min_length=1, max_length=3000)
    interested_tasks: list[str] = Field(min_length=1, max_length=30)
    equipment: list[str] = Field(default_factory=list, max_length=30)
    task_notifications_opt_in: bool

    @field_validator("email")
    @classmethod
    def validate_email(cls, value: str) -> str:
        if not re.fullmatch(r"[^@\s]+@[^@\s]+\.[^@\s]+", value):
            raise ValueError("Enter a valid email address.")
        return value

    @field_validator("whatsapp")
    @classmethod
    def validate_whatsapp(cls, value: str) -> str:
        if not value:
            return value
        normalized = re.sub(r"[\s().-]", "", value.removeprefix("whatsapp:"))
        if not re.fullmatch(r"\+[1-9]\d{7,14}", normalized):
            raise ValueError("Enter a WhatsApp number in international format, e.g. +256700000000.")
        return normalized

    @field_validator("skills", "interested_tasks", "equipment")
    @classmethod
    def validate_list_items(cls, values: list[str]) -> list[str]:
        if any(not value.strip() for value in values):
            raise ValueError("Items must be non-empty.")
        cleaned = list(dict.fromkeys(value.strip() for value in values))
        if any(len(value) > 120 for value in cleaned):
            raise ValueError("List items must not exceed 120 characters.")
        return cleaned


class ClientTaskInput(PersistenceInput):
    client_name: str = Field(min_length=2, max_length=120)
    client_email: str = Field(min_length=3, max_length=254)
    title: str = Field(min_length=3, max_length=160)
    description: str = Field(min_length=10, max_length=4000)
    scope: str = Field(min_length=3, max_length=2000)
    deliverables: list[str] = Field(min_length=1, max_length=20)
    acceptance_criteria: list[str] = Field(min_length=1, max_length=20)
    timing_access: str = Field(min_length=3, max_length=1000)
    safety_requirements: str = Field(min_length=3, max_length=1000)
    location: str = Field(min_length=2, max_length=240)
    required_skills: list[str] = Field(min_length=1, max_length=30)
    required_equipment: list[str] = Field(default_factory=list, max_length=30)
    deadline: str = Field(min_length=10, max_length=40)
    budget: str = Field(default="", max_length=120)

    @field_validator("client_email")
    @classmethod
    def validate_email(cls, value: str) -> str:
        if not re.fullmatch(r"[^@\s]+@[^@\s]+\.[^@\s]+", value):
            raise ValueError("Enter a valid client email address.")
        return value

    @field_validator(
        "deliverables",
        "acceptance_criteria",
        "required_skills",
        "required_equipment",
    )
    @classmethod
    def validate_requirement_items(cls, values: list[str]) -> list[str]:
        if any(not value.strip() for value in values):
            raise ValueError("Items must be non-empty.")
        cleaned = list(dict.fromkeys(value.strip() for value in values))
        if any(len(value) > 120 for value in cleaned):
            raise ValueError("Requirement items must not exceed 120 characters.")
        return cleaned

    @field_validator("deadline")
    @classmethod
    def normalize_deadline(cls, value: str) -> str:
        if re.fullmatch(r"\d{4}-\d{2}-\d{2}", value):
            return date.fromisoformat(value).isoformat()
        if not re.fullmatch(
            r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d{1,6})?)?(?:Z|[+-]\d{2}:\d{2})",
            value,
        ):
            raise ValueError(
                "Use an ISO date (YYYY-MM-DD) or ISO datetime with a timezone."
            )
        try:
            parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
        except ValueError as error:
            raise ValueError(
                "Use an ISO date (YYYY-MM-DD) or ISO datetime with a timezone."
            ) from error
        if parsed.tzinfo is None or parsed.utcoffset() is None:
            raise ValueError("Datetime deadlines must include a timezone.")
        return parsed.isoformat()


class TaskCompletionInput(PersistenceInput):
    summary: str = Field(min_length=10, max_length=2000)
    evidence: str = Field(default="", max_length=2000)

    @field_validator("summary", "evidence")
    @classmethod
    def strip_completion_text(cls, value: str) -> str:
        return value.strip()


class TaskCompletionDecisionInput(PersistenceInput):
    decision: Literal["accepted", "concern"]
    note: str = Field(default="", max_length=2000)

    @field_validator("note")
    @classmethod
    def strip_note(cls, value: str) -> str:
        return value.strip()

    @model_validator(mode="after")
    def require_concern_details(self) -> "TaskCompletionDecisionInput":
        if self.decision == "concern" and len(self.note) < 10:
            raise ValueError("Explain the concern in at least 10 characters.")
        return self


class CompletionCriterionAssessment(PersistenceInput):
    criterion: str = Field(min_length=1, max_length=500)
    status: Literal["addressed", "not_addressed", "unclear"]
    reason: str = Field(min_length=1, max_length=1000)


class CompletionEvidenceOrganizerOutput(PersistenceInput):
    task_id: str = Field(min_length=1, max_length=128)
    overview: str = Field(min_length=1, max_length=1500)
    assessments: list[CompletionCriterionAssessment] = Field(min_length=1, max_length=20)
    limitations: list[str] = Field(default_factory=list, max_length=10)

    @field_validator("limitations")
    @classmethod
    def strip_limitations(cls, values: list[str]) -> list[str]:
        if any(not value.strip() for value in values):
            raise ValueError("Limitations must be non-empty.")
        return [value.strip() for value in values]

    @model_validator(mode="after")
    def require_unique_criteria(self) -> "CompletionEvidenceOrganizerOutput":
        criteria = [assessment.criterion.casefold().strip() for assessment in self.assessments]
        if len(criteria) != len(set(criteria)):
            raise ValueError("Each acceptance criterion must appear only once.")
        return self


class TaskRatingInput(PersistenceInput):
    rating: int = Field(ge=1, le=5)
    feedback: str = Field(default="", max_length=1000)

    @field_validator("feedback")
    @classmethod
    def strip_feedback(cls, value: str) -> str:
        return value.strip()


class TaskMediationInput(PersistenceInput):
    category: Literal["completion", "quality"]
    statement: str = Field(min_length=10, max_length=3000)
    desired_outcome: str = Field(default="", max_length=1000)

    @field_validator("statement", "desired_outcome")
    @classmethod
    def strip_mediation_text(cls, value: str) -> str:
        return value.strip()

    @model_validator(mode="after")
    def require_statement_content(self) -> "TaskMediationInput":
        if len(self.statement) < 10:
            raise ValueError("Provide at least 10 characters describing your perspective.")
        return self


class TaskMediationResponseInput(PersistenceInput):
    statement: str = Field(min_length=10, max_length=3000)
    desired_outcome: str = Field(default="", max_length=1000)

    @field_validator("statement", "desired_outcome")
    @classmethod
    def strip_response_text(cls, value: str) -> str:
        return value.strip()

    @model_validator(mode="after")
    def require_statement_content(self) -> "TaskMediationResponseInput":
        if len(self.statement) < 10:
            raise ValueError("Provide at least 10 characters describing your perspective.")
        return self


class TaskMediationMessageInput(PersistenceInput):
    message: str = Field(min_length=1, max_length=2000)

    @field_validator("message")
    @classmethod
    def strip_message(cls, value: str) -> str:
        return value.strip()

    @model_validator(mode="after")
    def require_message_content(self) -> "TaskMediationMessageInput":
        if not self.message:
            raise ValueError("Enter a clarification message.")
        return self


class TaskMediationProposalInput(PersistenceInput):
    proposal: str = Field(min_length=10, max_length=2000)

    @field_validator("proposal")
    @classmethod
    def strip_proposal(cls, value: str) -> str:
        return value.strip()

    @model_validator(mode="after")
    def require_proposal_content(self) -> "TaskMediationProposalInput":
        if len(self.proposal) < 10:
            raise ValueError("Describe a resolution in at least 10 characters.")
        return self


class TaskMediationReopenInput(PersistenceInput):
    reason: str = Field(min_length=10, max_length=1000)

    @field_validator("reason")
    @classmethod
    def strip_reason(cls, value: str) -> str:
        return value.strip()

    @model_validator(mode="after")
    def require_reason_content(self) -> "TaskMediationReopenInput":
        if len(self.reason) < 10:
            raise ValueError("Explain why the case is being reopened.")
        return self


class TaskSafetyReportInput(PersistenceInput):
    description: str = Field(min_length=10, max_length=2000)
    immediate_risk: bool

    @field_validator("description")
    @classmethod
    def strip_description(cls, value: str) -> str:
        return value.strip()

    @model_validator(mode="after")
    def require_description_content(self) -> "TaskSafetyReportInput":
        if len(self.description) < 10:
            raise ValueError("Provide at least 10 characters describing the safety concern.")
        return self


def validation_errors(error: ValidationError) -> list[dict[str, str]]:
    return [
        {
            "field": ".".join(str(part) for part in item["loc"]),
            "message": item["msg"],
        }
        for item in error.errors()
    ]


def tool_error(
    error_code: str,
    message: str,
    field_errors: list[dict[str, str]] | None = None,
) -> dict[str, Any]:
    result = PersistenceToolError(
        error_code=error_code,
        message=message,
        field_errors=field_errors,
    )
    return result.model_dump(mode="json", exclude_none=True)
