import re
import uuid
from datetime import date, datetime, time, timedelta, timezone
from typing import Any

from firebase_admin import firestore
from firestore_store import get_firestore_client

INVITATION_TTL = timedelta(hours=24)
MAX_MATCH_CANDIDATES = 5
RATING_WINDOW = timedelta(days=14)
MEDIATION_REOPEN_WINDOW = timedelta(days=30)
MAX_MEDIATION_MESSAGES = 30


def _normalize(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", " ", value.casefold()).strip()


def _location_areas(value: str) -> set[str]:
    return {
        normalized
        for area in re.split(r"[,;]", value)
        if (normalized := _normalize(area))
    }


def rank_matching_workers(
    task: dict[str, Any],
    profiles: list[tuple[str, dict[str, Any]]],
) -> list[dict[str, Any]]:
    required_skills = {
        _normalize(skill) for skill in task.get("requiredSkills", []) if skill.strip()
    }
    required_equipment = {
        _normalize(item) for item in task.get("requiredEquipment", []) if item.strip()
    }
    task_areas = _location_areas(str(task.get("location", "")))
    task_labels = {
        _normalize(str(task.get("title", ""))),
        *required_skills,
    }
    if not required_skills or not task_areas:
        return []

    candidates: list[dict[str, Any]] = []
    for worker_id, profile in profiles:
        if profile.get("taskNotificationsOptIn") is not True:
            continue
        skills = {
            _normalize(skill) for skill in profile.get("skills", []) if skill.strip()
        }
        equipment = {
            _normalize(item)
            for item in profile.get("equipment", [])
            if item.strip()
        }
        worker_areas = _location_areas(str(profile.get("location", "")))
        matched_skills = required_skills & skills
        matched_equipment = required_equipment & equipment
        shared_areas = task_areas & worker_areas
        if (
            matched_skills != required_skills
            or matched_equipment != required_equipment
            or not shared_areas
        ):
            continue

        interests = {
            _normalize(item)
            for item in profile.get("interestedTasks", [])
            if item.strip()
        }
        interest_match = bool(interests & task_labels)
        reasons = [
            f"Has all required skills: {', '.join(sorted(required_skills))}",
            f"Serves the task area: {', '.join(sorted(shared_areas))}",
        ]
        if required_equipment:
            reasons.append(
                f"Has required equipment: {', '.join(sorted(required_equipment))}"
            )
        if interest_match:
            reasons.append("Task matches a stated worker interest")

        candidates.append(
            {
                "worker_id": worker_id,
                "worker_name": str(profile.get("fullName", "Worker")),
                "skills": sorted(skill for skill in profile.get("skills", []) if skill),
                "location": str(profile.get("location", "")),
                "vetting_status": str(profile.get("vettingStatus", "pending")),
                "interest_match": interest_match,
                "match_reasons": reasons,
            }
        )

    candidates.sort(
        key=lambda candidate: (
            not candidate["interest_match"],
            _normalize(candidate["worker_name"]),
            candidate["worker_id"],
        )
    )
    return candidates


def _task_reference(task_id: str) -> Any:
    return get_firestore_client().collection("tasks").document(task_id)


def _invitation_reference(invitation_id: str) -> Any:
    return get_firestore_client().collection("taskInvitations").document(invitation_id)


def _invitation_id(task_id: str, worker_id: str) -> str:
    return uuid.uuid5(
        uuid.NAMESPACE_URL, f"kivulu-task-invitation:{task_id}:{worker_id}"
    ).hex


def invite_task_matches(task_id: str, client_user_id: str) -> dict[str, Any]:
    if not task_id.strip():
        raise ValueError("A task ID is required.")
    task_ref = _task_reference(task_id)
    task_snapshot = task_ref.get()
    task = task_snapshot.to_dict() or {}
    if not task_snapshot.exists or task.get("clientUserId") != client_user_id:
        raise ValueError("That task was not found for your account.")
    if task.get("status") not in {"posted", "inviting"}:
        raise ValueError("This task is not accepting worker invitations.")
    if not task.get("requiredSkills") or not task.get("location"):
        raise ValueError(
            "Add at least one required skill and a service location before matching."
        )
    deadline_value = str(task.get("deadline", ""))
    try:
        if re.fullmatch(r"\d{4}-\d{2}-\d{2}", deadline_value):
            task_date = date.fromisoformat(deadline_value)
            deadline = datetime.combine(
                task_date, time.max, tzinfo=timezone.utc
            )
        else:
            deadline = datetime.fromisoformat(deadline_value.replace("Z", "+00:00"))
    except ValueError as error:
        raise ValueError("The task deadline is invalid; update it before matching.") from error
    if deadline.tzinfo is None:
        deadline = datetime.combine(deadline.date(), datetime.min.time(), timezone.utc)
    if deadline <= datetime.now(timezone.utc):
        raise ValueError("The task deadline has passed; update it before matching.")

    client = get_firestore_client()
    profile_snapshots = (
        client.collection("workerProfiles")
        .where("taskNotificationsOptIn", "==", True)
        .stream()
    )
    profiles = [
        (snapshot.id, snapshot.to_dict() or {})
        for snapshot in profile_snapshots
        if snapshot.id != client_user_id
    ]
    candidates = rank_matching_workers(task, profiles)
    if not candidates:
        return {
            "schema_version": 1,
            "status": "success",
            "task_id": task_id,
            "invitations": [],
            "message": (
                "No opted-in worker matched every required skill, required "
                "equipment, and a declared service area."
            ),
        }

    invitations: list[dict[str, Any]] = []
    now = datetime.now(timezone.utc)
    expires_at = now + INVITATION_TTL
    active_invitations = 0
    for candidate in candidates:
        worker_id = candidate["worker_id"]
        invitation_id = _invitation_id(task_id, worker_id)
        invitation_ref = _invitation_reference(invitation_id)
        existing = invitation_ref.get()
        existing_data = existing.to_dict() or {}
        if existing_data.get("status") in {"invited", "accepted", "assigned"}:
            invitations.append(
                {
                    **candidate,
                    "invitation_id": invitation_id,
                    "invitation_status": existing_data["status"],
                }
            )
            active_invitations += 1
            continue
        if existing_data.get("status") in {"declined", "declined_by_client"}:
            continue
        if active_invitations >= MAX_MATCH_CANDIDATES:
            continue

        invitation_ref.set(
            {
                "taskId": task_id,
                "clientUserId": client_user_id,
                "workerUserId": worker_id,
                "workerName": candidate["worker_name"],
                "vettingStatus": candidate["vetting_status"],
                "matchReasons": candidate["match_reasons"],
                "status": "invited",
                "createdAt": firestore.SERVER_TIMESTAMP,
                "expiresAt": expires_at,
            },
            merge=True,
        )
        invitations.append(
            {
                **candidate,
                "invitation_id": invitation_id,
                "invitation_status": "invited",
            }
        )
        active_invitations += 1

    if any(item["invitation_status"] in {"invited", "accepted"} for item in invitations):
        task_ref.update({"status": "inviting", "updatedAt": firestore.SERVER_TIMESTAMP})
    return {
        "schema_version": 1,
        "status": "success",
        "task_id": task_id,
        "invitations": invitations,
        "message": (
            f"Created {sum(item['invitation_status'] == 'invited' for item in invitations)} "
            "web invitation(s). Workers see them in their signed-in Kivulu web inbox. "
            "Matches use opt-in and self-reported profile data; workers are not "
            "background-checked or verified."
        ),
    }


def list_worker_invitations(worker_user_id: str) -> list[dict[str, Any]]:
    client = get_firestore_client()
    snapshots = (
        client.collection("taskInvitations")
        .where("workerUserId", "==", worker_user_id)
        .limit(100)
        .stream()
    )
    invitations: list[dict[str, Any]] = []
    now = datetime.now(timezone.utc)
    for snapshot in snapshots:
        invitation = snapshot.to_dict() or {}
        if invitation.get("status") not in {"invited", "accepted"}:
            continue
        expires_at = invitation.get("expiresAt")
        if isinstance(expires_at, datetime) and expires_at <= now:
            snapshot.reference.update({"status": "expired"})
            continue
        task_snapshot = client.collection("tasks").document(
            str(invitation.get("taskId", ""))
        ).get()
        task = task_snapshot.to_dict() or {}
        if task.get("status") not in {"inviting", "awaiting_client_confirmation"}:
            continue
        invitations.append(
            {
                "invitation_id": snapshot.id,
                "task_id": invitation.get("taskId"),
                "status": invitation.get("status"),
                "match_reasons": invitation.get("matchReasons", []),
                "title": task.get("title", ""),
                "description": task.get("description", ""),
                "scope": task.get("scope", ""),
                "deliverables": task.get("deliverables", []),
                "acceptance_criteria": task.get("acceptanceCriteria", []),
                "timing_access": task.get("timingAccess", ""),
                "safety_requirements": task.get("safetyRequirements", ""),
                "location": task.get("location", ""),
                "required_skills": task.get("requiredSkills", []),
                "required_equipment": task.get("requiredEquipment", []),
                "deadline": task.get("deadline", ""),
                "budget": task.get("budget"),
                "expires_at": expires_at.isoformat()
                if isinstance(expires_at, datetime)
                else None,
            }
        )
    return invitations


def list_task_invitations(
    task_id: str, client_user_id: str
) -> list[dict[str, Any]]:
    task_snapshot = _task_reference(task_id).get()
    task = task_snapshot.to_dict() or {}
    if not task_snapshot.exists or task.get("clientUserId") != client_user_id:
        raise ValueError("That task was not found for your account.")
    snapshots = (
        get_firestore_client()
        .collection("taskInvitations")
        .where("taskId", "==", task_id)
        .limit(100)
        .stream()
    )
    return [
        {
            "invitation_id": snapshot.id,
            "worker_name": (snapshot.to_dict() or {}).get("workerName", "Worker"),
            "match_reasons": (snapshot.to_dict() or {}).get("matchReasons", []),
            "status": (snapshot.to_dict() or {}).get("status", "unknown"),
            "vetting_status": (snapshot.to_dict() or {}).get(
                "vettingStatus", "pending"
            ),
            "expires_at": (
                (snapshot.to_dict() or {})["expiresAt"].isoformat()
                if isinstance((snapshot.to_dict() or {}).get("expiresAt"), datetime)
                else None
            ),
        }
        for snapshot in snapshots
        if (snapshot.to_dict() or {}).get("clientUserId") == client_user_id
    ]


def list_client_task_invitations(client_user_id: str) -> list[dict[str, Any]]:
    snapshots = (
        get_firestore_client()
        .collection("taskInvitations")
        .where("clientUserId", "==", client_user_id)
        .limit(100)
        .stream()
    )
    return [
        {
            "task_id": invitation.get("taskId"),
            "invitation_id": snapshot.id,
            "worker_name": invitation.get("workerName", "Worker"),
            "match_reasons": invitation.get("matchReasons", []),
            "status": invitation.get("status", "unknown"),
            "vetting_status": invitation.get("vettingStatus", "pending"),
        }
        for snapshot in snapshots
        if (invitation := snapshot.to_dict() or {}).get("clientUserId")
        == client_user_id
    ]


def _set_worker_response(
    invitation_id: str, worker_user_id: str, accept: bool
) -> dict[str, Any]:
    client = get_firestore_client()
    invitation_ref = client.collection("taskInvitations").document(invitation_id)
    invitation = invitation_ref.get().to_dict() or {}
    if invitation.get("workerUserId") != worker_user_id:
        raise ValueError("That invitation was not found for your account.")
    task_ref = client.collection("tasks").document(str(invitation.get("taskId", "")))
    transaction = client.transaction()

    @firestore.transactional
    def update_invitation(transaction: Any) -> dict[str, Any]:
        current_invitation = invitation_ref.get(transaction=transaction).to_dict() or {}
        current_task = task_ref.get(transaction=transaction).to_dict() or {}
        if current_invitation.get("workerUserId") != worker_user_id:
            raise ValueError("That invitation was not found for your account.")
        if current_invitation.get("status") != "invited":
            raise ValueError("This invitation is no longer available.")
        expires_at = current_invitation.get("expiresAt")
        if isinstance(expires_at, datetime) and expires_at <= datetime.now(timezone.utc):
            transaction.update(invitation_ref, {"status": "expired"})
            return {"status": "expired", "task_id": current_invitation.get("taskId")}
        if accept and current_task.get("status") != "inviting":
            raise ValueError("This task is no longer accepting worker responses.")

        status = "accepted" if accept else "declined"
        transaction.update(
            invitation_ref,
            {
                "status": status,
                "respondedAt": firestore.SERVER_TIMESTAMP,
            },
        )
        if accept:
            transaction.update(
                task_ref,
                {
                    "status": "awaiting_client_confirmation",
                    "acceptedInvitationId": invitation_id,
                    "updatedAt": firestore.SERVER_TIMESTAMP,
                },
            )
        return {"status": status, "task_id": current_invitation.get("taskId")}

    result = update_invitation(transaction)
    if result["status"] == "accepted":
        _cancel_other_invitations(result["task_id"], invitation_id)
    return result


def _cancel_other_invitations(task_id: str, accepted_invitation_id: str) -> None:
    client = get_firestore_client()
    others = (
        client.collection("taskInvitations")
        .where("taskId", "==", task_id)
        .stream()
    )
    batch = client.batch()
    count = 0
    for snapshot in others:
        invitation = snapshot.to_dict() or {}
        if (
            snapshot.id != accepted_invitation_id
            and invitation.get("status") == "invited"
        ):
            batch.update(snapshot.reference, {"status": "cancelled"})
            count += 1
    if count:
        batch.commit()


def respond_to_worker_invitation(
    invitation_id: str, worker_user_id: str, accept: bool
) -> dict[str, Any]:
    if not invitation_id.strip():
        raise ValueError("An invitation ID is required.")
    if not isinstance(accept, bool):
        raise ValueError("Choose whether you accept this invitation.")
    return _set_worker_response(invitation_id, worker_user_id, accept)


def confirm_worker_assignment(
    task_id: str, invitation_id: str, client_user_id: str
) -> dict[str, Any]:
    client = get_firestore_client()
    task_ref = client.collection("tasks").document(task_id)
    invitation_ref = client.collection("taskInvitations").document(invitation_id)
    transaction = client.transaction()

    @firestore.transactional
    def assign(transaction: Any) -> str:
        task = task_ref.get(transaction=transaction).to_dict() or {}
        invitation = invitation_ref.get(transaction=transaction).to_dict() or {}
        if task.get("clientUserId") != client_user_id:
            raise ValueError("That task was not found for your account.")
        if (
            task.get("status") != "awaiting_client_confirmation"
            or task.get("acceptedInvitationId") != invitation_id
            or invitation.get("taskId") != task_id
            or invitation.get("clientUserId") != client_user_id
            or invitation.get("status") != "accepted"
        ):
            raise ValueError(
                "This worker has not accepted the invitation for this task."
            )
        transaction.update(
            task_ref,
            {
                "status": "assigned",
                "assignedWorkerId": invitation["workerUserId"],
                "assignedWorkerName": invitation.get("workerName", "Worker"),
                "assignedAt": firestore.SERVER_TIMESTAMP,
                "updatedAt": firestore.SERVER_TIMESTAMP,
            },
        )
        transaction.update(
            invitation_ref,
            {"status": "assigned", "clientConfirmedAt": firestore.SERVER_TIMESTAMP},
        )
        return invitation["workerUserId"]

    worker_user_id = assign(transaction)
    _cancel_other_invitations(task_id, invitation_id)
    return {
        "schema_version": 1,
        "status": "success",
        "task_id": task_id,
        "invitation_id": invitation_id,
        "assigned_worker_id": worker_user_id,
        "message": "Worker assignment confirmed by the client.",
    }


def decline_worker_candidate(
    task_id: str, invitation_id: str, client_user_id: str
) -> dict[str, Any]:
    client = get_firestore_client()
    task_ref = client.collection("tasks").document(task_id)
    invitation_ref = client.collection("taskInvitations").document(invitation_id)
    transaction = client.transaction()

    @firestore.transactional
    def decline(transaction: Any) -> str:
        task = task_ref.get(transaction=transaction).to_dict() or {}
        invitation = invitation_ref.get(transaction=transaction).to_dict() or {}
        if task.get("clientUserId") != client_user_id:
            raise ValueError("That task was not found for your account.")
        if (
            task.get("status") != "awaiting_client_confirmation"
            or task.get("acceptedInvitationId") != invitation_id
            or invitation.get("taskId") != task_id
            or invitation.get("clientUserId") != client_user_id
            or invitation.get("status") != "accepted"
        ):
            raise ValueError("This worker is not awaiting your confirmation.")
        transaction.update(
            invitation_ref,
            {
                "status": "declined_by_client",
                "clientRespondedAt": firestore.SERVER_TIMESTAMP,
            },
        )
        transaction.update(
            task_ref,
            {
                "status": "inviting",
                "acceptedInvitationId": firestore.DELETE_FIELD,
                "updatedAt": firestore.SERVER_TIMESTAMP,
            },
        )
        return invitation["workerUserId"]

    worker_user_id = decline(transaction)
    return {
        "schema_version": 1,
        "status": "success",
        "task_id": task_id,
        "invitation_id": invitation_id,
        "declined_worker_id": worker_user_id,
        "message": "Worker declined by client; task is available for further matching.",
    }


def submit_task_completion(
    task_id: str, worker_user_id: str, summary: str, evidence: str = ""
) -> dict[str, Any]:
    if not summary.strip():
        raise ValueError("Provide a completion summary.")
    client = get_firestore_client()
    task_ref = client.collection("tasks").document(task_id)
    transaction = client.transaction()

    @firestore.transactional
    def submit(transaction: Any) -> None:
        task = task_ref.get(transaction=transaction).to_dict() or {}
        if task.get("assignedWorkerId") != worker_user_id:
            raise ValueError("That assigned task was not found for your account.")
        if task.get("status") not in {"assigned", "completion_concern"}:
            raise ValueError("This task is not accepting a completion submission.")
        transaction.update(
            task_ref,
            {
                "status": "awaiting_client_completion_review",
                "completionSubmission": {
                    "summary": summary.strip(),
                    "evidence": evidence.strip(),
                    "submittedAt": datetime.now(timezone.utc),
                },
                "completionConcern": firestore.DELETE_FIELD,
                "completionConcernAt": firestore.DELETE_FIELD,
                "updatedAt": firestore.SERVER_TIMESTAMP,
            },
        )

    submit(transaction)
    return {
        "schema_version": 1,
        "status": "success",
        "task_id": task_id,
        "task_status": "awaiting_client_completion_review",
        "message": "Completion submitted for the client's review.",
    }


def review_task_completion(
    task_id: str, client_user_id: str, decision: str, note: str = ""
) -> dict[str, Any]:
    if decision not in {"accepted", "concern"}:
        raise ValueError("Choose accepted or concern.")
    if decision == "concern" and len(note.strip()) < 10:
        raise ValueError("Explain the concern in at least 10 characters.")
    client = get_firestore_client()
    task_ref = client.collection("tasks").document(task_id)
    transaction = client.transaction()

    @firestore.transactional
    def review(transaction: Any) -> str:
        task = task_ref.get(transaction=transaction).to_dict() or {}
        if task.get("clientUserId") != client_user_id:
            raise ValueError("That task was not found for your account.")
        if task.get("status") != "awaiting_client_completion_review":
            raise ValueError("This task is not awaiting your completion review.")
        if decision == "concern":
            transaction.update(
                task_ref,
                {
                    "status": "completion_concern",
                    "completionConcern": note.strip(),
                    "completionConcernAt": firestore.SERVER_TIMESTAMP,
                    "updatedAt": firestore.SERVER_TIMESTAMP,
                },
            )
            return "completion_concern"
        transaction.update(
            task_ref,
            {
                "status": "completed",
                "completedAt": firestore.SERVER_TIMESTAMP,
                "completionReviewNote": note.strip(),
                "updatedAt": firestore.SERVER_TIMESTAMP,
            },
        )
        return "completed"

    task_status = review(transaction)
    return {
        "schema_version": 1,
        "status": "success",
        "task_id": task_id,
        "task_status": task_status,
        "message": (
            "Completion accepted. Both participants may submit one rating within "
            "14 days."
            if task_status == "completed"
            else "Concern recorded. The worker may address it and resubmit completion."
        ),
    }


def submit_task_rating(
    task_id: str, user_id: str, rating: int, feedback: str = ""
) -> dict[str, Any]:
    if isinstance(rating, bool) or not isinstance(rating, int) or not 1 <= rating <= 5:
        raise ValueError("Rating must be a whole number from 1 to 5.")
    client = get_firestore_client()
    task_ref = client.collection("tasks").document(task_id)
    transaction = client.transaction()
    now = datetime.now(timezone.utc)

    @firestore.transactional
    def submit(transaction: Any) -> bool:
        task = task_ref.get(transaction=transaction).to_dict() or {}
        if task.get("status") != "completed":
            raise ValueError("Ratings are available only after completion is accepted.")
        mediation = task.get("mediation") or {}
        if mediation and mediation.get("status") != "resolved":
            raise ValueError("Ratings are paused while task mediation is open.")
        client_user_id = task.get("clientUserId")
        worker_user_id = task.get("assignedWorkerId")
        if user_id == client_user_id and user_id != worker_user_id:
            role = "client"
        elif user_id == worker_user_id and user_id != client_user_id:
            role = "worker"
        else:
            raise ValueError("That completed task was not found for your account.")
        completed_at = task.get("completedAt")
        if not isinstance(completed_at, datetime):
            raise ValueError("The completion timestamp is unavailable.")
        if completed_at.tzinfo is None:
            completed_at = completed_at.replace(tzinfo=timezone.utc)
        if now >= completed_at + RATING_WINDOW:
            raise ValueError("The 14-day rating window for this task has closed.")
        reviews = dict(task.get("reviews") or {})
        if role in reviews:
            raise ValueError("You have already rated this task.")
        reviews[role] = {
            "rating": rating,
            "feedback": feedback.strip(),
            "submittedAt": firestore.SERVER_TIMESTAMP,
        }
        transaction.update(
            task_ref,
            {"reviews": reviews, "updatedAt": firestore.SERVER_TIMESTAMP},
        )
        return len(reviews) == 2

    published = submit(transaction)
    return {
        "schema_version": 1,
        "status": "success",
        "task_id": task_id,
        "review_status": "published" if published else "waiting_for_other_rating",
        "message": (
            "Both ratings are submitted and now visible to the participants."
            if published
            else "Your rating is saved and hidden until the other participant rates "
            "or the 14-day window closes."
        ),
    }


def _task_participant_role(task: dict[str, Any], user_id: str) -> str:
    if task.get("clientUserId") == user_id:
        return "client"
    if task.get("assignedWorkerId") == user_id:
        return "worker"
    raise ValueError("That task was not found for your account.")


def start_task_mediation(
    task_id: str,
    user_id: str,
    category: str,
    statement: str,
    desired_outcome: str = "",
) -> dict[str, Any]:
    if category not in {"completion", "quality"}:
        raise ValueError("Choose completion or quality mediation.")
    if not statement.strip():
        raise ValueError("Describe your concern before opening mediation.")
    client = get_firestore_client()
    task_ref = client.collection("tasks").document(task_id)
    transaction = client.transaction()

    @firestore.transactional
    def start(transaction: Any) -> str:
        task = task_ref.get(transaction=transaction).to_dict() or {}
        role = _task_participant_role(task, user_id)
        if task.get("status") not in {
            "assigned",
            "awaiting_client_completion_review",
            "completion_concern",
            "completed",
        }:
            raise ValueError("Mediation is available after a worker is assigned.")
        if role == "worker" and not task.get("clientUserId"):
            raise ValueError("The client for this task could not be found.")
        existing = task.get("mediation") or {}
        if existing and existing.get("status") != "resolved":
            raise ValueError("This task already has an open mediation case.")
        if existing:
            raise ValueError(
                "This mediation was resolved. Reopen it during the 30-day window."
            )
        task_ref_data = {
            "category": category,
            "status": "awaiting_other_party",
            "openedBy": role,
            "statements": {
                role: {
                    "statement": statement.strip(),
                    "desiredOutcome": desired_outcome.strip(),
                    "submittedAt": datetime.now(timezone.utc),
                }
            },
            "messages": [],
            "createdAt": firestore.SERVER_TIMESTAMP,
            "updatedAt": firestore.SERVER_TIMESTAMP,
        }
        transaction.update(task_ref, {"mediation": task_ref_data, "updatedAt": firestore.SERVER_TIMESTAMP})
        return role

    role = start(transaction)
    return {
        "schema_version": 1,
        "status": "success",
        "task_id": task_id,
        "mediation_status": "awaiting_other_party",
        "opened_by": role,
        "message": "Your mediation statement was saved. The other participant can now respond.",
    }


def respond_to_task_mediation(
    task_id: str, user_id: str, statement: str, desired_outcome: str = ""
) -> dict[str, Any]:
    if not statement.strip():
        raise ValueError("Add your perspective before responding.")
    client = get_firestore_client()
    task_ref = client.collection("tasks").document(task_id)
    transaction = client.transaction()

    @firestore.transactional
    def respond(transaction: Any) -> str:
        task = task_ref.get(transaction=transaction).to_dict() or {}
        role = _task_participant_role(task, user_id)
        mediation = dict(task.get("mediation") or {})
        if mediation.get("status") != "awaiting_other_party":
            raise ValueError("This case is not awaiting the other participant.")
        if role == mediation.get("openedBy"):
            raise ValueError("The other participant must submit the first response.")
        statements = dict(mediation.get("statements") or {})
        if role in statements:
            raise ValueError("You have already submitted your opening statement.")
        statements[role] = {
            "statement": statement.strip(),
            "desiredOutcome": desired_outcome.strip(),
            "submittedAt": datetime.now(timezone.utc),
        }
        mediation.update(
            {
                "status": "in_mediation",
                "statements": statements,
                "updatedAt": firestore.SERVER_TIMESTAMP,
            }
        )
        transaction.update(
            task_ref,
            {"mediation": mediation, "updatedAt": firestore.SERVER_TIMESTAMP},
        )
        return role

    role = respond(transaction)
    return {
        "schema_version": 1,
        "status": "success",
        "task_id": task_id,
        "mediation_status": "in_mediation",
        "responded_by": role,
        "message": "Both perspectives are recorded. Kivulu can now help summarize the disagreement neutrally.",
    }


def add_task_mediation_message(
    task_id: str, user_id: str, message: str
) -> dict[str, Any]:
    if not message.strip():
        raise ValueError("Enter a clarification message.")
    client = get_firestore_client()
    task_ref = client.collection("tasks").document(task_id)
    transaction = client.transaction()

    @firestore.transactional
    def add_message(transaction: Any) -> int:
        task = task_ref.get(transaction=transaction).to_dict() or {}
        role = _task_participant_role(task, user_id)
        mediation = dict(task.get("mediation") or {})
        if mediation.get("status") != "in_mediation":
            raise ValueError("Clarifications are available while mediation is open.")
        messages = list(mediation.get("messages") or [])
        if len(messages) >= MAX_MEDIATION_MESSAGES:
            raise ValueError("The mediation reached its message limit.")
        messages.append(
            {
                "role": role,
                "message": message.strip(),
                "createdAt": datetime.now(timezone.utc),
            }
        )
        mediation.update(
            {"messages": messages, "updatedAt": firestore.SERVER_TIMESTAMP}
        )
        transaction.update(
            task_ref,
            {"mediation": mediation, "updatedAt": firestore.SERVER_TIMESTAMP},
        )
        return len(messages)

    message_count = add_message(transaction)
    return {
        "schema_version": 1,
        "status": "success",
        "task_id": task_id,
        "message_count": message_count,
        "message": "Your clarification was added to the shared mediation record.",
    }


def propose_task_mediation_resolution(
    task_id: str, user_id: str, proposal: str
) -> dict[str, Any]:
    if not proposal.strip():
        raise ValueError("Describe the proposed resolution.")
    client = get_firestore_client()
    task_ref = client.collection("tasks").document(task_id)
    transaction = client.transaction()

    @firestore.transactional
    def propose(transaction: Any) -> str:
        task = task_ref.get(transaction=transaction).to_dict() or {}
        role = _task_participant_role(task, user_id)
        mediation = dict(task.get("mediation") or {})
        if mediation.get("status") != "in_mediation":
            raise ValueError("A proposal can be made only while mediation is open.")
        current_proposal = mediation.get("proposal") or {}
        if current_proposal.get("status") == "pending":
            if current_proposal.get("proposedBy") == role:
                raise ValueError("Your proposal is already awaiting a response.")
            raise ValueError("Respond to the pending proposal before replacing it.")
        mediation["proposal"] = {
            "text": proposal.strip(),
            "proposedBy": role,
            "status": "pending",
            "createdAt": datetime.now(timezone.utc),
        }
        mediation["updatedAt"] = firestore.SERVER_TIMESTAMP
        transaction.update(
            task_ref,
            {"mediation": mediation, "updatedAt": firestore.SERVER_TIMESTAMP},
        )
        return role

    role = propose(transaction)
    return {
        "schema_version": 1,
        "status": "success",
        "task_id": task_id,
        "mediation_status": "in_mediation",
        "proposed_by": role,
        "message": "Your proposal was shared. It closes the case only if the other participant accepts it.",
    }


def respond_to_task_mediation_proposal(
    task_id: str, user_id: str, accept: bool
) -> dict[str, Any]:
    if not isinstance(accept, bool):
        raise ValueError("Choose whether you accept the proposal.")
    client = get_firestore_client()
    task_ref = client.collection("tasks").document(task_id)
    transaction = client.transaction()

    @firestore.transactional
    def respond(transaction: Any) -> str:
        task = task_ref.get(transaction=transaction).to_dict() or {}
        role = _task_participant_role(task, user_id)
        mediation = dict(task.get("mediation") or {})
        proposal = dict(mediation.get("proposal") or {})
        if mediation.get("status") != "in_mediation" or proposal.get("status") != "pending":
            raise ValueError("There is no pending mediation proposal to respond to.")
        if proposal.get("proposedBy") == role:
            raise ValueError("The person who proposed the resolution cannot accept it.")
        proposal["status"] = "accepted" if accept else "declined"
        proposal["respondedAt"] = datetime.now(timezone.utc)
        proposal["respondedBy"] = role
        mediation["proposal"] = proposal
        if accept:
            resolved_at = datetime.now(timezone.utc)
            resolution = {
                "text": proposal["text"],
                "acceptedBy": role,
                "acceptedAt": firestore.SERVER_TIMESTAMP,
                "reopenUntil": resolved_at + MEDIATION_REOPEN_WINDOW,
            }
            mediation["resolution"] = resolution
            mediation["status"] = "resolved"
            mediation["resolvedAt"] = firestore.SERVER_TIMESTAMP
        mediation["updatedAt"] = firestore.SERVER_TIMESTAMP
        transaction.update(
            task_ref,
            {"mediation": mediation, "updatedAt": firestore.SERVER_TIMESTAMP},
        )
        return mediation["status"]

    mediation_status = respond(transaction)
    return {
        "schema_version": 1,
        "status": "success",
        "task_id": task_id,
        "mediation_status": mediation_status,
        "message": (
            "Both participants accepted the proposed resolution. The case may be reopened within 30 days."
            if mediation_status == "resolved"
            else "The proposal was declined. Mediation remains open; either participant may add a clarification or propose another option."
        ),
    }


def reopen_task_mediation(
    task_id: str, user_id: str, reason: str
) -> dict[str, Any]:
    if not reason.strip():
        raise ValueError("Explain why the mediation is being reopened.")
    client = get_firestore_client()
    task_ref = client.collection("tasks").document(task_id)
    transaction = client.transaction()

    @firestore.transactional
    def reopen(transaction: Any) -> None:
        task = task_ref.get(transaction=transaction).to_dict() or {}
        role = _task_participant_role(task, user_id)
        mediation = dict(task.get("mediation") or {})
        resolution = dict(mediation.get("resolution") or {})
        if mediation.get("status") != "resolved":
            raise ValueError("Only a mutually resolved mediation can be reopened.")
        reopen_until = resolution.get("reopenUntil")
        if not isinstance(reopen_until, datetime):
            raise ValueError("The mediation reopen deadline is unavailable.")
        if reopen_until.tzinfo is None:
            reopen_until = reopen_until.replace(tzinfo=timezone.utc)
        if datetime.now(timezone.utc) >= reopen_until:
            raise ValueError("The 30-day mediation reopen window has closed.")
        history = list(mediation.get("resolutionHistory") or [])
        history.append(resolution)
        mediation.pop("resolution", None)
        mediation.pop("proposal", None)
        mediation.update(
            {
                "status": "in_mediation",
                "resolutionHistory": history,
                "reopenedBy": role,
                "reopenedAt": firestore.SERVER_TIMESTAMP,
                "updatedAt": firestore.SERVER_TIMESTAMP,
            }
        )
        messages = list(mediation.get("messages") or [])
        messages.append(
            {
                "role": role,
                "message": f"Case reopened: {reason.strip()}",
                "createdAt": datetime.now(timezone.utc),
            }
        )
        mediation["messages"] = messages[-MAX_MEDIATION_MESSAGES:]
        transaction.update(
            task_ref,
            {"mediation": mediation, "updatedAt": firestore.SERVER_TIMESTAMP},
        )

    reopen(transaction)
    return {
        "schema_version": 1,
        "status": "success",
        "task_id": task_id,
        "mediation_status": "in_mediation",
        "message": "Mediation reopened. Both participants can review the history and continue voluntarily.",
    }


def get_task_mediation_context(task_id: str, user_id: str) -> dict[str, Any]:
    task_snapshot = _task_reference(task_id).get()
    task = task_snapshot.to_dict() or {}
    if not task_snapshot.exists:
        raise ValueError("That task was not found for your account.")
    _task_participant_role(task, user_id)
    mediation = task.get("mediation") or {}
    if not mediation:
        raise ValueError("This task has no mediation case.")
    context = {
        "task_id": task_id,
        "task_title": task.get("title", ""),
        "task_status": task.get("status", ""),
        "agreed_task_brief": task.get("description", ""),
        "completion_submission": task.get("completionSubmission"),
        "completion_concern": task.get("completionConcern"),
        "completion_review_note": task.get("completionReviewNote"),
        "category": mediation.get("category", ""),
        "status": mediation.get("status", ""),
        "statements": mediation.get("statements", {}),
        "messages": mediation.get("messages", []),
        "proposal": mediation.get("proposal"),
        "resolution": mediation.get("resolution"),
        "resolution_history": mediation.get("resolutionHistory", []),
    }

    def serialize(value: Any) -> Any:
        if isinstance(value, datetime):
            return value.isoformat()
        if isinstance(value, dict):
            return {key: serialize(item) for key, item in value.items()}
        if isinstance(value, list):
            return [serialize(item) for item in value]
        return value

    return serialize(context)


def get_completion_review_context(task_id: str, client_user_id: str) -> dict[str, Any]:
    task_snapshot = _task_reference(task_id).get()
    task = task_snapshot.to_dict() or {}
    if not task_snapshot.exists or task.get("clientUserId") != client_user_id:
        raise ValueError("That task was not found for your account.")
    if task.get("status") != "awaiting_client_completion_review":
        raise ValueError("This task is not awaiting a completion review.")
    criteria = task.get("acceptanceCriteria") or []
    submission = task.get("completionSubmission")
    if (
        not isinstance(criteria, list)
        or not criteria
        or any(not isinstance(item, str) or not item.strip() for item in criteria)
        or not isinstance(submission, dict)
        or not isinstance(submission.get("summary"), str)
        or not submission["summary"].strip()
        or not isinstance(submission.get("evidence", ""), str)
    ):
        raise ValueError("This task has no complete acceptance criteria and submission to review.")
    return {
        "task_id": task_id,
        "task_title": task.get("title", ""),
        "agreed_task_brief": {
            "description": task.get("description", ""),
            "scope": task.get("scope", ""),
            "deliverables": task.get("deliverables", []),
            "acceptance_criteria": criteria,
            "timing_access": task.get("timingAccess", ""),
            "safety_requirements": task.get("safetyRequirements", ""),
        },
        "completion_submission": {
            "summary": submission.get("summary", ""),
            "evidence": submission.get("evidence", ""),
        },
    }


def record_task_safety_report(
    task_id: str, user_id: str, description: str, immediate_risk: bool
) -> dict[str, Any]:
    if not description.strip():
        raise ValueError("Describe the safety concern.")
    client = get_firestore_client()
    task_ref = client.collection("tasks").document(task_id)
    task = task_ref.get().to_dict() or {}
    _task_participant_role(task, user_id)
    report_id = uuid.uuid4().hex
    client.collection("safetyReports").document(report_id).set(
        {
            "taskId": task_id,
            "reporterUserId": user_id,
            "description": description.strip(),
            "immediateRisk": immediate_risk,
            "status": "reported",
            "createdAt": firestore.SERVER_TIMESTAMP,
        }
    )
    return {
        "schema_version": 1,
        "status": "success",
        "report_id": report_id,
        "message": (
            "Safety report recorded privately. Kivulu does not automatically alert a reviewer. "
            "If anyone is in immediate danger, stop unsafe work if safe to do so and contact local emergency services."
        ),
    }
