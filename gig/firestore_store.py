import os
import uuid
from datetime import datetime, timedelta, timezone
from typing import Any

import firebase_admin
from dotenv import load_dotenv
from firebase_admin import firestore

from persistence_models import (
    ClientTaskInput,
    ClientTaskSaved,
    WorkerProfileInput,
    WorkerProfileSaved,
)

RATING_WINDOW = timedelta(days=14)

load_dotenv()

FIREBASE_PROJECT_ID = os.getenv("FIREBASE_PROJECT_ID") or os.getenv(
    "GOOGLE_CLOUD_PROJECT"
)
FIREBASE_APP: firebase_admin.App | None = None
FIRESTORE_CLIENT: firestore.Client | None = None


def get_firebase_app() -> firebase_admin.App:
    global FIREBASE_APP
    if FIREBASE_APP is not None:
        return FIREBASE_APP
    if not FIREBASE_PROJECT_ID:
        raise RuntimeError("Set FIREBASE_PROJECT_ID or GOOGLE_CLOUD_PROJECT.")
    try:
        FIREBASE_APP = firebase_admin.get_app()
    except ValueError:
        FIREBASE_APP = firebase_admin.initialize_app(
            options={"projectId": FIREBASE_PROJECT_ID}
        )
    return FIREBASE_APP


def get_firestore_client() -> firestore.Client:
    global FIRESTORE_CLIENT
    if FIRESTORE_CLIENT is None:
        FIRESTORE_CLIENT = firestore.client(app=get_firebase_app())
    return FIRESTORE_CLIENT


def save_user_identity(
    user_id: str, *, email: str | None, display_name: str | None, photo_url: str | None
) -> None:
    profile: dict[str, Any] = {"updatedAt": firestore.SERVER_TIMESTAMP}
    if email:
        profile["email"] = email
    if display_name:
        profile["displayName"] = display_name
    if photo_url:
        profile["photoURL"] = photo_url
    get_firestore_client().collection("users").document(user_id).set(
        profile, merge=True
    )


def _serialize_document(document: dict[str, Any]) -> dict[str, Any]:
    def serialize(value: Any) -> Any:
        if isinstance(value, datetime):
            return value.isoformat()
        if isinstance(value, dict):
            return {key: serialize(item) for key, item in value.items()}
        if isinstance(value, list):
            return [serialize(item) for item in value]
        return value

    return serialize(document)


def _task_for_user(
    task_id: str, document: dict[str, Any], role: str
) -> dict[str, Any]:
    task = _serialize_document(document)
    reviews = document.get("reviews") or {}
    own_review = reviews.get(role)
    other_role = "worker" if role == "client" else "client"
    other_review = reviews.get(other_role)
    completed_at = document.get("completedAt")
    if isinstance(completed_at, datetime):
        if completed_at.tzinfo is None:
            completed_at = completed_at.replace(tzinfo=timezone.utc)
        review_deadline = completed_at + RATING_WINDOW
        is_published = bool(own_review and other_review) or (
            datetime.now(timezone.utc) >= review_deadline
        )
        task["rating_window_ends_at"] = review_deadline.isoformat()
    else:
        review_deadline = None
        is_published = False
    task["id"] = task_id
    task.pop("reviews", None)
    task["my_rating"] = _serialize_document(own_review) if own_review else None
    task["other_rating"] = (
        _serialize_document(other_review)
        if other_review and is_published
        else None
    )
    task["can_rate"] = bool(
        document.get("status") == "completed"
        and not own_review
        and review_deadline
        and datetime.now(timezone.utc) < review_deadline
    )
    task["ratings_published"] = is_published
    mediation = document.get("mediation") or {}
    resolution = mediation.get("resolution") or {}
    reopen_until = resolution.get("reopenUntil")
    if isinstance(reopen_until, datetime):
        if reopen_until.tzinfo is None:
            reopen_until = reopen_until.replace(tzinfo=timezone.utc)
        task["can_reopen_mediation"] = bool(
            mediation.get("status") == "resolved"
            and datetime.now(timezone.utc) < reopen_until
        )
    else:
        task["can_reopen_mediation"] = False
    return task


def save_worker_profile(
    *,
    user_id: str,
    full_name: str,
    email: str,
    whatsapp: str,
    location: str,
    skills: list[str],
    experience: str,
    interested_tasks: list[str],
    equipment: list[str],
    task_notifications_opt_in: bool,
) -> dict[str, Any]:
    profile_input = WorkerProfileInput.model_validate(
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

    client = get_firestore_client()
    profile_ref = client.collection("workerProfiles").document(user_id)
    is_new_profile = not profile_ref.get().exists
    user_ref = client.collection("users").document(user_id)
    user_data = user_ref.get().to_dict() or {}
    email = user_data.get("email") or profile_input.email
    profile: dict[str, Any] = {
        "userId": user_id,
        "fullName": profile_input.full_name,
        "email": email,
        "whatsapp": profile_input.whatsapp,
        "location": profile_input.location,
        "skills": profile_input.skills,
        "experience": profile_input.experience,
        "interestedTasks": profile_input.interested_tasks,
        "equipment": profile_input.equipment,
        "taskNotificationsOptIn": profile_input.task_notifications_opt_in,
        "vettingStatus": "pending",
        "updatedAt": firestore.SERVER_TIMESTAMP,
    }
    if profile_input.task_notifications_opt_in:
        profile["taskNotificationsOptInAt"] = firestore.SERVER_TIMESTAMP
    if is_new_profile:
        profile["createdAt"] = firestore.SERVER_TIMESTAMP
    batch = client.batch()
    batch.set(profile_ref, profile, merge=True)
    batch.set(
        user_ref,
        {
            "email": email,
            "displayName": profile_input.full_name,
            "roles": firestore.ArrayUnion(["worker"]),
            "updatedAt": firestore.SERVER_TIMESTAMP,
        },
        merge=True,
    )
    batch.commit()
    return WorkerProfileSaved(
        record_id=user_id,
        message="Worker profile saved. Vetting status is pending.",
    ).model_dump(mode="json")


def get_user_records(user_id: str) -> dict[str, Any]:
    client = get_firestore_client()
    user_snapshot = client.collection("users").document(user_id).get()
    worker_snapshot = client.collection("workerProfiles").document(user_id).get()
    tasks = (
        client.collection("tasks")
        .where("clientUserId", "==", user_id)
        .order_by("createdAt", direction=firestore.Query.DESCENDING)
        .limit(50)
        .stream()
    )
    worker_tasks = (
        client.collection("tasks")
        .where("assignedWorkerId", "==", user_id)
        .order_by("createdAt", direction=firestore.Query.DESCENDING)
        .limit(50)
        .stream()
    )
    safety_reports = (
        client.collection("safetyReports")
        .where("reporterUserId", "==", user_id)
        .limit(50)
        .stream()
    )
    client_task_records = [
        _task_for_user(task.id, task.to_dict() or {}, "client")
        for task in tasks
    ]
    worker_task_records = [
        _task_for_user(task.id, task.to_dict() or {}, "worker")
        for task in worker_tasks
    ]
    def summarize_received_ratings(
        task_records: list[dict[str, Any]],
    ) -> dict[str, Any]:
        received_ratings = [
            task["other_rating"]
            for task in task_records
            if task.get("other_rating")
        ]
        rating_total = sum(review["rating"] for review in received_ratings)
        return {
            "average": round(rating_total / len(received_ratings), 1)
            if received_ratings
            else None,
            "count": len(received_ratings),
        }

    return {
        "user": _serialize_document(user_snapshot.to_dict() or {}),
        "worker_profile": _serialize_document(worker_snapshot.to_dict() or {})
        if worker_snapshot.exists
        else None,
        "tasks": client_task_records,
        "worker_tasks": worker_task_records,
        "safety_reports": [
            {"id": report.id, **_serialize_document(report.to_dict() or {})}
            for report in safety_reports
        ],
        "worker_rating_summary": summarize_received_ratings(worker_task_records),
        "client_rating_summary": summarize_received_ratings(client_task_records),
    }


def create_client_task(
    *,
    user_id: str,
    request_id: str | None = None,
    client_name: str,
    client_email: str,
    title: str,
    description: str,
    scope: str,
    deliverables: list[str],
    acceptance_criteria: list[str],
    timing_access: str,
    safety_requirements: str,
    location: str,
    required_skills: list[str],
    required_equipment: list[str],
    deadline: str,
    budget: str,
) -> dict[str, Any]:
    task_input = ClientTaskInput.model_validate(
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

    client = get_firestore_client()
    user_ref = client.collection("users").document(user_id)
    user_data = user_ref.get().to_dict() or {}
    client_email = user_data.get("email") or task_input.client_email
    task_id = (
        str(uuid.uuid5(uuid.NAMESPACE_URL, f"kivulu-task:{user_id}:{uuid.UUID(request_id)}"))
        if request_id
        else str(uuid.uuid4())
    )
    task_ref = client.collection("tasks").document(task_id)
    if request_id:
        existing_task = task_ref.get()
        if existing_task.exists:
            existing_data = existing_task.to_dict() or {}
            if existing_data.get("clientUserId") != user_id:
                raise ValueError("This confirmation key does not belong to your account.")
            return ClientTaskSaved(
                record_id=task_id,
                message="This task was already saved for this confirmation.",
            ).model_dump(mode="json")
    batch = client.batch()
    batch.set(
        task_ref,
        {
            "clientUserId": user_id,
            "clientName": task_input.client_name,
            "clientEmail": client_email,
            "title": task_input.title,
            "description": task_input.description,
            "scope": task_input.scope,
            "deliverables": task_input.deliverables,
            "acceptanceCriteria": task_input.acceptance_criteria,
            "timingAccess": task_input.timing_access,
            "safetyRequirements": task_input.safety_requirements,
            "location": task_input.location,
            "requiredSkills": task_input.required_skills,
            "requiredEquipment": task_input.required_equipment,
            "deadline": task_input.deadline,
            "budget": task_input.budget or None,
            "status": "posted",
            "createdAt": firestore.SERVER_TIMESTAMP,
            "updatedAt": firestore.SERVER_TIMESTAMP,
        },
    )
    batch.set(
        user_ref,
        {
            "email": client_email,
            "displayName": task_input.client_name,
            "roles": firestore.ArrayUnion(["client"]),
            "updatedAt": firestore.SERVER_TIMESTAMP,
        },
        merge=True,
    )
    batch.commit()
    return ClientTaskSaved(
        record_id=task_id,
        message="Task posted successfully.",
    ).model_dump(mode="json")
