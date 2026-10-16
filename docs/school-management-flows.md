# School Management Flows: Review and Proposals

- **Status:** discussion notes, not yet implemented
- **Date:** 2026-10-13, updated 2026-10-14 (section 7)
- **Branch:** `refactor/structure`. Line numbers refer to commit `f671f7e2` (section 7: `424c1b83`). The branch does not build yet, so treat references as pointers, not guarantees.
- **Perspective:** a school administrator onboarding a high school onto the system to run AI-assisted oral assessments.
- **Builds on:** the practice-first drafts `practice-first-migration.md` and `bulk-import-target.md` (both 2026-10-10, in git at `f671f7e2`). Section 8 lists where this document settles or changes something in them.

## Contents

1. [Context](#1-context)
2. [School management flows](#2-school-management-flows)
3. [Code fields](#3-code-fields)
4. [Import use cases](#4-import-use-cases)
5. [Student roster import design](#5-student-roster-import-design)
6. [Academic year design sketch](#6-academic-year-design-sketch)
7. [Evaluation configuration](#7-evaluation-configuration)
8. [Reconciliation with the earlier drafts](#8-reconciliation-with-the-earlier-drafts)
9. [Open questions](#9-open-questions)
10. [Suggested implementation order](#10-suggested-implementation-order)

---

## 1. Context

### 1.1 Practice-first direction

The system is moving from school-only use to practice-first use. These principles, from the migration draft, shape every proposal below:

- **Accounts are personal.** Anyone can create an account and practice without belonging to a school. A school is something a user joins, not where an account comes from.
- **Schools invite, people accept.** A school membership links an existing user to a school. Someone who already has an account accepts by signing in; someone without one signs up while accepting. This lets a student who already practices on Vox join their school with the same account instead of getting a second one.
- **Data is split by owner.** The person owns their account and practice data. The school owns its record of the person in `school_users`: role, member code, official name, status.
- **Leaving never touches the account.** Leaving a school ends the membership. It never deletes or changes the person's account.

### 1.2 Core problem on the school side

The system models a school as a snapshot: one admin, a flat list of people, and membership dates typed in per person. A real school runs on a yearly cycle: set up the year, enroll students, run assessments, promote students, repeat. Most proposals below come from closing that gap.

---

## 2. School management flows

### 2.1 Make the academic year the backbone (highest priority)

As an admin, I would set up "2026–2027" once, with its semesters. Grades, classes, assessments and reports would all belong to it.

**Today**
- `SchoolAcademicTimeline` exists as a model and `school_grades.academic_timeline_id` is required. There is no use case, entity or endpoint to create a timeline.
- Enrollment is typed per student. `CreateSchoolUserUseCase.java:115` and the user import both require `startDate`/`endDate` for every student.

**Proposal**
- A per-school academic year with terms and a lifecycle (`PLANNING → ACTIVE → CLOSED`). Grades, classes and enrollments hang off it, and closing it freezes that year. See [section 6](#6-academic-year-design-sketch).

### 2.2 Add a year-end rollover flow (biggest missing workflow)

Nothing handles promotion, graduation or transfer. Every summer the admin would have to recreate grades and classes, re-assign every student, and remove graduates one by one. Removing a member also soft-deletes their account (`DeleteSchoolUserUseCase.java:54`), which contradicts the practice-first principles.

**Proposal:** a "Roll over to next year" wizard using the preview-then-confirm pattern from imports:
1. Copy the structure into the new year (10A1 → 11A1, with an editable mapping).
2. Promote students in bulk and flag exceptions (repeaters, class changes, leavers).
3. Graduate grade 12 by ending their school memberships. The accounts are their own, so they keep practicing with their full history.
4. Show the full set of changes before committing.

### 2.3 Act on the school's record, never on the account

A school can only change its own record of a person.

| Action | Who | Effect |
|---|---|---|
| Suspend / restore school access | School admin | Blocks school features (classes, assessments, school-paid practice) during a cheating investigation, for example. The person can still sign in and practice on their own. Needs a `SUSPENDED` membership status. |
| End membership (graduated / transferred / left) | School admin | Membership `ENDED`, history kept, account untouched. |
| Lock or delete the account | The person, or a system admin | Never available to a school. |

**Today**
- `DeleteSchoolUserUseCase` soft-deletes the account.
- `UpdateSchoolUserUseCase` edits the person's account fields (full name, phone, address, date of birth, avatar). In the target it edits only the school's record: official name, member code, role, and the identity photo (2.5).

**Also**
- Block ending a teacher's membership while they still have open grading assignments or chair an assessment, until the work is reassigned (`ReassignGradingUseCase` exists).
- **Authorization:** the membership lookups used for it (`findByUserId` and `findSchoolIdByUserId` in `SpringDataSchoolUserRepository`) ignore `end_date`. They must count only active memberships.
- **Rejoining:** `UNIQUE (user_id)` on `school_users` allows one membership row per user, ever, so nobody can rejoin a school or move to another. Uniqueness should apply only to live memberships, with ended rows kept as history (6.5).

### 2.4 Let the registrant use their own account, and allow several admins

**Today**
- `RegisterFromSchoolDirectoryUseCase` and `RegisterBySelfDeclaredUseCase` reject a contact email or phone that already belongs to a user.
- `ProvisionSchoolService.java:69` always creates a new admin account.
- That registrant is the only admin. `CreateSchoolUserUseCase.java:38` only allows creating STUDENT or TEACHER, and admins cannot remove themselves. If that person leaves the school, nobody there can manage it.
- `SCHOOL_ADMIN` covers billing, quotas, roster, assessments and appeals. In a real school those belong to the principal, the English department head, an office clerk and an accountant.

**Proposal**
- A signed-in user claims a school (directory with domain OTP, or documents), and their existing account becomes the first admin.
- Multiple admins, with an "at least one active admin" invariant and a "transfer ownership" action.
- Permission scopes on the membership, for example `MANAGE_ROSTER`, `MANAGE_BILLING`, `MANAGE_ASSESSMENT`, `VIEW_REPORTS`.

### 2.5 Roster through invitations

**Today**
- The school creates accounts. Students need email, phone, date of birth and address (`AcceptSchoolUserImportUseCase.java:34`), and phone must be unique.
- Every new account is emailed a password-setup link.
- When an imported email already exists, the import overwrites that person's profile (`SchoolUserImportCommitHandler.java:155`), or rejects the row if the person belongs to another school (`:139`).

**Proposal**
- **Invitations, not accounts:** the import creates invitations. See section 5.
- **Minimal data:** the school collects only what it needs: official name, member code and/or email, class. No phone, address or date of birth.
- **Claim codes:** students without email get a personal claim code, printable per class.
- **Identity photo:** today the school sets the person's avatar, because proctors use it to check attendance (see the comment in `UpdateSchoolUserUseCase`). Once accounts are personal, the photo the school relies on belongs on the school's record (e.g. `school_users.identity_photo_url`). That way the person's own avatar and the school's verification photo never overwrite each other.
- **Sign-in:** Google Workspace for Education sign-in restricted to the school domain makes accepting easy for schools that use it.

### 2.6 Give class membership a role and rules

**Today**
- `SchoolClassUser` has no role, so teachers and students are added the same way.
- `validateTargetUser` (`CreateSchoolClassUserUseCase.java:156`) only checks that the person is in the same school.
- Nothing stops a student from being in two homeroom classes, or in a class for the wrong grade.
- Moving a student takes two separate calls.

**Proposal**
- A membership role (student or teacher, optionally main or assistant teacher).
- A grade-match check.
- A single atomic "move student" action.
- One homeroom class per student per year, plus any number of groups (remedial, gifted training, assessment review). See 7.5.
- An invitation can carry a class; the class membership is created when the invitation is accepted.

### 2.7 Show quotas in units admins think in

Practice billing moves behind `PracticeEntitlementPort` (migration draft, 5.4). School-paid practice is one implementation of that port. For that implementation:
- **Today:** `DistributeQuotaToUsersService.distribute` either splits evenly across every student or teacher, or takes a VND amount per person.
- **Proposal:**
  - Allocation rules by grade or class, displayed as estimated sessions or minutes, with VND kept internal.
  - Rules apply automatically to students who join mid-year.
  - Quota comes back when a membership ends.

### 2.8 Make the admin dashboard about learning, not only operations

`ViewSchoolAdminDashboardUseCase.java:117` shows assessment counts, appeals, spending, quota and grading backlog. A principal is paying for evidence of outcomes:
- **Targets:** the share of students meeting their target, and the distribution of estimated levels, per grade and program (7.8).
- **Comparisons:** class-by-class and term-over-term progress. Class score stats currently exist only on the teacher dashboard.
- **Who needs help:** students below target, and students who are not practicing.
- **Grading quality:** AI vs. human grading agreement at school level (`ViewAiQualityReportUseCase` exists in grading).
- **Reports:** consolidated per-term or per-class exports, not just per-assessment score sheets.

Practice metrics only cover what the school may see: practice after joining, or earlier practice if the student agrees (open question 4).

### 2.9 Handle consent, retention and an audit trail

The system stores voice recordings, identity photos and personal data of 15–18-year-olds. There is no consent record, retention setting, or audit log of admin actions on students. Result changes do have a status history.

**Proposal**
- **Consent at acceptance:** accepting a school invitation is the natural moment to record it. What practice data can the school see? And for students under 16, guardian consent. Vietnamese personal-data rules appear to require it for under-16s, and grade-10 students are often 15. *To be confirmed with legal counsel.*
- **Retention:** a per-school setting, e.g. delete raw assessment audio N months after results are published.
- **Audit:** a log for roster, membership and result actions.

---

## 3. Code fields

### 3.1 The rule

A code earns its place only if something reads it. Every code falls into one of four cases:

1. **Contract key:** another system or rule reads the exact string (the AI agent, scoring rules, seed data). Keep it, have the system define it, and lock it once published.
2. **Real-world ID:** the school already has this number from outside the app. Keep it, but import it rather than having users invent it.
3. **Reference for people:** users need to say "assessment 42", but nobody should have to make the value up. Generate it.
4. **Display only:** remove it. Imports then need another natural key (see 3.3).

### 3.2 Per model

| Model | Verdict | Why |
|---|---|---|
| `RubricCriterion` | **Stop storing and typing it.** Take it from the linked `FrameworkCriterion` (7.4). | AI contract key: sent as the criterion key (`SubmitAssessmentSessionUseCase.java:356`), special handling only for `grammar`/`vocabulary`/`coherence` (`:340`), AI scores matched back by code (`RecordAssessmentAttemptEvaluationUseCase.java:153`), referenced by scoring rules (`CriterionScoreThresholdParams.criterionCode`). Schools currently type it freely (`CreateSchoolRubricCriterionUseCase.java:112`), so a typo silently changes how a criterion is evaluated. Seeds already use identical codes on both sides (`FrameworkInitializer.java:74`, `SystemRubricTemplateInitializer.java:103`). |
| `FrameworkVersion` / `FrameworkCriterion` / `FrameworkResultBand` | **Keep.** System admin only, locked once published. | Codes like B1 and GRAMMAR are sent to the AI. |
| `InterestDimension` | **Keep.** System-defined. | Used as keys by the quiz scorer and topic suggestions. |
| `LearningLanguage`, `Area` | **Keep.** Seeded. | ISO language codes, official administrative-area codes. |
| `School`, `SchoolDirectory` | **Keep.** Never typed by school admins. | Official ministry school code. It comes from the directory, or the system admin sets it at approval. |
| `SchoolUser.memberCode` | **Keep.** Import it; it is the roster matching key. | The school's own student or staff ID. Unique per school across live members and pending invitations. The `Code` value object throws on anything outside `^[A-Z0-9_-]+$`, so IDs need normalizing before validation. |
| `Assessment` | **Generate a short code.** | Optional today, but the fallback is `EX-` + 32 hex characters (`CreateAssessmentUseCase.java:174`), so paper codes come out as `EX-<32 chars>-P1`. Use a short per-school sequence such as `EX-0042`. |
| `AssessmentPaper`, `AssessmentBlueprintVersion` | **Keep as derived.** | Already derived from the parent code (`-P{n}`, `-V{n}`). |
| `AssessmentBlueprint`, `Question`, `QuestionBank` | **Generate when left blank.** | Imports can still supply a code to update existing rows. |
| `ScoringRule` | **See 7.3.** | Rules are never executed, and school-level rules are removed. If platform rules come later, generate their codes; they are copied into `AssessmentItemRuleHit.ruleCode`. |
| `RubricVersion` | **Remove.** Use (rubric, version number). | A unique index on `(rubric_id, version)` already exists. |
| `Rubric`, `QuestionTopic` | **Remove.** Use the name within its owner. | Only used for uniqueness and import lookups. |
| `SchoolClass`, `SchoolGrade`, `SchoolRoom`, `GradeLevel` | **Already removed from the models.** Finish the cleanup. | See 3.3. |

The `…Code` fields on result records (`matchedBandCode`, `ruleCode`, `reasonCode`, `targetCriterionCode`, `reviewReasonCode`) are copies taken at evaluation time, not identifiers. They stay meaningful only if the contract codes they copy never change.

### 3.3 Leftovers after removing codes

Call sites that still use the removed codes:
- **Classes:** `CreateSchoolClassUseCase.java:157`, `SchoolClassImportCommitHandler.java:183`, `SchoolClassUserImportCommitHandler.java:177` (its import file has a `classCode` column).
- **Grades:** `CreateSchoolGradeUseCase.java:67`, `SchoolClassImportCommitHandler.java:197`.
- **Rooms:** `AddSchoolRoomUseCase.java:56`, `SchoolRoomImportCommitHandler.java:167`.
- **Grade levels:** `GradeLevelJpaEntity` still has a unique index on `code`. `SchoolGradeImportCommitHandler.java:184`, `GradeLevelBandScopeInitializer.java:135` and `SystemAssessmentPolicyInitializer.java:227` look grade levels up by code.
- **Policy import:** `AssessmentPolicyImportCommitHandler.java:322–330` looks up classes, school grades and grade levels by code.
- **Class table:** `SchoolClassJpaEntity` still declares a `code` column with a unique index. Its index `idx_school_class_class_school_language` references a column `language` that does not exist; the column is `learning_language_id`.

Replacement natural keys:

| Entity | Key |
|---|---|
| Class | Name, unique per (school, academic year), so "10A1" can exist again next year |
| School grade | (academic year, grade level). No name key needed. |
| Room | Name, unique per school |
| Grade level | `order` (10, 11, 12). Make it unique. |

Names used as keys need normalizing (trim, collapse spaces, upper-case). Otherwise "10A1" and "10a1 " count as two classes.

### 3.4 Bug: rubric version lookup ignores scope

`AssessmentPolicyImportCommitHandler.java:311` looks up rubric versions by code across every school (`findByCodeIn`). But codes are only unique within a rubric, and the `indexBy` helper keeps whichever match comes last. If two schools both have a version "V1", school A's import can resolve to school B's version. The ownership check at `:446` then rejects the row, so no data leaks, but a valid row fails with a misleading error. Switching to (rubric, version number) removes the problem.

---

## 4. Import use cases

### 4.1 What is already good

- Preview then accept.
- A background worker with leases and retries (`ImportJobDispatcher`).
- A separate transaction per row.
- A row view that can be filtered by status (`ViewImportRowsUseCase`).
- Vietnamese header aliases that ignore diacritics (`FileProcessingService.aliasesBySystemField`).
- Question import already offers a downloadable template and an export in the same format (`QuestionSpreadsheetService`).

### 4.2 Jobs an admin needs import for

| Job | Today | Should be |
|---|---|---|
| **1. Set up the school from existing data** | Grades → classes → users → class members, in order, as separate files keyed by codes being removed. First sheet only, header must be row 1. Students need email, phone and address. The import creates accounts. | One student roster import (section 5) that creates invitations and places students in classes for the chosen academic year. |
| **2. Keep the roster in sync** | The user import matches existing rows by email and overwrites profiles. The class-member import only adds, matching on (user, class), so moving a student leaves them in both classes. Nothing can end a membership. | Re-upload the same file and get a diff. Two modes: "partial" or "full roster for this role". In full-roster mode, members missing from the file are listed, and the admin can end their memberships. |
| **3. Know before committing, fix and retry** | Preview shows headers and 10 sample rows. Validation runs only during the background commit, so a half-bad file leaves half the data written. No error file to download, no undo. The summary counts only new rows (`ImportCommitService` sets `importedRows = created`). No completion notification. | Dry-run validation with a planned action per row. Exclude rows before committing. Downloadable failed-rows file. Undo until invitations are sent. Notification when done. |
| **4. Control when people are contacted** | Every created user is emailed a password-setup link the moment the import commits. | Committing creates invitations and sends nothing. A separate step sends emails, or prints claim codes per class. |
| **5. Edit in bulk by round-tripping a spreadsheet** | Rosters have no template or export. | Copy the question pattern: export the class roster in the import format, edit it, re-upload, get the diff from job 2. |

### 4.3 Smaller fixes

- **Values aren't mapped, only headers.** Files contain "Học sinh", "Nam/Nữ", "Lớp 10A1". The role is chosen in the UI for the whole file. A role column can override it per row and accepts `STUDENT`, `TEACHER`, `Học sinh` and `Giáo viên`, with or without diacritics.
- **Mapping suggestions have drifted from the handlers.** The USER mapping suggests `studentId`, but `SchoolUserImportCommitHandler.SUPPORTED_FIELDS` does not include it, so mapped data is silently dropped. The class mapping suggests `targetSchoolLevelCode`/`targetSchoolLevelVersion`, which the class handler ignores.

### 4.4 Keep, merge or drop

| Import | Recommendation |
|---|---|
| USER + SCHOOL_CLASS_USER (+ SCHOOL_CLASS) | **Merge into `STUDENT_ROSTER`** (invitations). |
| Teachers (part of USER today) | **Separate `STAFF` import**, also invitation-based, with an optional "classes taught" column. |
| SCHOOL_GRADE | **Drop.** Grades come from academic year × grade level. |
| SCHOOL_ROOM | **Keep.** Low priority. |
| ASSESSMENT_POLICY | **Replace with the targets screen** (7.6): prefilled from grade expectations and copied forward by the rollover. |
| RUBRIC_VERSION / RUBRIC_CRITERION / RUBRIC_RESULT_BAND | **One workbook, or rely on cloning** (7.4). One rubric takes three files today. `CloneSystemRubricToSchoolUseCase` plus editing is the more natural path. |
| QUESTION / QUESTION_BANK / QUESTION_TOPIC | **Keep.** Teacher-facing and already the most complete pipeline. |
| Assessment candidates | **Already right.** `ImportAssessmentCandidatesFromClass`/`FromGrade` take candidates from the roster, not from a file. |

---

## 5. Student roster import design

### 5.1 Core decision: the import creates invitations, not accounts

Each roster row becomes a school invitation:
- **With email:** the person receives an invitation link.
- **Without email:** the person receives a personal claim code, which the school hands out.
- **Accepting:** the person signs in or signs up. Only then is a `school_users` row created.
- **Existing accounts:** if the email already belongs to an account, the invitation is addressed to it, but the person must still accept. Nothing is linked automatically, and the person's profile is never overwritten.
- **No account writes:** the import never writes to `users`.

This is what allows people who already practice on Vox to join their school without a second account. It also removes the need for phone, birth date and address, avoids mass emails, and makes undo simple.

**The cost:** students who have not accepted yet cannot be put in assessments or classes. The roster shows them as pending so the school can follow up.

### 5.2 Sources

- **Most schools:** their own Excel files, with a different layout at each school. The importer has to detect the structure and remember each school's layout.
- **Some schools:** vnEdu exports, which share one fixed layout. vnEdu becomes a built-in preset of the same mechanism, not a separate integration. *The preset needs a real (anonymized) vnEdu export before it can be written.*

### 5.3 What school files look like vs. what the parser assumes

| Typical in school files | Current behavior (`FileProcessingService`) |
|---|---|
| Title rows above the header ("DANH SÁCH HỌC SINH LỚP 10A1") | The first non-empty row is taken as the header, so nothing gets mapped. |
| One sheet per class | Only `getSheetAt(0)` is read. Other sheets are silently ignored. |
| Merged or two-row headers, blank or repeated column names | Each row is a map keyed by header text, so duplicate or blank headers overwrite each other. |
| Class only in the title or sheet name | No way to assign a class per sheet. |
| Name split into "Họ đệm" + "Tên" (lists are sorted by given name) | A column maps to only one field. Columns cannot be combined. |
| Dates as text `15/01/2009`, or as real Excel dates | Cells become their displayed text, so the same date can arrive in different shapes. |
| Footer lines ("Tổng số: 45", "Hiệu trưởng") | Imported as data rows, which fail with noisy errors. |
| Neither a student code nor an email | Every row would fail the "member code or email" rule. See [open question 6](#9-open-questions). |
| Students with the same name | Matching never uses names, only member code or email. |

Error row numbers count only non-blank rows and skip the header offset (`PreviewSchoolUserImportFromFileUseCase.saveRows`). An error on "row 14" does not point to row 14 in Excel.

### 5.4 Columns

| Field | Required | Notes |
|---|---|---|
| `officialName` | Yes | Name as the school records it. Can be combined from two columns (Họ đệm + Tên). |
| `memberCode` | At least one of these two | Student or staff code; the matching key within the school. |
| `email` | At least one of these two | Used for the invitation link, and to find an existing account. |
| `class` | No (students only) | Must match a class of the chosen academic year. Taken from a column, the sheet name, the title row, or picked for the whole file. |
| `role` | No | Chosen in the UI for the whole file; a column overrides it per row. |

Phone, address, birth date and membership start/end dates are not collected. Membership starts on acceptance; enrollment comes from the academic year.

### 5.5 Flow

| Step | Who | Session status |
|---|---|---|
| 1. Download a template (optional). It is generated for the school, with its class names as a dropdown. | Admin | – |
| 2. Upload. Pick the academic year, the role and the mode (partial / full roster). | Admin | `PREVIEWED` |
| 3. Confirm the detected structure: header row per sheet, sheets to skip, footer lines ignored, where the class comes from. Confirm column and value mapping. Suggestions come from aliases plus the school's saved layout. | Admin | `VALIDATING` |
| 4. Dry run: check every row against the database without writing, and give it a planned action. | Worker | `VALIDATED` (new) |
| 5. Review the summary. Exclude rows, or fix the file and start over. | Admin | `VALIDATED` |
| 6. Commit the remaining rows. Each row is checked again, because data can change between validation and commit; rows that now conflict are marked `FAILED` with the reason. | Admin, then worker | `QUEUED` → `IMPORTING` → `COMPLETED` |
| 7. Send invitations, or undo the import. | Admin | `COMPLETED` or `REVERTED` (new) |

**Value cleanup in step 3:**
- Read Excel date cells as dates (`DateUtil.isCellDateFormatted`). Text dates accept `d/M/yyyy`, `dd/MM/yyyy` and `yyyy-MM-dd`.
- Normalize class names ("Lớp 10 a1" → 10A1) before matching.
- Normalize member codes before the `Code` validation.

**Saved layout per school (`ImportProfile`).** Step 3's decisions are saved: header pattern, class source, column mapping and value mapping. The first import takes about five minutes of mapping, and next year's file in the same layout takes one click. vnEdu ships as a system-wide profile.

### 5.6 Planned actions and validation rules

| Planned action | When |
|---|---|
| `INVITE` | No member or invitation matches; a new invitation will be created. |
| `INVITE_EXISTING_ACCOUNT` | The email belongs to an account that is not a member of this school. |
| `UPDATE_MEMBER` | `memberCode` matches an active member, and school-side fields change (official name, class). |
| `UPDATE_INVITATION` | `memberCode` or email matches a pending invitation. |
| `UNCHANGED` | Matches a member or invitation with identical data. |
| `ERROR` | The row cannot be imported; see its errors. |

For a full-roster file, the summary also lists **members missing from the file**: active members of the same role who are not in it. The admin can end their memberships. Partial files never list missing members.

Rules:
- Each error names the field and the problem, one entry per problem. No combined messages such as "email or phone already exists or invalid".
- A `memberCode` or email that appears twice in the file is an error on both rows.
- An unknown class is an error, grouped in the summary by class (for example, "12 rows use 10A5, which does not exist"). The import never creates classes; classes are created when the academic year is set up (6.6).
- Errors point to the sheet and the real Excel row.

### 5.7 Invitations, acceptance and undo

- **Committing** creates invitations but sends nothing.
- **Sending** is a separate step: emails for rows with an email, and a printable claim-code list per class for the rest.
- **Accepting** (sign in or sign up) creates the `school_users` row as `ACTIVE`, with role, member code and official name. If the invitation carries a class, it also creates the student's enrollment for that class's grade and year, plus the class membership (section 6).
- **Undo** is possible until invitations are sent. Invitations created by the session are deleted, and member fields and ended memberships are restored from each row's stored previous values.
- **Expiry:** invitations expire (proposed: 30 days) and can be resent or revoked.

### 5.8 Data model changes

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

**`import_sessions`**
- New statuses `VALIDATED` and `REVERTED`.
- New columns `is_full_roster`, `invitations_sent_at`, `academic_year_id`, `import_profile_id`, `updated_rows`.

**`import_rows`**
- New columns `planned_action`, `excluded`, `previous_data_json`, `sheet_name`, `source_row_number`, and `target_id` (the matched or created member or invitation).

**`member_code` uniqueness:** it is unique within a school across live members and pending invitations. A partial unique index covers each table, and validation and commit check across both.

New table `import_profiles`:
- `school_id`, nullable for system presets such as vnEdu.
- `import_type`, `name`.
- The saved structure, column mapping and value mapping as JSON.

### 5.9 Code changes

| Class | Change |
|---|---|
| `FileProcessingService`, `ParseImportFileResult` | Read all sheets, detect the header row, key columns by position (not header text), keep the real Excel row number, read cells by type, aliases for `memberCode`/`officialName`/`class`, an explicit row limit with a clear message. |
| `ImportCommitHandler` | Add `validate(session, rows)`, which sets planned actions without writing; `commit` applies them. |
| `ImportCommitService` | Run validation and undo as well as commit; count updated rows. |
| `AcceptSchoolUserImportUseCase` | Split into "confirm mapping and validate" and "commit validated session". |
| `SchoolUserImportCommitHandler` | Rewrite: creates and updates invitations and member records. No account creation, no `USER_CREATED` outbox, no writes to `users`. |
| `SchoolClassUserImportCommitHandler` | Remove once the roster import covers class assignment. |
| New `ImportProfile` model + repository | Saved layouts and system presets. |
| Value normalizers in `application/shared` | Dates, class names, member codes. Pure functions, good first unit tests. |
| New use cases | Send invitations, accept invitation, undo import, download template, download failed rows. |

The parser and the value normalizers do not depend on the rest and can start any time. Everything else needs the `school_users` target (2.3) and the academic year (section 6).

---

## 6. Academic year design sketch

### 6.1 Goals

- An admin sets up a school year once. Grades, classes and enrollments belong to it, and reports and assessments can filter by it.
- A student's enrollment comes from the year, created when they become an active member, not from dates typed per student.
- Closing a year freezes it, and the rollover wizard (2.2) moves the school to the next year in one confirmed step.

### 6.2 Model overview

```mermaid
erDiagram
    SCHOOL ||--o{ SCHOOL_ACADEMIC_YEAR : plans
    SCHOOL_ACADEMIC_YEAR ||--|{ SCHOOL_ACADEMIC_TERM : "divided into"
    SCHOOL_ACADEMIC_YEAR ||--o{ SCHOOL_GRADE : opens
    GRADE_LEVEL ||--o{ SCHOOL_GRADE : "instance of"
    SCHOOL_GRADE ||--o{ SCHOOL_CLASS : groups
    SCHOOL_GRADE ||--o{ SCHOOL_STUDENT_ENROLLMENT : enrolls
    SCHOOL_USER ||--o{ SCHOOL_STUDENT_ENROLLMENT : "one per year"
    SCHOOL_CLASS ||--o{ SCHOOL_CLASS_USER : has
    SCHOOL_USER ||--o{ SCHOOL_CLASS_USER : "member of"
    SCHOOL_INVITATION }o--o| SCHOOL_CLASS : "places into on acceptance"
```

| Model | Change | Role |
|---|---|---|
| `SchoolAcademicYear` | **New.** Replaces `SchoolAcademicTimeline`, which has no `schoolId` today. Keeping the old name works too; only the fields matter. | One school year with its terms and lifecycle. |
| `SchoolAcademicTerm` | **New**, part of the year aggregate | Semesters (Học kỳ I, Học kỳ II). |
| `SchoolGrade` | `academicTimelineId` → `academicYearId`. Name optional; the display name comes from `GradeLevel`. | A grade level offered in one year ("Khối 10, 2026–2027"). |
| `SchoolStudentEnrollment` | **New** | A student's place in a year: which grade, and the year's outcome (promoted, retained, graduated, left). |
| `SchoolClass` | Add `academicYearId` (copied from the grade so the name can be unique per year), plus `type`, program and group purpose (7.5) | A homeroom class (belongs to a program) or a group (remedial, gifted training, assessment review) within a grade. |
| `SchoolClassUser` | Add `role` and `leaveReason`. Re-joining creates a new row. | Class membership history. |
| `SchoolUser` | Target from the migration draft (role, member code, official name), with status `ACTIVE`/`SUSPENDED`/`ENDED`. `endDate` set when the membership ends; ended rows kept as history. | The school's record of a person across years. |
| `SchoolInvitation` | **New** (5.8) | A pending membership, possibly for someone without an account. |

**Why a separate enrollment record instead of deriving everything from class membership:**
- The rollover needs somewhere to record each student's outcome. Retained students (lưu ban) cannot be expressed through class membership alone.
- Grade-level rules (`GradeLevelBandScope` targets, assessment policies, quota by grade) need the student's grade for the year, whether or not the student belongs to one class, several, or none yet.
- It fits the decision in 7.5: one homeroom per student per year, plus any number of groups. The grade comes from the enrollment, and homeroom classes and groups must match it. Because students can belong to groups as well, deriving the grade from class membership would be ambiguous.

### 6.3 Domain sketches

The sketches follow the current conventions: extend `BaseModel`, hand-written `Builder`, no Lombok, status enums with `from`/`value` helpers. Constructors, builders and getters are omitted.

Dates are `LocalDate`, not `Instant`. A school year is a set of calendar days in the school's zone, so storing `Instant`s forces zone conversions such as `DateMapper.DEFAULT_INPUT_ZONE` everywhere. Year and term names are plain `String`s because the `Name` value object requires every word to start with a capital letter, which "2026-2027" and "Học kỳ I" don't.

```java
package com.sep.vox.domain.model.school;

public enum SchoolAcademicYearStatus {
    PLANNING,
    ACTIVE,
    CLOSED;
    // from(String) / value(SchoolAcademicYearStatus) as in the other status enums
}
```

```java
package com.sep.vox.domain.model.school;

public class SchoolAcademicYear extends BaseModel {
    private UUID schoolId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private SchoolAcademicYearStatus status;
    private List<SchoolAcademicTerm> terms;
    private Instant activatedAt;
    private Instant closedAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public static SchoolAcademicYear plan(UUID schoolId, String name, LocalDate startDate, LocalDate endDate,
            List<SchoolAcademicTerm> terms, UUID createdBy, Instant now) {
        requireValidRange(startDate, endDate);
        requireValidTerms(startDate, endDate, terms);
        return SchoolAcademicYear.builder()
            .schoolId(schoolId)
            .name(name)
            .startDate(startDate)
            .endDate(endDate)
            .terms(List.copyOf(terms))
            .status(SchoolAcademicYearStatus.PLANNING)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(createdBy)
            .build();
    }

    /** Dates and terms can be reshaped freely while the year is still being planned. */
    public void reschedule(LocalDate newStartDate, LocalDate newEndDate, List<SchoolAcademicTerm> newTerms,
            UUID actorId, Instant now) {
        requireStatus(SchoolAcademicYearStatus.PLANNING);
        requireValidRange(newStartDate, newEndDate);
        requireValidTerms(newStartDate, newEndDate, newTerms);
        this.startDate = newStartDate;
        this.endDate = newEndDate;
        this.terms = List.copyOf(newTerms);
        touch(actorId, now);
    }

    /**
     * An active year can only grow: shrinking it could leave assessments or memberships
     * that are already dated inside it outside the year.
     */
    public void extendTo(LocalDate newEndDate, UUID actorId, Instant now) {
        requireStatus(SchoolAcademicYearStatus.ACTIVE);
        if (!newEndDate.isAfter(endDate)) {
            throw new IllegalArgumentException("An active academic year can only be extended");
        }
        this.endDate = newEndDate;
        touch(actorId, now);
    }

    public void activate(UUID actorId, Instant now) {
        requireStatus(SchoolAcademicYearStatus.PLANNING);
        this.status = SchoolAcademicYearStatus.ACTIVE;
        this.activatedAt = now;
        touch(actorId, now);
    }

    public void close(UUID actorId, Instant now) {
        requireStatus(SchoolAcademicYearStatus.ACTIVE);
        this.status = SchoolAcademicYearStatus.CLOSED;
        this.closedAt = now;
        touch(actorId, now);
    }

    public boolean acceptsStructureChanges() {
        return status != SchoolAcademicYearStatus.CLOSED;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean overlaps(LocalDate otherStartDate, LocalDate otherEndDate) {
        return !otherEndDate.isBefore(startDate) && !otherStartDate.isAfter(endDate);
    }

    public Optional<SchoolAcademicTerm> termOn(LocalDate date) {
        return terms.stream().filter(term -> term.contains(date)).findFirst();
    }

    private void requireStatus(SchoolAcademicYearStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Academic year must be " + expected + " but is " + status);
        }
    }

    private static void requireValidRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || !startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("Academic year start date must be before its end date");
        }
    }

    /** Terms are numbered 1..n, lie inside the year and do not overlap; gaps (holidays) are allowed. */
    private static void requireValidTerms(LocalDate startDate, LocalDate endDate, List<SchoolAcademicTerm> terms) {
        if (terms == null || terms.isEmpty()) {
            throw new IllegalArgumentException("An academic year needs at least one term");
        }
        List<SchoolAcademicTerm> sorted = terms.stream()
            .sorted(Comparator.comparing(term -> term.getOrder().value()))
            .toList();
        LocalDate previousEndDate = null;
        for (int index = 0; index < sorted.size(); index++) {
            SchoolAcademicTerm term = sorted.get(index);
            if (term.getOrder().value() != index + 1) {
                throw new IllegalArgumentException("Term order must run from 1 without gaps");
            }
            if (term.getStartDate().isBefore(startDate) || term.getEndDate().isAfter(endDate)) {
                throw new IllegalArgumentException("Terms must lie inside the academic year");
            }
            if (previousEndDate != null && !term.getStartDate().isAfter(previousEndDate)) {
                throw new IllegalArgumentException("Terms must not overlap");
            }
            previousEndDate = term.getEndDate();
        }
    }

    private void touch(UUID actorId, Instant now) {
        this.updatedBy = actorId;
        this.updatedAt = now;
    }
}
```

```java
package com.sep.vox.domain.model.school;

/** Part of the SchoolAcademicYear aggregate: only changed through the year. */
public class SchoolAcademicTerm extends BaseModel {
    private UUID academicYearId;
    private String name;
    private PositiveInteger order;
    private LocalDate startDate;
    private LocalDate endDate;

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
```

```java
package com.sep.vox.domain.model.school;

public enum SchoolStudentEnrollmentStatus {
    ENROLLED,   // studying this year, outcome not decided yet
    PROMOTED,   // moves to the next grade level next year
    RETAINED,   // repeats the same grade level next year
    GRADUATED,  // finished the last grade level
    LEFT;       // transferred out, dropped out, ...
}
```

```java
package com.sep.vox.domain.model.school;

public class SchoolStudentEnrollment extends BaseModel {
    private UUID schoolId;
    private UUID academicYearId;
    private UUID schoolGradeId;
    private UUID userId;
    private SchoolStudentEnrollmentStatus status;
    private LocalDate enrolledOn;
    private LocalDate leftOn;
    private String leaveReason;
    private Instant decidedAt;
    private UUID decidedBy;

    public static SchoolStudentEnrollment enroll(UUID schoolId, UUID academicYearId, UUID schoolGradeId,
            UUID userId, LocalDate enrolledOn) {
        return SchoolStudentEnrollment.builder()
            .schoolId(schoolId)
            .academicYearId(academicYearId)
            .schoolGradeId(schoolGradeId)
            .userId(userId)
            .status(SchoolStudentEnrollmentStatus.ENROLLED)
            .enrolledOn(enrolledOn)
            .build();
    }

    public void promote(UUID actorId, Instant now) {
        decide(SchoolStudentEnrollmentStatus.PROMOTED, actorId, now);
    }

    public void retain(UUID actorId, Instant now) {
        decide(SchoolStudentEnrollmentStatus.RETAINED, actorId, now);
    }

    /** The caller checks that the grade is the school's last grade level. */
    public void graduate(UUID actorId, Instant now) {
        decide(SchoolStudentEnrollmentStatus.GRADUATED, actorId, now);
    }

    public void leave(LocalDate leftOn, String reason, UUID actorId, Instant now) {
        decide(SchoolStudentEnrollmentStatus.LEFT, actorId, now);
        this.leftOn = leftOn;
        this.leaveReason = reason;
    }

    public boolean isOpen() {
        return status == SchoolStudentEnrollmentStatus.ENROLLED;
    }

    private void decide(SchoolStudentEnrollmentStatus outcome, UUID actorId, Instant now) {
        if (!isOpen()) {
            throw new IllegalStateException("Enrollment outcome has already been decided: " + status);
        }
        this.status = outcome;
        this.decidedBy = actorId;
        this.decidedAt = now;
    }
}
```

Changes to existing models (fields only):

```java
public class SchoolGrade extends BaseModel {
    private UUID schoolId;
    private UUID academicYearId;      // was academicTimelineId
    private UUID gradeLevelId;
    private String description;
    private SchoolGradeStatus status; // ACTIVE, ARCHIVED
    // updatedAt, createdBy, updatedBy
}

public class SchoolClass extends BaseModel {
    private UUID schoolId;
    private UUID academicYearId;      // new, copied from the grade; makes name unique per year
    private UUID schoolGradeId;
    private UUID learningLanguageId;
    private SchoolClassType type;            // new: HOMEROOM, GROUP (7.5)
    private UUID programId;                  // new: homeroom classes only
    private SchoolGroupPurpose groupPurpose; // new: groups only: REMEDIAL, ADVANCED, ASSESSMENT_REVIEW
    private String name;              // normalized: "10A1"
    private String description;
    private SchoolClassStatus status;
    private Instant archivedAt;
    // updatedAt, createdBy
}

public class SchoolClassUser {
    private UUID id;
    private UUID userId;
    private UUID schoolClassId;
    private SchoolClassMemberRole role;          // new: STUDENT, TEACHER
    private Instant joinAt;
    private Instant leftAt;
    private SchoolClassLeaveReason leaveReason;  // new: MOVED, LEFT_SCHOOL, YEAR_CLOSED, REMOVED
    private UUID assignedBy;
}
```

### 6.4 Lifecycle and invariants

```mermaid
stateDiagram-v2
    [*] --> PLANNING: plan
    PLANNING --> PLANNING: reschedule
    PLANNING --> [*]: delete
    PLANNING --> ACTIVE: activate
    ACTIVE --> ACTIVE: extendTo
    ACTIVE --> CLOSED: close
```

**Academic year**

| Invariant | Enforced in |
|---|---|
| Start before end; terms numbered 1..n, inside the year, non-overlapping | `SchoolAcademicYear` |
| Name unique per school | DB unique constraint |
| Years of one school never overlap | DB exclusion constraint (or `CreateAcademicYearUseCase` if you'd rather not add `btree_gist`) |
| At most one `ACTIVE` year per school | DB partial unique index |
| Only `PLANNING` years can be deleted | Use case |
| No new grades, classes, enrollments or assessments in a `CLOSED` year | `acceptsStructureChanges()`, checked by those use cases |

**Membership, enrollment and classes**

| Invariant | Enforced in |
|---|---|
| Only `ACTIVE` members can be enrolled, placed in classes or added to assessments; pending invitations show as pending | Use cases |
| One enrollment per student per year | DB unique constraint |
| One open homeroom membership per student per year; any number of groups | `CreateSchoolClassUserUseCase`, invitation acceptance |
| A student's homeroom class and groups must belong to the grade of their enrollment that year | `CreateSchoolClassUserUseCase`, invitation acceptance |
| At most one open membership per (class, user); re-joining creates a new row | DB partial unique index |
| Class name unique per year | DB unique constraint |

**Closing a year** (inside the rollover commit, or standalone):
1. Refuse while any assessment of the year is `IN_PROGRESS`. Warn about results not yet published.
2. Require an outcome for every enrollment still `ENROLLED`. The rollover preview proposes defaults: `PROMOTED`, or `GRADUATED` for the last grade.
3. Archive the year's grades and classes, and set `leftAt` on open class memberships with reason `YEAR_CLOSED`.
4. End the school memberships (`ENDED`, `endDate`) of `GRADUATED` and `LEFT` students. Their accounts are untouched.
5. Pending invitations that point at this year's classes are re-targeted through the rollover's class mapping, or revoked.

**Rollover commit** (one transaction, or queued like imports if it gets slow):
1. Close the current year (above).
2. Activate the next year. It was created as `PLANNING` with a copy of the current structure.
3. For `PROMOTED` and `RETAINED` students, create next year's enrollment in the right grade, plus class memberships from the class mapping (10A1 → 11A1).

### 6.5 Schema sketch (PostgreSQL)

This goes into the Flyway baseline, or into new migrations if production data must be kept (open question 11). JPA `@Index` cannot express partial or exclusion constraints, so these belong in the Flyway SQL.

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE school_academic_years (
    id            uuid PRIMARY KEY,
    school_id     uuid         NOT NULL REFERENCES schools (id),
    name          varchar(100) NOT NULL,
    start_date    date         NOT NULL,
    end_date      date         NOT NULL,
    status        varchar(20)  NOT NULL CHECK (status IN ('PLANNING', 'ACTIVE', 'CLOSED')),
    activated_at  timestamptz,
    closed_at     timestamptz,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL,
    created_by    uuid         NOT NULL,
    updated_by    uuid,
    CONSTRAINT ck_school_academic_years_range CHECK (start_date < end_date),
    CONSTRAINT uq_school_academic_years_name UNIQUE (school_id, name),
    CONSTRAINT ex_school_academic_years_overlap EXCLUDE USING gist (
        school_id WITH =,
        daterange(start_date, end_date, '[]') WITH &&
    )
);

CREATE UNIQUE INDEX uq_school_academic_years_one_active
    ON school_academic_years (school_id) WHERE status = 'ACTIVE';

CREATE TABLE school_academic_terms (
    id                uuid PRIMARY KEY,
    academic_year_id  uuid         NOT NULL REFERENCES school_academic_years (id) ON DELETE CASCADE,
    name              varchar(100) NOT NULL,
    term_order        integer      NOT NULL CHECK (term_order > 0),
    start_date        date         NOT NULL,
    end_date          date         NOT NULL,
    CONSTRAINT ck_school_academic_terms_range CHECK (start_date <= end_date),
    CONSTRAINT uq_school_academic_terms_order UNIQUE (academic_year_id, term_order)
);

-- school_grades: academic_timeline_id -> academic_year_id
ALTER TABLE school_grades
    ADD CONSTRAINT uq_school_grades_year_level UNIQUE (academic_year_id, grade_level_id);

CREATE TABLE school_student_enrollments (
    id                uuid PRIMARY KEY,
    school_id         uuid        NOT NULL REFERENCES schools (id),
    academic_year_id  uuid        NOT NULL REFERENCES school_academic_years (id),
    school_grade_id   uuid        NOT NULL REFERENCES school_grades (id),
    user_id           uuid        NOT NULL REFERENCES users (id),
    status            varchar(20) NOT NULL CHECK (status IN ('ENROLLED', 'PROMOTED', 'RETAINED', 'GRADUATED', 'LEFT')),
    enrolled_on       date        NOT NULL,
    left_on           date,
    leave_reason      varchar(255),
    decided_at        timestamptz,
    decided_by        uuid,
    created_at        timestamptz NOT NULL,
    CONSTRAINT uq_school_student_enrollments_year_user UNIQUE (academic_year_id, user_id)
);

-- school_classes: drop code; add academic_year_id; replace the broken language index
ALTER TABLE school_classes
    ADD CONSTRAINT uq_school_classes_year_name UNIQUE (academic_year_id, name);

-- school_class_users: add role and leave_reason; history rows instead of reactivation
CREATE UNIQUE INDEX uq_school_class_users_open
    ON school_class_users (school_class_id, user_id) WHERE left_at IS NULL;

-- school_users: replace UNIQUE (user_id) and UNIQUE (school_id, user_id);
-- uniqueness only among live memberships, ended rows kept as history
CREATE UNIQUE INDEX uq_school_users_live_member
    ON school_users (school_id, user_id) WHERE status <> 'ENDED';
CREATE UNIQUE INDEX uq_school_users_live_member_code
    ON school_users (school_id, member_code) WHERE status <> 'ENDED';
-- school_invitations gets the same partial index on (school_id, member_code) WHERE status = 'PENDING';
-- uniqueness across both tables is checked in import validation and commit.
```

### 6.6 Use cases

New, in `application/port/input/usecase/academicyear`:

| Use case | Notes |
|---|---|
| `CreateAcademicYearUseCase` | Name, dates, terms, offered grade levels (default: last year's set). Optionally copies or bulk-creates the class structure ("10A1–10A12"). Creates the year's `SchoolGrade`s. |
| `UpdateAcademicYearUseCase` | `reschedule` while `PLANNING`, `extendTo` while `ACTIVE`. |
| `ActivateAcademicYearUseCase` | Used directly for the very first year; otherwise through the rollover. |
| `CloseAcademicYearUseCase` | Section 6.4. |
| `DeleteAcademicYearUseCase` | `PLANNING` only. |
| `ViewAcademicYearsUseCase`, `ViewAcademicYearDetailsUseCase` | Includes terms, grades and counts. |
| `PreviewAcademicYearRolloverUseCase` | Class mapping plus a proposed outcome per student, with exceptions highlighted. |
| `CommitAcademicYearRolloverUseCase` | Section 6.4. |

Changes to existing use cases:

| Use case | Change |
|---|---|
| `CreateSchoolUserUseCase` | Replaced by an invite use case (one invitation). No account creation, no student start/end dates. |
| `UpdateSchoolUserUseCase` | Edits only the school's record: official name, member code, role, identity photo. |
| `DeleteSchoolUserUseCase` | Replaced by suspend / end membership (2.3). Never touches the account. |
| Accept invitation (new, person-side) | Creates the `school_users` row; for a student with a class, also the enrollment and class membership. |
| `CreateSchoolGradeUseCase` | Becomes "add a grade level to a year". No dates, no code. |
| `CreateSchoolClassUseCase` | Grade implies year. Rejects `CLOSED` years. Name unique per year. |
| `CreateSchoolClassUserUseCase` | Requires an `ACTIVE` member and a membership role. For students, requires an open enrollment whose grade matches the class. |
| Authorization lookups in `SchoolUserRepository` | Only `ACTIVE` memberships count (2.3). |

### 6.7 Follow-ups in other modules

- **Assessments:** add `academicYearId` (and optionally `termId`), set at creation. Reject new assessments in `CLOSED` years. Candidates must be `ACTIVE` members. Dashboards and exports filter by year and term. Scope and assessment type are covered in 7.7.
- **Assessment policies:** keyed by the year's school grades, programs and groups (7.6); the rollover copies them to the next year.
- **Quotas:** school-paid practice allocation by grade or class uses enrollments (2.7).

---

## 7. Evaluation configuration

How a school decides what the AI evaluates against: framework, grade expectations, scoring scheme (today's "rubric"), assessment policy and scoring rules. Assessment models are out of scope except where they consume these settings.

### 7.1 What decides a student's score today

| Setting | Where | Effect today |
|---|---|---|
| Criterion descriptors and positive/negative signals per level | Framework version | Sent to the AI; the core of scoring. |
| Target level | Policy | Chooses which descriptors the AI scores against: the whole ladder of levels for grammar, vocabulary and coherence (`SubmitAssessmentSessionUseCase.java:340`), only the target level for pronunciation and fluency. |
| Which criteria are included, and their weights | Rubric version | Item score = Σ(score × weight) / Σ weight. |
| Score scale | Rubric version | The range of every criterion score. |
| Result bands | Rubric version | The label on the result. A score in a gap between bands gets no label. |
| Passing score | Policy | PASSED/FAILED when results are published (`UpdateAssessmentStatusUseCase.java:461`). |
| `required` | Rubric criterion | Human graders must score it (`GradingItemScoreResolver.java:152`); skipped optional criteria drop out of the weighting. |

**No effect on grading:**
- `strictness`: only copied when cloning (`CloneSystemRubricToSchoolUseCase.java:236`), and not part of the AI request.
- Scoring rules (17 conditions × 11 actions).
- Policy scope (class / school grade / grade level), and the effective dates of policies and rubric versions.
- Criterion `examples`.
- Criterion min/max: they must equal the scale (`RubricScoringConsistencyValidator.assertEveryCriterionCoversScale`).
- `totalScoreMethod`: it only constrains the weights (sum to 1, or all 1); both use the same formula.
- Rubric criterion `code`: it must equal the framework code (3.2).
- Criterion name and description: shown to graders only.

### 7.2 Problems found

1. **Scoring rules never run.** The `scoringrule` package has 12 CRUD use cases and no evaluator, and `AssessmentItemRuleHit` is never saved. The behavior rules would provide is hard-coded instead: human-review confidence thresholds in `ConfidenceReviewCalculator` (`C_REF_SOFT = 0.90`, …) and invalid results when there's no transcript (`SubmitAssessmentSessionUseCase.persistInvalidBlockedResult`).
2. **Policy scope is a label, and it opens a hole in the level ceilings.**
   - Grading reads `assessments.assessment_policy_id`. `findActivePolicy` and `findCurrentPolicyForStudent` have no callers (see the comment at `SystemAssessmentPolicyInitializer.java:264`), and `coversScope`/`isSelectableAt` are never called. `AssessmentPolicyValidator.java:57` checks only status and school.
   - The "one active policy per scope" rule (`CreateSchoolAssessmentPolicyUseCase.java:180`) still restricts authoring for a lookup that no longer happens.
   - `GradeLevelBandScopeGuardService` gives class-scoped policies the higher class-test ceiling, and nothing stops a whole-grade assessment from using such a policy.
3. **`strictness` is never used.**
4. **The framework version is stored twice and can disagree.** The policy has `frameworkVersionId`, while the rubric's criteria point at framework criteria of a specific version. Policy creation only checks they belong to the same framework (`CreateSchoolAssessmentPolicyUseCase.java:226`). A mismatch makes `SubmitAssessmentSessionUseCase` throw "Tiêu chí framework chưa có mô tả cho bậc mục tiêu" when it builds the AI request. Latent while only one framework version exists.
5. **Effective dates are never enforced** for policies or rubric versions.
6. **Publishing order is backwards.** A school rubric version can only be published once a policy linked to it is published (`ChangeSchoolRubricVersionStatusUseCase`), while `PublishSchoolAssessmentPoliciesByRubricVersionUseCase.java:86` requires the rubric to still be a draft. The assessment check doesn't look at the rubric's status, so an assessment can be graded against an editable draft rubric.
7. **Result bands and `passingScore` can contradict each other.** Nothing checks one against the other.
8. **Results don't say what level the student reached.** `AssessmentCandidateResult` stores the policy's target level. The level the AI matched per criterion (`AssessmentItemCriterionScore.matchedBandCode`) is stored but never aggregated.
9. **Ceiling columns are misnamed.** `GradeLevelBandScope.defaultTargetBandId` is the ceiling for whole-grade assessments and `hardMaxBandId` the ceiling for class tests (see the header of `GradeLevelBandScopeGuardService`). Both are seeded for every grade, as Bậc 4 and Bậc 5 (`GradeLevelBandScopeInitializer`).

### 7.3 Target model

**Definition:** an assessment policy is the framework level that a grade, a program or a group is evaluated at, together with the scoring scheme that turns the AI's judgments into scores.

| Layer | Owner | Holds |
|---|---|---|
| Framework | System | The fixed 5 criteria, the levels (KNLNNVN Bậc 1–6), descriptors and signals. Practice uses it directly with the level the student picks. |
| Grade expectations | System | Per (grade level, program kind): a recommended level and a ceiling. Replaces today's two ceiling columns. |
| Scoring scheme (today's "rubric") | System templates; schools copy and adjust | Scale (default 0–10), a weight per framework criterion, result labels each marked passing or not. Edited as one document (7.4). |
| Assessment policy | School | Target level and scoring scheme version for a grade, a program or a group, with an effective-from date (7.6). |
| Evaluation guards | Platform | Human-review and invalid-response thresholds (today's hard-coded ones). |

Keeping the scoring scheme separate from the policy makes sense: one scheme ("our 10-point scale") is reused across grades, while the target level differs per grade.

**Decisions**
- **School-level scoring rules are removed.** Evaluation guards stay platform-level; named school settings can be added later if schools ask for them.
- **`strictness` is removed** unless the AI service gets a concrete meaning for it.
- **The framework version comes from the target level** and is never stored separately.
- **The pass mark comes from the scheme's labels**, so the two can't contradict each other.
- **Scope is enforced when an assessment is scheduled** (7.7), which closes the ceiling hole.

### 7.4 Editing frameworks and rubrics as whole versions

A version, with its criteria and bands, is the unit that is created, edited and published, instead of one use case per criterion or band.

**Why**
- **A rubric is one table:** criteria with weights, plus result labels across the scale. Saving it piece by piece leaves half-saved drafts when a call fails.
- **Schools rarely build from scratch.** They clone a system template (`CloneSystemRubricToSchoolUseCase`) and adjust it.
- **Versions are small.** A framework has 5 criteria (`FrameworkCriterionCode.ALLOWED_CODES`) and a handful of levels; the criterion × level grid is about 30 cells.
- **Published versions are already frozen.** Every child edit requires `DRAFT`, and a new version copies from an existing one (`sourceRubricVersionId`).

**The rules span the whole version**

| Rule | Today |
|---|---|
| Weights add up (`SUM` → 1, `WEIGHTED_AVERAGE` → 1 each) | Only checked at publish (`RubricScoringConsistencyValidator`). The comment at `CreateSchoolRubricCriterionUseCase.java:126` records weights stored 100× too large that were only caught at publish. |
| Bands don't overlap | Checked per band create or update. Older overlapping data still reaches grading, which logs an error and takes the first match (`AssessmentSessionResultCalculator.java:304`). |
| Bands cover the scale without gaps | **Never checked**, and publishing doesn't even require bands. A score in a gap is saved without a label, with only an INFO log (`AssessmentSessionResultCalculator.java:297`). |
| Changing the scale keeps children inside it | `UpdateSchoolRubricVersionUseCase` reloads every band and criterion to check this. |
| Every framework criterion has a descriptor and signals at every level | Only checked at publish, in the removed `UpdateFrameworkVersionStatusUseCase`. |

**Two levels of validation**
- **Save draft:** structural checks only: values inside the scale, no duplicate framework criteria, weights between 0 and 1. Incomplete drafts are allowed, so work in progress can be saved.
- **Publish:** completeness: weights add up, bands cover the scale with no gaps or overlaps, framework descriptors and signals are filled in.
- **Errors:** every violation is returned as a list with field paths such as `criteria[2].weight`, not one exception at a time.

**Simpler scoring scheme**
- Drop criterion min/max (always the scale).
- Drop criterion `code`; read it from the linked framework criterion.
- Drop `totalScoreMethod`; weights are entered as percentages, equal by default.
- Drop `examples` (never read).
- Mark each result label as passing or not.

**Use cases**

One rubric set serves system and school rubrics, with an owner check (system admin for `SYSTEM`, the school's admin for `SCHOOL`). It replaces the 33 files in `rubricschool` and the 32 in `rubricsystem`:
- `CreateRubricUseCase`: the rubric plus its first draft version, from scratch or cloned from a system template.
- `CreateRubricVersionUseCase`: a new draft copied from an existing version.
- `SaveRubricVersionDraftUseCase`: version fields, criteria and bands, all at once.
- `PublishRubricVersionUseCase`, `ArchiveRubricVersionUseCase`, `DeleteRubricVersionUseCase` (drafts only).
- View and search use cases that return the whole version.

The framework set is for the system admin only, and replaces the 29 use cases removed in `424c1b83`:
- Create and update the framework itself.
- `CreateFrameworkVersionUseCase`, optionally copied from an earlier version.
- `SaveFrameworkVersionDraftUseCase`: the levels plus the 5 × N grid of descriptors and signals.
- Publish, archive, delete draft, view.

**Domain model**
- **Aggregates:** `RubricVersion` and `FrameworkVersion` become aggregate roots holding their criteria and bands. Each gets `replaceContent(...)` for the structural checks and `publish()` for the completeness checks; `RubricScoringConsistencyValidator` moves under `publish()`.
- **Concurrent edits:** an optimistic version number on the version row, so a second concurrent save gets a conflict instead of silently overwriting the first.
- **Child IDs:** published criterion IDs never change, because results store them (`AssessmentItemCriterionScore.rubricCriterionId`). Draft children can be replaced freely once grading can only use published versions (next point).

**Publishing order.** Reverse the dependency. Publishing a rubric checks only the rubric. Publishing a policy, or attaching it to an assessment, requires a published rubric version. "Publish the scheme and its targets" can still be one button.

**Import.** The three rubric imports become one workbook (sheets "Version", "Criteria", "Bands") feeding the same save-draft command, so file and screen go through the same validation.

### 7.5 Programs and groups

**What schools do.** From research during this discussion; to confirm with pilot schools:
- **Specialized classes:** only specialized schools (trường chuyên) may open them. They have their own curriculum and their own tests.
- **Higher-demand classes at regular schools** (lớp chọn, lớp trọng điểm tiếng Anh, lớp nâng cao): they follow the same curriculum. Commonly they sit the same mid-term and end-of-term tests as the rest of the grade, and some schools also give them separate tests.
- **Extra groups:** as we understand Circular 29/2024 on extra teaching, schools may run free sessions at school for three kinds of students. These are usually groups drawn from several classes, per subject:
  - students who did not meet requirements at the end of the previous semester (phụ đạo);
  - students selected for gifted training (bồi dưỡng học sinh giỏi);
  - final-year students reviewing for the graduation assessment (ôn thi).

These rules vary by school type, school level and province, so they are modeled as data each school defines, not as system rules.

**Homeroom classes and programs**
- **Program:** every homeroom class belongs to one, with a name the school chooses (Chuẩn, Lớp chọn, Trọng điểm tiếng Anh, Chuyên Anh…).
- **Kind:** each program has a kind the system understands, `STANDARD` or `ADVANCED`. Ceilings attach to the kind.
- **Periodic assessment:** each program is either `SHARED_WITH_GRADE` (the default) or `SEPARATE` (7.7).
- **Defaults:** a regular school starts with just "Chuẩn". The system never ships "Chuyên" as a default.
- **Moving between programs:** some schools re-sort lớp chọn after semester 1. Since the program belongs to the homeroom class, a move is just moving the student to another homeroom class.

**Groups**
- **Type and purpose:** class type `GROUP`, with a purpose of `REMEDIAL`, `ADVANCED` (gifted training) or `ASSESSMENT_REVIEW`.
- **Own target:** a group may have its own target, lower (a stepping stone for remedial groups) or higher (gifted training, up to the advanced ceiling).
- **Not periodic:** group tests are `REGULAR` or `PRACTICE` (7.7). Students' official mid-term and end-of-term results always come from their program's assessments.

**Membership rule:** one homeroom class per student per year, plus any number of groups.

### 7.6 Configuring targets

The school admin, or the English department head, sets targets once per school year:

```
Evaluation targets · English · 2026–2027                                  [Publish]
Framework: Khung năng lực ngoại ngữ 6 bậc (KNLNNVN v1)

Regular school
Grade 10  Whole-grade assessments       Bậc 3 ▾  (up to Bậc 4)   10-point scale (system)
          Program                 Target          Mid/end-of-term   Classes
          Chuẩn                   uses grade's    shared            10A4–10A12
          Trọng điểm tiếng Anh    Bậc 4 ▾         shared            10A1–10A3

Specialized school
Grade 10  Whole-grade assessments       Bậc 3 ▾
          Chuyên Anh              Bậc 4 ▾         separate          10 Anh 1, 10 Anh 2
          Chuyên Toán             uses grade's    shared            10 Toán

Groups
          Phụ đạo tiếng Anh 10    Bậc 2 ▾         (regular / practice tests only)
```

- **Prefilled:** targets start at the recommended level from grade expectations (e.g. Bậc 3, the national upper-secondary target), so the admin only changes what differs.
- **Readable levels:** each level shows its descriptor per criterion in plain language. Admins aren't assessment experts, so they need to see what they're choosing.
- **One publish for the whole year.** The rollover copies targets to the next year, and promoted students move to the next grade's targets through their enrollment.

**Checks at publish**
- Each target is within the ceiling of its program kind.
- The level belongs to a published framework version.
- Every criterion of the scoring scheme has a descriptor and signals at that level. Today a missing one only shows up when a student submits (`SubmitAssessmentSessionUseCase`).
- The scoring scheme is published.
- Warn about grades or programs without a target.
- Warn when a `SEPARATE` program's target is above the grade's (7.7).

### 7.7 Assessment scope and assessment types

**Assessment types.** As we understand Circular 22/2021, a secondary student's subject result combines three kinds of score:
- **Regular assessment** (đánh giá thường xuyên, ĐĐGtx): several per term.
- **Mid-term** (giữa kỳ, ĐĐGgk): one per term.
- **End-of-term** (cuối kỳ, ĐĐGck): one per term.

The term average is (ΣĐĐGtx + 2 × ĐĐGgk + 3 × ĐĐGck) / (number of ĐĐGtx + 5). Each assessment therefore carries an `assessmentType` of `REGULAR`, `MID_TERM`, `END_OF_TERM` or `PRACTICE` (not recorded).

**Scope and target**

| Assessment scope | Allowed assessment types | Target used |
|---|---|---|
| Whole grade (programs set to `SHARED_WITH_GRADE`) | All | Grade target |
| One program set to `SEPARATE` | All | Program target |
| One program set to `SHARED_WITH_GRADE` | `REGULAR`, `PRACTICE` | Program target |
| One homeroom class | `REGULAR`, `PRACTICE` | Its program's target (the grade's for a standard program) |
| A group | `REGULAR`, `PRACTICE` | Group target |

**Rules**
- **Resolved at scheduling:** the policy comes from the assessment's scope and date, and is saved in `assessment.assessmentPolicyId`, so the grading path doesn't change. Teachers never pick a policy; today `CreateClassTestUseCase.java:130` asks for one.
- **Special cases:** for something like a Bậc 4 mock assessment for grade 12, the admin picks another level within the ceiling and gives a reason, which is recorded on the assessment.
- **One periodic assessment per type:** each student has at most one `MID_TERM` and one `END_OF_TERM` assessment per term. The term comes from the assessment date and the academic year's terms. Warn when a `SEPARATE` program has no mid-term or end-of-term assessment scheduled.
- **Candidate import:** follows the scope and adds students only. Today `ImportAssessmentCandidatesFromGradeUseCase` adds every class of the grade and every active class member, including teachers.

**Warning on `SEPARATE` with a higher target.** Those students' periodic scores are measured against a harder standard than their peers' on the same 10-point scale. Their averages, and the awards that depend on them, may drop. This is likely why shared tests are the common practice.

### 7.8 What the AI receives and how results are reported

- **The AI request:** for each criterion of the scoring scheme, the target level's descriptors and signals. For grammar, vocabulary and coherence it also gets the whole ladder of levels, so the AI reports the level it matched.
- **Scores are relative to their target.** 8/10 at Bậc 3 and 7/10 at Bậc 4 aren't comparable. So:
  - every score is shown with its target, e.g. "7.2/10 · Bậc 3 · Đạt";
  - statistics are split by target automatically, and programs with different targets are never averaged or ranked together;
  - exports have columns by assessment type, matching the grade book.
- **Two outcomes are reported:**
  - **Met target:** pass/fail, from the scheme's passing labels.
  - **Estimated level:** derived from the levels the AI matched on the ladder criteria. It answers "how many grade-12 students reached Bậc 3?" and needs a new `AssessmentCandidateResult.estimatedBandId`.

### 7.9 Changing targets

**No raising in reaction to results.** The target describes what students are expected to achieve. As we understand Circular 22/2021, students are assessed against the program's required outcomes. Raising the target because a cohort did well is grading on a curve: "Đạt Bậc 3" would mean different things across classes and years, and a class would be penalized for doing well.

**High results usually mean one of three things:**
1. **The papers are too easy.** Fix question difficulty and blueprints.
2. **The AI is too lenient.** Compare a teacher-graded sample with the AI's scores (`ViewAiQualityReportUseCase`).
3. **The cohort really is above target.** Move strong students into an advanced group or program, or plan a higher target for the next term or year.

**System support**
- **Dashboard signal:** flag a grade or program when, for example, more than 60% score 9–10 at their target, or most estimated levels are above it, and suggest the three checks above. The thresholds are adjustable.
- **Planned changes only:** a change takes effect from a chosen date (default: the next term's start), applies only to assessments scheduled after it, requires a reason and is logged. Results already given are never re-scored.
- **Estimated level makes high achievers visible.** When everyone scores 9–10 at Bậc 3, the score can't tell students apart, but the estimated level shows who is at Bậc 4.

### 7.10 Practice

For school students, their program's (or grade's) target is the *suggested* practice level, and they can still choose another. Practice stays the student's own; `StartPracticeSessionPersistenceService` already uses the level the student picks.

### 7.11 Model and use case changes

| Area | Change |
|---|---|
| `AssessmentPolicy` | Keyed by (school grade) for the whole-grade target, (school grade, program), or (group). Holds target level, scoring scheme version, effective-from date and reason. Drop the grade-level and per-class scopes, `frameworkVersionId` (derived from the level), `strictness`, `effectiveTo` (a newer version replaces it) and `passingScore` (comes from the labels). |
| `GradeLevelBandScope` | (grade level, program kind) → recommended level and ceiling. |
| `SchoolProgram` (new) | School, name, kind (`STANDARD`/`ADVANCED`), `periodicAssessment` (`SHARED_WITH_GRADE`/`SEPARATE`). Persists across years. |
| `SchoolClass` | `type` (`HOMEROOM`/`GROUP`). Homeroom classes reference a program; groups have a purpose. |
| `Assessment` | Scope (grade / program / class / group), `assessmentType`, override reason. `assessmentPolicyId` stays as the resolved policy, saved at scheduling. |
| `AssessmentCandidateResult` | `estimatedBandId`. |
| `RubricVersion`, `FrameworkVersion` | Aggregates with whole-version save and publish (7.4). |
| `RubricCriterion`, `RubricResultBand`, `RubricVersion` | Drop criterion min/max, `code` and `examples`, and `totalScoreMethod`. Add `passing` to result bands. |
| Scoring rules | Remove the school and system rule CRUD (12 use cases). Keep or drop the tables depending on whether platform rules come later. |
| New services | `AssessmentPolicyResolver` (scope + date → policy). A periodic-assessment check (one mid-term and one end-of-term per student per term). |
| `AssessmentPolicyValidator` | Also require a published rubric version and a matching scope. |

New use cases for targets: view the year's targets, save draft targets, publish targets, manage programs, and plan a target change.

---

## 8. Reconciliation with the earlier drafts

| Topic | Earlier drafts | This document |
|---|---|---|
| Where invitations live | Migration draft §4: `school_users.status = INVITED`. Import draft §8: separate `school_invitations` table. | **Separate table.** An invitation may target someone without an account, and `school_users.user_id` is required. Claim codes, expiry and the class to apply also belong to the invitation. `school_users.status` becomes `ACTIVE`/`SUSPENDED`/`ENDED`. |
| Suspending school access | Not covered | **Added** `SUSPENDED` (2.3). |
| Birth date in the roster | Migration draft §5.5 lists it; import draft §4 drops it. | **Dropped.** Matching uses member code or email only. |
| Classes missing from the file | Import draft: an error, never created by the import. | **Kept.** Classes are created during academic-year setup (6.6). |
| Year rollover | Import draft: a full-roster file lists missing members to end. | **Kept for exceptions.** The main path is the rollover wizard with per-student outcomes (2.2, 6.4). |
| Enrollment dates | Membership `start_date`/`end_date` | Membership dates stay; the per-year place comes from `school_student_enrollments` (section 6). |

---

## 9. Open questions

1. ~~One class per student per year?~~ **Resolved:** one homeroom class per student per year, plus any number of groups (7.5).
2. **Who defines the calendar?** `SchoolAcademicTimeline` has no `schoolId` today. Was it meant as a system-wide calendar? The proposal makes years per school, optionally pre-filled from a system calendar template.
3. **Active memberships per user.** Can a user have more than one active membership? Earlier recommendation: students one, teachers several.
4. **Practice from before joining.** Can a school see it? Earlier recommendation: no, unless the student agrees. When and how is guardian consent collected for under-16s?
5. **Invitations only, or also create accounts for people without one?** Earlier recommendation: invitations only.
6. **Files with neither member code nor email.** Many school-made files have only name, birth date and class. Should those rows be rejected, or should the import offer to generate member codes, handed out as claim codes? Recommendation: offer generation as an opt-in. The generated codes then appear in the roster export, so later imports match by code.
7. **Invitation details.** Who can see and print claim codes? (Earlier: school admins, and teachers for their own classes.) How long are invitations valid? (Earlier: 30 days, resendable.)
8. **Row limit per file.** Earlier recommendation: based on the largest expected school, for example 5,000.
9. **Leaving and returning in the same year:** reopen the enrollment, or create a second record?
10. **vnEdu export layout:** an anonymized sample file is needed to build the preset.
11. **Production data:** is there any to keep? If yes, every schema change here is a new Flyway migration instead of part of the baseline.
12. **Where oral English scores are recorded.** Do pilot schools record them as regular assessments (a speaking check during the term), or as part of the mid-term and end-of-term tests? This sets the default assessment type for whole-grade oral assessments (7.7).
13. **`strictness`:** is it planned on the AI service side? If not, remove it (7.3).
14. **School-level scoring rules:** recommendation: none; keep evaluation guards platform-level (7.3).
15. **"Results too high" thresholds:** defaults for the dashboard signal (7.9).
16. **Programs at pilot schools:** which programs exist (lớp chọn, trọng điểm, chuyên…), and does any of them set separate mid-term or end-of-term tests (7.5)?

**Decided during the evaluation discussion (section 7)**
- An assessment policy is the framework level a grade, a program or a group is evaluated at.
- Specialized (chuyên) programs take separate assessments. Advanced programs at regular schools share mid-term and end-of-term tests with the grade by default, with separate tests as an opt-in.
- Groups may have lower or higher targets, for regular and practice tests only.
- Targets are never raised in reaction to results. Planned changes apply only to assessments scheduled after their effective date.
- Frameworks and rubrics are edited and published as whole versions.

---

## 10. Suggested implementation order

Steps 1–4 come from the migration draft's order of work and are outside the scope of this document:
1. The build compiles.
2. Authentication without a school.
3. `users` changes and sign-up.
4. The practice billing port.

Then:

5. **`school_users` target:** role, member code, official name, status including `SUSPENDED`, history with partial unique indexes, authorization on `ACTIVE` memberships only. Several admins, and the registrant claims a school with their own account (2.3, 2.4).
6. **Academic year** (section 6), with programs and class types (7.5): models, schema, the create/activate/close use cases, and class setup.
7. **Framework and rubric whole-version editing** (7.4), including the publishing-order fix and requiring a published rubric for assessments (7.2, item 6). This doesn't depend on steps 5–6, and the framework use cases need rewriting anyway, so it can start now.
8. **Evaluation targets** (7.6, 7.7): reshape the policy, resolve it at scheduling, assessment scope and assessment types, publish checks. Remove school scoring rules and `strictness`.
9. **Invitations and the student roster import** (section 5). The parser and value normalizers can start at any time.
10. **Rollover wizard** (2.2, 6.4); it also copies targets forward. Needed before the first school's first summer.
11. **Code cleanup** (section 3) alongside steps 6–9, since imports need the new natural keys.
12. **Reporting and the rest:** estimated level, statistics split by target, exports by assessment type (7.8), the "results too high" signal (7.9), then quota rules, outcome dashboard, consent and retention (2.7–2.9).
