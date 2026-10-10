# Bulk Import Target: School Roster

Status: Draft · Last updated: 2026-10-10 · Part of [practice-first-migration.md](practice-first-migration.md), section 5.5

This document describes the target for importing students and teachers into a school. Other import types (rubrics, questions, policies, rooms, grades) keep their current behavior, but the file handling in section 5 applies to all of them.

## 1. Keep from today

- `ImportSession` and `ImportRow`, with per-row status and errors.
- Header matching with Vietnamese and English aliases in `FileProcessingService`, and the mapping confirmation step.
- `ImportJobDispatcher`: queued work with leases, a sweeper, and up to 3 attempts.
- One `ImportCommitHandler` per `ImportType`, run through `ImportCommitService`.
- The GraphQL queries `importSession`, `importSessions` and `importRows`.

## 2. Core decision: the import creates invitations, not accounts

Today `SchoolUserImportCommitHandler` creates an account for every new email, queues a password-setup email for each, and overwrites the profile of existing accounts. In the target, each roster row becomes a school invitation:

- A person with an email receives an invitation link. A person without one receives a personal claim code, handed out by the school.
- The person accepts by signing in or signing up. Only then is a `school_users` row created.
- If the email already belongs to an account, the invitation is addressed to it, but the person must still accept. Nothing is linked automatically.
- The import never writes to `users`.

This removes the need for phone, birth date and address, avoids mass emails to people who never asked for an account, and makes undo simple. The cost: students who have not accepted cannot be added to exams yet. The roster shows them as pending so the school can follow up.

## 3. Target flow

| Step | Who | Session status |
|---|---|---|
| 1. Download a template (optional) | Admin | - |
| 2. Upload the file; it is parsed and columns are matched | Admin | `PREVIEWED` |
| 3. Confirm the mapping, pick sheets, mark the file as full roster or partial | Admin | `VALIDATING` |
| 4. Check every row against the database without writing (dry run) | Worker | `VALIDATED` (new) |
| 5. Review the summary and exclude rows, or fix the file and start over | Admin | `VALIDATED` |
| 6. Commit the remaining rows | Admin, then worker | `QUEUED` → `IMPORTING` → `COMPLETED` |
| 7. Send invitations, or undo the import | Admin | `COMPLETED` or `REVERTED` (new) |

Step 6 checks each row again, because data can change between validation and commit. Rows that now conflict are marked `FAILED` with the reason. Sessions expire after 1 day today; that should be long enough to fix a file and come back.

## 4. Columns

| Field | Required | Notes |
|---|---|---|
| `officialName` | Yes | Name as the school records it |
| `memberCode` | At least one of these two | Student or staff code; the matching key within the school |
| `email` | At least one of these two | Used for the invitation link |
| `classCode` | No (students only) | Must match an existing class |
| `role` | No | Chosen in the UI for the whole file; a column overrides it per row |

Phone, address, birth date and membership start/end dates are no longer collected. Membership starts on acceptance. The role column accepts `STUDENT`, `TEACHER`, `Học sinh` and `Giáo viên`, with or without diacritics.

## 5. File handling (all import types)

- A template per import type: an xlsx with Vietnamese headers from the alias list, an example row, and dropdowns for fixed values (role, and the school's class codes for rosters).
- Sheet selection in step 3. Today only the first sheet is read. For rosters, the class can come from the sheet name, since many schools keep one sheet per class.
- Excel date cells are read as dates (`DateUtil.isCellDateFormatted`), not as display text from `DataFormatter`. Text dates accept `d/M/yyyy`, `dd/MM/yyyy` and `yyyy-MM-dd`.
- An explicit row limit with a clear message. Today only the 5 MB upload limit applies.
- A failed-rows download: the original columns plus an error column, for invalid and failed rows. Uploading the fixed file starts a new session.

## 6. Validation

Step 4 gives every row a planned action:

| Planned action | When |
|---|---|
| `INVITE` | No member or invitation matches; a new invitation will be created |
| `INVITE_EXISTING_ACCOUNT` | The email belongs to an account that is not a member of this school |
| `UPDATE_MEMBER` | `memberCode` matches an active member, and school-side fields change |
| `UPDATE_INVITATION` | `memberCode` or email matches a pending invitation |
| `UNCHANGED` | Matches a member or invitation with identical data |
| `ERROR` | The row cannot be imported; see its errors |

For a full-roster file, the summary also lists members missing from the file: active members of the same role who are not in it. The admin can end their memberships, which is how school-year rollover works. Partial files never list missing members.

Rules:
- Each error names the field and the problem, one entry per problem. No combined messages such as "email or phone already exists or invalid".
- A `memberCode` or email that appears twice in the file is an error on both rows.
- An unknown `classCode` is an error, grouped in the summary by code (for example, "12 rows use 10A5, which does not exist"). The admin creates the class and validates again; the import never creates classes.

## 7. Invitations and undo

- Committing creates invitations but sends nothing. The admin sends them in a separate step: emails for rows with an email, and a printable claim-code list per class for the rest.
- Until invitations are sent, the admin can undo the import. Invitations created by the session are deleted, and member fields and ended memberships are restored from each row's stored previous values.
- Invitations expire and can be resent or revoked.

## 8. Data model changes

New table `school_invitations`:

| Column | Notes |
|---|---|
| `school_id`, `role`, `member_code`, `official_name` | Same meaning as in `school_users` |
| `email` | Nullable |
| `school_class_id` | Nullable; applied on acceptance |
| `claim_code_hash` | The code is never stored in plain text |
| `status` | `PENDING`, `ACCEPTED`, `REVOKED`, `EXPIRED` |
| `import_session_id` | Nullable; set when created by an import |
| `invited_by`, `accepted_user_id`, `accepted_at`, `sent_at`, `expires_at` | |

- `import_sessions`: new statuses `VALIDATED` and `REVERTED`; new columns `is_full_roster` and `invitations_sent_at`.
- `import_rows`: new columns `planned_action`, `excluded` and `previous_data_json`.
- `member_code` is unique within a school across active members and pending invitations. A partial unique index covers each table; validation and commit check across both.

## 9. Code changes

| Class | Change |
|---|---|
| `FileProcessingService` | Excel date cells, sheet selection, aliases for `memberCode`, `officialName`, `classCode` |
| `ImportCommitHandler` | Add `validate(session, rows)`, which sets planned actions without writing; `commit` applies them |
| `ImportCommitService` | Run validation and undo as well as commit |
| `AcceptSchoolUserImportUseCase` | Split into "confirm mapping and validate" and "commit validated session" |
| `SchoolUserImportCommitHandler` | Rewrite: creates and updates invitations and member fields; no account creation, no `USER_CREATED` outbox, no writes to `users` |
| `SchoolClassUserImportCommitHandler` | Remove once the roster import covers class assignment |
| New use cases | Send invitations, undo import, download template, download failed rows, accept invitation |

## 10. Open decisions

| Decision | Recommended default |
|---|---|
| Invitations only, or create accounts for people without one? | Invitations only |
| Can a user be an active member of several schools? | Students: one; teachers: several |
| Who can see and print claim codes? | School admins, and teachers for their own classes |
| How long are invitations valid? | 30 days, resendable |
| Row limit per file | Based on the largest expected school, for example 5,000 |
| What does a student agree to when accepting? | Which practice data the school can see (migration doc, section 3) |
