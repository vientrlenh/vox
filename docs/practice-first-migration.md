# Practice-First Migration Target

Status: Draft · Last updated: 2026-10-10

This document describes the target state for moving Vox from school-only use to practice-first use. Each section states the end state and, where relevant, what blocks it today.

## 1. Goal

Anyone can create an account and practice without belonging to a school. Schools remain supported for exams and class management, but a school is something a user joins, not where an account comes from.

## 2. Principles

- Accounts are created at sign-up. A school membership links an existing user to a school.
- Data is split by owner, not by role: the person owns their account and practice data; the school owns its record of the person.
- Leaving a school ends the membership. It never deletes or changes the person's account.
- Practice code does not depend on school, subscription, or balance code.

## 3. Open decisions

| Decision | Recommended default |
|---|---|
| Do school exams stay? | Yes, as an optional membership feature |
| Who pays for practice? | Undecided; hidden behind `PracticeEntitlementPort` (5.4) |
| Is there production data to keep? | If yes, every schema change is a new Flyway migration (V14+) |
| Can a school have several admins? | Yes |
| Can a user belong to several schools? | Keep history; decide whether more than one active membership is allowed |
| Can a school see practice from before the student joined? | No, unless the student agrees |

## 4. Target data model

### `users`: the person's account

| Column | Today | Target |
|---|---|---|
| `email` | Required, unique | Unchanged |
| `password_hash` | Required | Nullable, for Google-only accounts |
| `full_name` | Required | Unchanged (display name) |
| `date_of_birth` | Required | Required only if needed for age checks |
| `phone` | Optional, globally unique | Optional; unique only if verified and used for login |
| `address` | Optional | Removed, or never collected at sign-up |
| `gender`, `avatar_url`, `status` | Exist | Unchanged |

The domain `User` still has a `UserRole role` field although commit `033b8b33` removed it from the schema. Remove it together with the roles change below.

### `user_roles`: platform roles only

Holds platform-wide roles: `SYSTEM_ADMIN`, plus a default role for regular users if needed. School roles move to `school_users.role`.

### `school_users`: the school's record of a person

| Column | Today | Target |
|---|---|---|
| `school_id`, `user_id` | Exist | Unchanged |
| `role` | Missing (role is global in `user_roles`) | `STUDENT`, `TEACHER`, `SCHOOL_ADMIN` |
| `member_code` | Missing | Student or staff code, unique per school |
| `official_name` | Missing | Name as the school records it, used on exam results |
| `status` | Missing | `INVITED`, `ACTIVE`, `ENDED` |
| `invited_by` | Missing | User who invited or imported the member |
| `start_date`, `end_date` | Exist | Unchanged |

Today `UNIQUE (user_id)` and `UNIQUE (school_id, user_id)` allow only one membership row per user, ever: a user cannot rejoin a school or move to another one. Target: uniqueness applies only to active memberships (partial unique index), and ended rows stay as history.

### Unchanged tables

- `school_class_users`: class assignments, already owned by the school.
- `learner_profiles`: practice goals and interests, owned by the person and kept across schools.
- Practice tables (`practice_papers`, `practice_sessions`, ...) already reference only the user. There, `student_id` means the learner, not the school `STUDENT` role.

## 5. Target behavior

### 5.1 Authentication

- Login, refresh, OAuth2 login and user-details loading work for users without a school. Today `LoginUseCase`, `RefreshUseCase`, `OAuth2LoginUseCase` and `CustomUserDetailsService` reject every non-admin who is not in a school.
- The JWT `schoolId` claim is optional. Endpoints that need a school check membership themselves.
- Fix along the way: `OAuth2LoginUseCase` passes `schoolId` (null) instead of the user ID to `findSchoolIdByUserId`.

### 5.2 Sign-up

- Individual sign-up with email, OTP verification and password, reusing `register-verification-otp.html`.
- Google login creates an account for an unknown email instead of rejecting it.

### 5.3 School registration and membership

- A signed-in user claims a school (directory with domain OTP, or documents). Today `RegisterFromSchoolDirectoryUseCase` rejects existing users and `ProvisionSchoolService` always creates a new admin account.
- Members join through invitations (email or join code) that they accept. Accounts are created only for people who have none.
- Removing a member ends the membership. Today `DeleteSchoolUserUseCase` soft-deletes the whole account.
- A school admin can add other admins. Today only `ProvisionSchoolService` assigns `SCHOOL_ADMIN`.

### 5.4 Practice billing

- Practice use cases call `PracticeEntitlementPort` (`requireCanPractice`, `charge`) instead of `SchoolSubscriptionRepository` and `SchoolSubscriptionActiveGuardService`.
- Today `BuildPracticePaperUseCase` requires an active school subscription and `SubmitPracticeTurnUseCase` charges every turn to the school quota.
- The first implementation can be simple, such as free with a daily limit. School-paid practice becomes a second implementation.

### 5.5 Bulk import

- Preview validates every row and shows new members, existing accounts to invite, and errors before anything is committed.
- One roster import (student code, name, birth date, class) replaces the separate user and class-user imports.
- Required fields: full name, and student code or email. `member_code` is the matching key within a school.
- Existing accounts receive an invitation; their profile is never overwritten. Today `SchoolUserImportCommitHandler.updateUser` overwrites it.
- Invitations are sent in a separate step, so a wrong import can be undone before anyone is emailed.
- Downloadable templates and a failed-rows file. Excel date cells are read as dates, not as display text.
- Year rollover: preview lists members missing from the file, with an option to end their memberships.

### 5.6 API structure

- One controller per resource, following the use case packages. Single-audience controllers use class-level `@PreAuthorize`.
- URLs stay resource-based, without role prefixes. School-scoped controllers use `/api/v1/schools/{schoolId}/<resource>`. `/admin/**` belongs to the Spring Boot Admin console.
- Grade levels and school directories may move to `/api/v1/grade-levels` and `/api/v1/school-directories` if the frontend changes anyway.

## 6. Guardrails (ArchUnit)

- Practice and learner-profile packages do not depend on school, subscription or balance packages.
- Every `@RestController` method has `@PreAuthorize` at method or class level, except endpoints listed as public in `SecurityConfig`.

## 7. Order of work

1. Get the build compiling with no behavior change: finish the mapper removal, and delete code that will be rewritten instead of fixing it. Split the school controllers while fixing their imports, keeping URLs unchanged (5.6).
2. Authentication (5.1).
3. `users` column changes (section 4) and sign-up (5.2).
4. Practice billing port (5.4).
5. `school_users` and `user_roles` changes (section 4), then membership and import (5.3, 5.5).
