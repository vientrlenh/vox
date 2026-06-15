# Question Permission Spec

Tai lieu nay mo ta "behavior hien tai trong code" cho toan bo module `QuestionBank`, `QuestionTopic`, `Question`, bao gom:

- role
- owner type
- school ownership
- creator ownership
- scope
- visibility
- status
- CRUD
- review / publish / archive / restore
- GraphQL list/detail
- REST mutation
- nhung cho list va detail khong giong nhau
- nhung cho implementation hien tai con "loose" hoac gap voi nghiep vu mong muon

Tai lieu nay duoc viet theo source code hien tai, khong phai theo nghiep vu ly tuong.

## Source Of Truth

Nhung file chinh dang quyet dinh permission hien tai:

- `src/main/java/com/sep/vox/infrastructure/persistence/query/JpaQuestionBankReadQueryRepository.java`
- `src/main/java/com/sep/vox/infrastructure/persistence/query/JpaQuestionTopicReadQueryRepository.java`
- `src/main/java/com/sep/vox/infrastructure/persistence/query/JpaQuestionReadQueryRepository.java`
- `src/main/java/com/sep/vox/infrastructure/persistence/query/JpaQuestionViewPermissionQuery.java`
- `src/main/java/com/sep/vox/infrastructure/persistence/query/JpaQuestionBankPermissionQuery.java`
- `src/main/java/com/sep/vox/infrastructure/persistence/query/JpaQuestionTopicPermissionQuery.java`
- `src/main/java/com/sep/vox/infrastructure/persistence/query/JpaQuestionPermissionQuery.java`
- cac REST controller trong `src/main/java/com/sep/vox/interfaces/rest/controller`
- cac GraphQL controller trong `src/main/java/com/sep/vox/interfaces/graphql/controller`
- cac use case create/update/delete/review trong `src/main/java/com/sep/vox/application/port/input/usecase`

## Enum Dictionary

### Bank Owner Type

- `SYSTEM`: tai nguyen he thong
- `SCHOOL`: tai nguyen cua truong

### Bank Status

- `DRAFT`
- `PUBLISHED`
- `ARCHIVED`

### Topic Status

- `DRAFT`
- `PUBLISHED`
- `ARCHIVED`

### Question Status

- `DRAFT`
- `SUBMITTED_FOR_REVIEW`
- `REVISION_REQUESTED`
- `APPROVED`
- `REJECTED`
- `PUBLISHED`
- `ARCHIVED`

### Question Scope

- `QUESTION_BANK`
- `CLASSROOM_ASSESSMENT`
- `CENTRAL_EXAM_DRAFT`
- `CENTRAL_EXAM_PAPER`

### Question Visibility

- `BANK_VISIBLE`
- `AUTHOR_ONLY`
- `REVIEWER_ONLY`
- `ASSESSMENT_ONLY`
- `EXAM_PAPER_ONLY`

### Question Type

- `READ_ALOUD`
- `SHORT_ANSWER`
- `LONG_ANSWER`
- `OPINION`
- `DESCRIPTION`

### Question Asset Type

- `AUDIO`
- `IMAGE`
- `VIDEO`
- `TEXT_PASSAGE`

## Role Model

### Real Roles In Security

- `SYSTEM_ADMIN`
- `SCHOOL_ADMIN`
- `TEACHER`
- `STUDENT`

### Derived Teacher Sub-Cases Used In This Doc

Code khong co role rieng cho 3 loai teacher duoi day, nhung permission thuc te tac dong khac nhau:

- `TEACHER_OWNER`: teacher la `createdBy` cua question
- `TEACHER_REVIEWER`: teacher khong phai creator, cung school, va question dang thoa dieu kien review queue
- `TEACHER_UNRELATED`: teacher khong phai creator, khong du dieu kien reviewer hop le

## Global Ownership Rules

### School Context

- `SCHOOL_ADMIN` va `TEACHER` duoc map sang `schoolId` thong qua `SchoolUserRepository.findByUserId(...)`
- neu user khong thuoc truong nao thi nhieu use case se fail

### Resource Ownership Axes

- `QuestionBank.ownerType` quyet dinh tai nguyen thuoc `SYSTEM` hay `SCHOOL`
- `QuestionBank.schoolId` quyet dinh truong so huu khi `ownerType = SCHOOL`
- `Question.createdBy` quyet dinh creator cua question
- `QuestionTopic.createdBy` va `QuestionBank.createdBy` hien tai khong duoc dung cho permission mutation/doc lap

### Important Current Reality

- permission cua `QuestionBank` va `QuestionTopic` chu yeu dua vao `ownerType + schoolId + status`
- permission cua `Question` vua dua vao `ownerType + schoolId`, vua dua vao `createdBy`, `scope`, `visibility`, `status`, `locked`
- `scope` hien tai anh huong rat manh toi `view detail` va `edit content`, nhung khong duoc enforce dong deu tren moi review action

## Endpoint Inventory

## REST Endpoints

### QuestionBank REST

- `POST /api/v1/question-banks/system`
  - `@PreAuthorize("hasRole('SYSTEM_ADMIN')")`
- `POST /api/v1/question-banks/school`
  - `@PreAuthorize("hasRole('SCHOOL_ADMIN')")`
- `PATCH /api/v1/question-banks/{bankId}`
  - `@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")`
- `DELETE /api/v1/question-banks/{bankId}`
  - `@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")`
- `PATCH /api/v1/question-banks/{bankId}/review-actions`
  - `@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")`

### QuestionTopic REST

- `POST /api/v1/question-topics`
  - `@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")`
- `PUT /api/v1/question-topics/{id}`
  - `@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")`
- `DELETE /api/v1/question-topics/{topicId}`
  - `@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")`
- `PATCH /api/v1/question-topics/{topicId}/review-actions`
  - `@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")`

### Question REST

- `POST /api/v1/questions/system`
  - `@PreAuthorize("hasRole('SYSTEM_ADMIN')")`
- `POST /api/v1/questions/school`
  - `@PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")`
- `PUT /api/v1/questions/{questionId}/content`
  - `@PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")`
- `POST /api/v1/questions/{questionId}/assets`
  - `@PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")`
- `PUT /api/v1/questions/{questionId}/assets`
  - `@PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")`
- `DELETE /api/v1/questions/{questionId}/assets`
  - `@PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")`
- `POST /api/v1/questions/{questionId}/evaluation-guide`
  - `@PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")`
- `PUT /api/v1/questions/{questionId}/evaluation-guide`
  - `@PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")`
- `DELETE /api/v1/questions/{questionId}/evaluation-guide`
  - `@PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")`
- `DELETE /api/v1/questions/{questionId}`
  - `@PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")`
- `PATCH /api/v1/questions/{questionId}/review-actions`
  - `@PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")`

## GraphQL Queries

### QuestionBank GraphQL

- `teacherQuestionBanks(page, size)`
- `teacherQuestionBank(id)`
- `schoolQuestionBanks(page, size)`
- `schoolQuestionBank(id)`
- `adminQuestionBanks(page, size)`
- `adminSchoolQuestionBanks(schoolId, page, size)`
- `adminQuestionBank(id)`

### QuestionTopic GraphQL

- `teacherBankTopics(bankId, page, size)`
- `teacherQuestionTopic(id)`
- `teacherTopicQuestions(bankId, topicId, page, size, scope, status, type, keyword)`
- `schoolBankTopics(bankId, page, size)`
- `schoolQuestionTopic(id)`
- `schoolTopicQuestions(bankId, topicId, page, size, scope, status, type, keyword)`
- `adminBankTopics(bankId, page, size, includeArchived)`
- `adminQuestionTopic(id)`
- `adminTopicQuestions(bankId, topicId, page, size, includeArchived, scope, status, type, keyword)`

### Question GraphQL

- `question(id)`
- `teacherMyQuestions(page, size)`
- `teacherQuestions(page, size, scope, status, type, keyword)`
- `teacherReviewQueue(page, size)`
- `schoolQuestions(page, size, scope, status, type, keyword)`
- `schoolReviewQueue(page, size)`
- `adminQuestions(page, size, includeArchived, status, keyword)`
- `adminReviewQueue(page, size)`

### GraphQL Filter Semantics

- `page` mac dinh: `1`
- `size` mac dinh: `20`
- `scope`: so sanh bang chuoi chinh xac voi `Question.scope`
- `status`: so sanh bang chuoi chinh xac voi `Question.status`
- `type`: so sanh bang chuoi chinh xac voi `Question.type`
- `keyword`: `LOWER(questionText) LIKE %keyword% OR LOWER(code) LIKE %keyword%`
- `adminQuestions` hien tai KHONG nhan `scope` va KHONG nhan `type`

## Part A - QuestionBank

## A1. Create QuestionBank

### `POST /api/v1/question-banks/system`

- chi `SYSTEM_ADMIN` goi duoc
- use case:
  - bat buoc `languageId` ton tai va active
  - tao bank voi:
    - `ownerType = SYSTEM`
    - `schoolId = null`
    - `status = DRAFT`
    - `createdBy = currentUser`
    - `updatedBy = currentUser`

### `POST /api/v1/question-banks/school`

- chi `SCHOOL_ADMIN` goi duoc
- use case:
  - current user phai thuoc truong
  - `command.schoolId` phai bang `schoolId` cua current user
  - KHONG co check `languageId` active trong use case nay
  - tao bank voi:
    - `ownerType = SCHOOL`
    - `status = DRAFT`
    - `createdBy = currentUser`
    - `updatedBy = currentUser`

## A2. Read QuestionBank

### Teacher

#### `teacherQuestionBanks(page, size)`

- chi tra ve bank co `status = PUBLISHED`
- teacher thay duoc:
  - bank `SYSTEM`
  - bank `SCHOOL` cung `schoolId` voi teacher

#### `teacherQuestionBank(id)`

- cung rule voi list
- neu bank khong `PUBLISHED` thi teacher detail fail

### School Admin

#### `schoolQuestionBanks(page, size)`

- thay duoc:
  - moi bank `SCHOOL` cua school minh, KHONG phan biet `DRAFT / PUBLISHED / ARCHIVED`
  - bank `SYSTEM` chi khi `status = PUBLISHED`

#### `schoolQuestionBank(id)`

- cung rule voi list

### System Admin

#### `adminQuestionBanks(page, size)`

- thay tat ca bank, moi owner type, moi status

#### `adminSchoolQuestionBanks(schoolId, page, size)`

- thay tat ca bank `SCHOOL` cua school do, moi status

#### `adminQuestionBank(id)`

- thay duoc moi bank theo id

## A3. Update QuestionBank Content

### Endpoint

- `PATCH /api/v1/question-banks/{bankId}`

### Controller Role Gate

- `SYSTEM_ADMIN`
- `SCHOOL_ADMIN`

### Real Permission

- `SYSTEM_ADMIN`:
  - chi duoc update bank `SYSTEM`
  - bank status hien tai phai la `DRAFT` hoac `PUBLISHED`
- `SCHOOL_ADMIN`:
  - chi duoc update bank `SCHOOL` cua school minh
  - bank status hien tai phai la `DRAFT` hoac `PUBLISHED`

### Fields Really Updated

`UpdateQuestionBankUseCase` hien tai CHI update:

- `name`
- `description`
- `updatedAt`
- `updatedBy`

Khong doi status tai endpoint nay.

### Important Note

- request co `isActive`, nhung use case hien tai KHONG dung field nay de doi status
- doi status phai di qua `PATCH /review-actions`

## A4. Review / Status Change For QuestionBank

### Endpoint

- `PATCH /api/v1/question-banks/{bankId}/review-actions`

### Allowed Target Status Values

- `PUBLISHED`
- `ARCHIVED`
- `DRAFT`

### Transition Matrix

| Role | Resource owner | Current status | Target status | Duoc? |
|---|---|---|---|---|
| `SYSTEM_ADMIN` | `SYSTEM` | `DRAFT` | `PUBLISHED` | `YES` |
| `SYSTEM_ADMIN` | `SYSTEM` | `DRAFT` hoac `PUBLISHED` | `ARCHIVED` | `YES` |
| `SYSTEM_ADMIN` | `SYSTEM` | `ARCHIVED` | `DRAFT` | `YES` |
| `SYSTEM_ADMIN` | `SCHOOL` | bat ky | bat ky | `NO` |
| `SCHOOL_ADMIN` | `SCHOOL` cua school minh | `DRAFT` | `PUBLISHED` | `YES` |
| `SCHOOL_ADMIN` | `SCHOOL` cua school minh | `DRAFT` hoac `PUBLISHED` | `ARCHIVED` | `YES` |
| `SCHOOL_ADMIN` | `SCHOOL` cua school minh | `ARCHIVED` | `DRAFT` | `YES` |
| `SCHOOL_ADMIN` | `SYSTEM` hoac school khac | bat ky | bat ky | `NO` |

### Notes

- khong co note/reason trong bank review action
- use case set thang `bank.status = targetStatus`

## A5. Delete QuestionBank

### Endpoint

- `DELETE /api/v1/question-banks/{bankId}`

### Permission

- phai pass `canUpdateBank(...)`
- sau do bank con phai co `status = DRAFT`

### Behavior

- neu bank khong `DRAFT` thi fail
- neu duoc xoa:
  - xoa toan bo question asset trong tung question
  - xoa toan bo evaluation guide
  - xoa toan bo question
  - xoa toan bo topic
  - xoa bank

### Delete Type

- day la hard delete
- KHONG chuyen bank sang `ARCHIVED`

## Part B - QuestionTopic

## B1. Create QuestionTopic

### Endpoint

- `POST /api/v1/question-topics`

### Controller Role Gate

- `SYSTEM_ADMIN`
- `SCHOOL_ADMIN`

### Actual Use Case Behavior

`CreateQuestionTopicUseCase` hien tai:

- KHONG goi `QuestionTopicPermissionQuery.canCreateTopic(...)`
- CHI check `questionBankRepository.existsById(bankId)`
- neu bank ton tai thi tao topic moi voi:
  - `status = DRAFT`
  - `code = normalizeCode(topicName)`
  - `createdBy = currentUser`
  - `updatedBy = currentUser`

### Important Gap

Ve implementation hien tai:

- `SCHOOL_ADMIN` co the tao topic vao bat ky `bankId` ton tai neu biet id
- use case hien tai KHONG verify:
  - bank co thuoc school cua minh khong
  - bank co phai `SYSTEM` hay `SCHOOL` khong
  - bank dang `DRAFT / PUBLISHED / ARCHIVED` gi

Neu muon nghiep vu dung, can dua `canCreateTopic(...)` vao use case hoac enforce o tang khac.

## B2. Read QuestionTopic

### Teacher

#### `teacherBankTopics(bankId, page, size)`

- bank phai:
  - `status = PUBLISHED`
  - `ownerType = SYSTEM`
    - hoac
  - `ownerType = SCHOOL` va `schoolId = teacher.schoolId`
- topic phai `status = PUBLISHED`

#### `teacherQuestionTopic(id)`

- cung rule voi list

### School Admin

#### `schoolBankTopics(bankId, page, size)`

- thay topic neu:
  - bank la `SCHOOL` cua school minh va topic `status <> ARCHIVED`
  - hoac bank la `SYSTEM` va bank/topic deu `PUBLISHED`

#### `schoolQuestionTopic(id)`

- cung rule voi list

### System Admin

#### `adminBankTopics(bankId, page, size, includeArchived)`

- thay moi topic cua bank do
- neu `includeArchived != true` thi loai topic `ARCHIVED`
- use case nay KHONG loc theo bank owner type

#### `adminQuestionTopic(id)`

- thay topic theo id, moi status

## B3. Update QuestionTopic Content

### Endpoint

- `PUT /api/v1/question-topics/{id}`

### Controller Role Gate

- `SYSTEM_ADMIN`
- `SCHOOL_ADMIN`

### Actual Use Case Behavior

`UpdateQuestionTopicUseCase` hien tai:

- KHONG goi `QuestionTopicPermissionQuery.canUpdateTopic(...)`
- CHI check:
  - bank trong request ton tai
  - topic id ton tai
- sau do CHI update:
  - `name`
  - `description`
  - `updatedAt`
  - `updatedBy`

### Important Gaps

- school admin co the update topic bat ky neu biet `topicId` va gui mot `bankId` ton tai
- request co `bankId`, nhung use case KHONG move topic sang bank moi
- ownership/status permission hien tai khong duoc enforce o endpoint update topic

## B4. Review / Status Change For QuestionTopic

### Endpoint

- `PATCH /api/v1/question-topics/{topicId}/review-actions`

### Allowed Target Status Values

- `PUBLISHED`
- `ARCHIVED`
- `DRAFT`

### Transition Matrix

| Role | Resource owner | Bank status | Topic current status | Target status | Duoc? |
|---|---|---|---|---|---|
| `SYSTEM_ADMIN` | bank `SYSTEM` | `DRAFT` hoac `PUBLISHED` | `DRAFT` | `PUBLISHED` | `YES` |
| `SYSTEM_ADMIN` | bank `SYSTEM` | `DRAFT` hoac `PUBLISHED` | `DRAFT` hoac `PUBLISHED` | `ARCHIVED` | `YES` |
| `SYSTEM_ADMIN` | bank `SYSTEM` | `DRAFT` hoac `PUBLISHED` | `ARCHIVED` | `DRAFT` | `YES` |
| `SYSTEM_ADMIN` | bank `SCHOOL` | bat ky | bat ky | bat ky | `NO` |
| `SCHOOL_ADMIN` | bank `SCHOOL` cua school minh | `DRAFT` hoac `PUBLISHED` | `DRAFT` | `PUBLISHED` | `YES` |
| `SCHOOL_ADMIN` | bank `SCHOOL` cua school minh | `DRAFT` hoac `PUBLISHED` | `DRAFT` hoac `PUBLISHED` | `ARCHIVED` | `YES` |
| `SCHOOL_ADMIN` | bank `SCHOOL` cua school minh | `DRAFT` hoac `PUBLISHED` | `ARCHIVED` | `DRAFT` | `YES` |
| `SCHOOL_ADMIN` | bank `SYSTEM` hoac school khac | bat ky | bat ky | bat ky | `NO` |

## B5. Delete QuestionTopic

### Endpoint

- `DELETE /api/v1/question-topics/{topicId}`

### Permission

- phai pass `canUpdateTopic(topicId)`
- sau do topic phai co `status = DRAFT`

### Behavior

- neu topic khong `DRAFT` thi fail
- neu duoc xoa:
  - xoa toan bo asset cua tat ca question ben duoi topic
  - xoa toan bo evaluation guide
  - xoa toan bo question
  - xoa topic

### Delete Type

- hard delete
- KHONG chuyen topic sang `ARCHIVED`

## Part C - Question

## C1. Question Create

## `POST /api/v1/questions/system`

### Controller Role Gate

- chi `SYSTEM_ADMIN`

### Actual Use Case Rules

- current user phai `ACTIVE`
- topic phai ton tai
- topic phai `isActive() = true`
  - nghia la topic phai `PUBLISHED`
- bank cua topic phai ton tai va `ownerType = SYSTEM`
- KHONG check bank status mot cach explicit ngoai viec topic phai published
- `minResponseSeconds <= maxResponseSeconds`

### Data Created

- `status = DRAFT`
- `locked = false`
- `createdBy = currentUser`
- `updatedBy = currentUser`
- `scope`, `visibility`, `type` lay thang tu request

### Important Note

- implementation hien tai cho phep create `SYSTEM` question voi BAT KY `scope` va BAT KY `visibility`
- khong co validation rang buoc combo `scope <-> visibility`

## `POST /api/v1/questions/school`

### Controller Role Gate

- `SCHOOL_ADMIN`
- `TEACHER`

### Actual Use Case Rules

- current user phai `ACTIVE`
- current user phai thuoc 1 school
- topic phai ton tai
- topic phai belong to school cua current user
- bank cua topic phai ton tai
- bank khong duoc `ARCHIVED`
- topic khong duoc `ARCHIVED`
- `minResponseSeconds <= maxResponseSeconds`

### Data Created

- `status = DRAFT`
- `locked = false`
- `createdBy = currentUser`
- `updatedBy = currentUser`
- `scope`, `visibility`, `type` lay thang tu request

### Important Note

- implementation hien tai cho phep `TEACHER` va `SCHOOL_ADMIN` tao school question voi BAT KY `scope` va BAT KY `visibility`
- khong co validation combo `scope <-> visibility`

## C2. Question Detail - `question(id)`

### Important Architecture Note

- GraphQL `question(id)` khong dung `QuestionReadQueryRepository.findVisibleQuestion(...)`
- no dung:
  - `JpaQuestionViewPermissionQuery.canViewQuestionDetail(...)`
  - sau do load thang question tu `QuestionRepository.findById(...)`
- vi vay list rules va detail rules KHONG nhat thiet giong nhau

### Nested Fields

Khi da qua duoc `question(id)`, cac field nested:

- `questionTopic`
- `assets`
- `evaluationGuide`

duoc resolve khong co permission check rieng.

### Detail Rules By Scope

## Scope = `QUESTION_BANK`

### `SYSTEM_ADMIN`

- xem duoc moi question
- khong phan biet:
  - owner type
  - school
  - visibility
  - question status
  - bank/topic archived hay khong

### `SCHOOL_ADMIN`

Duoc xem neu 1 trong 2 dieu kien:

- question thuoc bank `SCHOOL` cua school minh
  - bank `status <> ARCHIVED`
  - topic `status <> ARCHIVED`
  - `visibility <> AUTHOR_ONLY`
  - question status duoc phep ke ca `ARCHIVED`
- hoac question thuoc bank `SYSTEM`
  - bank `PUBLISHED`
  - topic `PUBLISHED`
  - question `PUBLISHED`
  - visibility `BANK_VISIBLE`

### `TEACHER`

Duoc xem neu 1 trong 3 dieu kien:

- la creator cua question
  - bank `status <> ARCHIVED`
  - topic `status <> ARCHIVED`
  - question status bat ky, ke ca `ARCHIVED`
  - visibility bat ky
- hoac question la published public item
  - bank `PUBLISHED`
  - topic `PUBLISHED`
  - question `PUBLISHED`
  - visibility `BANK_VISIBLE`
  - bank la `SYSTEM` hoac school minh
- hoac question dang trong review queue hop le
  - question `SUBMITTED_FOR_REVIEW`
  - visibility `REVIEWER_ONLY`
  - khong phai do teacher do tao
  - bank la `SCHOOL` va cung school
  - bank/topic khong `ARCHIVED`

### `STUDENT`

- khong duoc xem

## Scope = `CLASSROOM_ASSESSMENT`

### `SYSTEM_ADMIN`

- xem duoc moi question

### `SCHOOL_ADMIN`

Duoc xem neu:

- same-school question va `visibility <> AUTHOR_ONLY`
  - KHONG check bank/topic status
  - KHONG check question status
- hoac question la published bank-visible
  - bank `PUBLISHED`
  - topic `PUBLISHED`
  - question `PUBLISHED`
  - visibility `BANK_VISIBLE`
  - bank la `SYSTEM` hoac school minh

### `TEACHER`

Duoc xem neu:

- la creator
  - KHONG check bank/topic status
  - KHONG check question status
  - visibility bat ky
- hoac question la published bank-visible
  - bank `PUBLISHED`
  - topic `PUBLISHED`
  - question `PUBLISHED`
  - visibility `BANK_VISIBLE`
  - bank la `SYSTEM` hoac school minh

### Reviewer Note

- o `CLASSROOM_ASSESSMENT`, teacher reviewer KHONG co shortcut "review queue" rieng
- neu khong la creator, teacher chi xem duoc theo nhanh `published bank-visible`

## Scope = `CENTRAL_EXAM_DRAFT`

### `SYSTEM_ADMIN`

- xem duoc moi question

### `SCHOOL_ADMIN`

Duoc xem neu:

- same-school question va `visibility <> AUTHOR_ONLY`
  - KHONG check bank/topic status
  - KHONG check question status
- hoac question la published bank-visible
  - bank `PUBLISHED`
  - topic `PUBLISHED`
  - question `PUBLISHED`
  - visibility `BANK_VISIBLE`
  - bank la `SYSTEM` hoac school minh

### `TEACHER`

Duoc xem neu:

- la creator
  - KHONG check bank/topic status
  - KHONG check question status
- hoac question la published bank-visible
  - bank `PUBLISHED`
  - topic `PUBLISHED`
  - question `PUBLISHED`
  - visibility `BANK_VISIBLE`
  - bank la `SYSTEM` hoac school minh
- hoac question dang reviewer queue hop le
  - `SUBMITTED_FOR_REVIEW`
  - `REVIEWER_ONLY`
  - khong phai creator
  - same school
  - KHONG can bank/topic published

## Scope = `CENTRAL_EXAM_PAPER`

### `SYSTEM_ADMIN`

- xem duoc moi question

### `SCHOOL_ADMIN`

Duoc xem neu:

- same-school question va `visibility <> AUTHOR_ONLY`
  - KHONG check bank/topic status
  - KHONG check question status
- hoac question la published bank-visible
  - bank `PUBLISHED`
  - topic `PUBLISHED`
  - question `PUBLISHED`
  - visibility `BANK_VISIBLE`
  - bank la `SYSTEM` hoac school minh

### `TEACHER`

Duoc xem neu:

- la creator
- hoac question la published bank-visible
  - bank `PUBLISHED`
  - topic `PUBLISHED`
  - question `PUBLISHED`
  - visibility `BANK_VISIBLE`
  - bank la `SYSTEM` hoac school minh

### Reviewer Note

- khong co nhanh reviewer queue rieng cho `CENTRAL_EXAM_PAPER`

## C3. Question List Queries

## `teacherMyQuestions(page, size)`

- chi loc theo `createdBy = currentUser`
- KHONG loc theo:
  - bank status
  - topic status
  - question status
  - scope
  - visibility

### Result

- list nay co the tra ve item ma sau do `question(id)` van fail
- vi detail cua `QUESTION_BANK` creator van can bank/topic khong `ARCHIVED`

## `teacherQuestions(page, size, scope, status, type, keyword)`

### Base Filter

- bank `status <> ARCHIVED`
- topic `status <> ARCHIVED`

### Teacher Thay Duoc Khi

#### Nhanh 1 - bank/topic da published

- bank `PUBLISHED`
- topic `PUBLISHED`
- bank la `SYSTEM` hoac school minh
- va question thoa 1 trong cac nhom:
  - `BANK_VISIBLE + status = PUBLISHED`
  - `BANK_VISIBLE + status in (DRAFT, SUBMITTED_FOR_REVIEW, REVISION_REQUESTED, APPROVED, REJECTED) + createdBy = currentUser`
  - `AUTHOR_ONLY + createdBy = currentUser`
  - `REVIEWER_ONLY + createdBy <> currentUser + bank owner SCHOOL same school + status = SUBMITTED_FOR_REVIEW`

#### Nhanh 2 - school bank draft

- bank owner `SCHOOL`
- schoolId = school minh
- bank `status = DRAFT`
- va question thoa 1 trong 2 nhom:
  - `createdBy = currentUser`
  - `REVIEWER_ONLY + createdBy <> currentUser + status = SUBMITTED_FOR_REVIEW`

### Important Nuances

- creator KHONG co dieu kien rieng cho `REVIEWER_ONLY` own question trong published bank/topic
- creator KHONG co dieu kien rieng cho `ASSESSMENT_ONLY` own question trong published bank/topic
- creator KHONG co dieu kien rieng cho `EXAM_PAPER_ONLY` own question trong published bank/topic
- neu question own roi roi vao cac combo tren, no co the khong xuat hien trong `teacherQuestions`, nhung van xuat hien trong `teacherMyQuestions`

## `teacherReviewQueue(page, size)`

- query hien tai CHI loc:
  - `status = SUBMITTED_FOR_REVIEW`
  - `visibility = REVIEWER_ONLY`
  - `createdBy <> currentUser`
  - bank owner `SCHOOL`
  - `schoolId = currentUser.schoolId`

### Important Note

- query nay KHONG check bank/topic status

## `schoolQuestions(page, size, scope, status, type, keyword)`

- school admin thay duoc:
  - question school-owned cua school minh khi:
    - bank `status <> ARCHIVED`
    - topic `status <> ARCHIVED`
    - question `status <> ARCHIVED`
    - visibility `<> AUTHOR_ONLY`
  - hoac question system khi:
    - bank `PUBLISHED`
    - topic `PUBLISHED`
    - question `PUBLISHED`
    - visibility `BANK_VISIBLE`

### Important Note

- list nay KHONG tra ve archived same-school question
- nhung `question(id)` co the van xem duoc archived same-school question

## `schoolReviewQueue(page, size)`

- query hien tai CHI loc:
  - `status = SUBMITTED_FOR_REVIEW`
  - `visibility = REVIEWER_ONLY`
  - bank owner `SCHOOL`
  - schoolId = school minh

### Important Note

- KHONG check bank/topic status

## `adminQuestions(page, size, includeArchived, status, keyword)`

- global list, khong loc owner type
- neu `includeArchived != true`:
  - CHI loc bank `status <> ARCHIVED`
  - CHI loc topic `status <> ARCHIVED`
  - KHONG loai question `status = ARCHIVED`
- `status` filter la equality exact
- `keyword` loc tren `questionText` hoac `code`

### Important Note

- `adminQuestions(includeArchived = false)` van co the thay question `ARCHIVED` neu bank/topic chua archived

## `adminReviewQueue(page, size)`

- query hien tai CHI loc `status = SUBMITTED_FOR_REVIEW`
- khong loc:
  - owner type
  - bank status
  - topic status
  - visibility

## Topic-Scoped Question Lists

### `teacherTopicQuestions(...)`

- bank va topic phai `PUBLISHED`
- bank phai la `SYSTEM` hoac same-school bank
- visible predicate:
  - `BANK_VISIBLE + PUBLISHED`
  - `BANK_VISIBLE + non-public status + creator`
  - `AUTHOR_ONLY + creator`
  - `REVIEWER_ONLY + submitted + same school + not creator`

### `schoolTopicQuestions(...)`

- visible predicate:
  - same-school bank + topic `<> ARCHIVED` + question `<> ARCHIVED` + visibility `<> AUTHOR_ONLY`
  - hoac system bank/topic/question published + visibility `BANK_VISIBLE`

### `adminTopicQuestions(...)`

- loc theo `bankId` va `topicId`
- neu `includeArchived != true` chi loai `topic.status = ARCHIVED`
- KHONG loai question archived
- KHONG loai bank archived

## C4. Question Update Content

### Endpoint

- `PUT /api/v1/questions/{questionId}/content`

### Fields Really Updated

- `instructionText`
- `questionText`
- `promptText`
- `preparationText`
- `type`
- `scope`
- `visibility`
- `preparationTimeSeconds`
- `minResponseSeconds`
- `maxResponseSeconds`
- `updatedAt`
- `updatedBy`

### Important Notes

- endpoint nay KHONG doi status
- endpoint nay KHONG doi topic
- endpoint nay KHONG doi `locked`
- endpoint nay CO THE doi `scope` va `visibility`
- school admin co the sua 1 question khi current scope la `QUESTION_BANK`, roi save scope moi sang `CLASSROOM_ASSESSMENT` / `CENTRAL_EXAM_*`
- sau khi doi scope, quyen sua tiep co the thay doi

### Permission Matrix

| Role | Dieu kien can edit content |
|---|---|
| `TEACHER_OWNER` | `createdBy = currentUser`, `locked = false`, bank/topic `<> ARCHIVED`, question status trong `DRAFT / REVISION_REQUESTED / REJECTED` |
| `SCHOOL_ADMIN` | same-school school-owned question, CURRENT `scope = QUESTION_BANK`, `locked = false`, bank/topic `<> ARCHIVED`, question status trong `DRAFT / REVISION_REQUESTED / REJECTED` |
| `SYSTEM_ADMIN` | question thuoc bank `SYSTEM`, `locked = false`, bank/topic `<> ARCHIVED`, question status trong `DRAFT / REVISION_REQUESTED / REJECTED` |
| `TEACHER_REVIEWER` | `NO` |
| `TEACHER_UNRELATED` | `NO` |
| `STUDENT` | `NO` |

## C5. Question Assets

## Create Assets - `POST /api/v1/questions/{questionId}/assets`

### Permission

- dung cung `canEditContent(questionId)`

### Extra Rules

- question phai ton tai
- question hien tai CHUA co asset nao
- neu da co asset thi fail va bat dung endpoint update

### No Extra Status Gate

- ngoai `canEditContent`, create assets KHONG bat question phai la `DRAFT`

## Update Assets - `PUT /api/v1/questions/{questionId}/assets`

### Permission

- dung cung `canEditContent(questionId)`

### Extra Rules

- question phai ton tai
- question phai DA co assets
- `order` trong request phai unique
- `id` cua asset trong request phai unique
- moi `id` khac null phai ton tai trong asset hien co cua question
- asset cu khong duoc gui len se bi xoa
- asset co `id = null` se duoc tao moi

### No Extra Status Gate

- ngoai `canEditContent`, update assets KHONG bat question phai la `DRAFT`

## Delete Assets - `DELETE /api/v1/questions/{questionId}/assets`

### Permission

- dung cung `canEditContent(questionId)`

### Extra Rules

- question phai ton tai
- question phai co `status = DRAFT`
- question phai da co assets

## C6. Question Evaluation Guide

## Create Guide - `POST /api/v1/questions/{questionId}/evaluation-guide`

### Permission

- dung cung `canEditContent(questionId)`

### Extra Rules

- question phai ton tai
- question chua co guide

### No Extra Status Gate

- ngoai `canEditContent`, create guide KHONG bat question phai la `DRAFT`

## Update Guide - `PUT /api/v1/questions/{questionId}/evaluation-guide`

### Permission

- dung cung `canEditContent(questionId)`

### Extra Rules

- question phai ton tai
- question da co guide

### No Extra Status Gate

- ngoai `canEditContent`, update guide KHONG bat question phai la `DRAFT`

## Delete Guide - `DELETE /api/v1/questions/{questionId}/evaluation-guide`

### Permission

- dung cung `canEditContent(questionId)`

### Extra Rules

- question phai ton tai
- question phai co `status = DRAFT`
- question phai da co guide

## C7. Delete Question

### Endpoint

- `DELETE /api/v1/questions/{questionId}`

### Permission

- dung cung `canEditContent(questionId)`

### Behavior

Neu question `status = DRAFT` va KHONG bi question khac reference qua `sourceQuestionId`:

- hard delete question
- hard delete assets
- hard delete evaluation guide
- response `result = HARD_DELETE`

Nguoc lai:

- set `question.status = ARCHIVED`
- response `result = ARCHIVE`

### Important Notes

- delete khong bat buoc question dang `DRAFT`
- neu question dang `REVISION_REQUESTED` hoac `REJECTED`, delete se archive
- neu question dang `DRAFT` nhung da duoc reuse qua `sourceQuestionId`, delete cung se archive

## C8. Question Review / Status Change

### Endpoint

- `PATCH /api/v1/questions/{questionId}/review-actions`

### Allowed Target Status Values

- `SUBMITTED_FOR_REVIEW`
- `REVISION_REQUESTED`
- `APPROVED`
- `REJECTED`
- `PUBLISHED`
- `ARCHIVED`
- `DRAFT`

## Role-Based Transition Matrix

## Teacher Owner (`TEACHER` + `createdBy = currentUser`)

| Current constraints | Target status | Duoc? | Notes |
|---|---|---|---|
| `locked = false`, bank/topic `<> ARCHIVED`, current status trong `DRAFT / REVISION_REQUESTED / REJECTED` | `SUBMITTED_FOR_REVIEW` | `YES` | khong check scope |
| current status `APPROVED`, `locked = false`, bank/topic `<> ARCHIVED` | `PUBLISHED` | `YES` | creator publish duoc |
| current status `<> ARCHIVED` va `<> PUBLISHED` | `ARCHIVED` | `YES` | KHONG check locked, KHONG check bank/topic status, KHONG check scope |
| current status `ARCHIVED`, bank/topic `<> ARCHIVED` | `DRAFT` | `YES` | creator restore duoc |
| current status bat ky | `APPROVED / REJECTED / REVISION_REQUESTED` | `NO`, tru khi teacher do dong thoi la reviewer hop le theo nhanh reviewer | teacher-owner khong auto duyet cau cua minh |

## Teacher Reviewer (`TEACHER` + khong phai creator + same school review queue)

| Current constraints | Target status | Duoc? |
|---|---|---|
| `status = SUBMITTED_FOR_REVIEW`, `visibility = REVIEWER_ONLY`, `createdBy <> currentUser`, bank owner `SCHOOL`, same school | `APPROVED` | `YES` |
| `status = SUBMITTED_FOR_REVIEW`, `visibility = REVIEWER_ONLY`, `createdBy <> currentUser`, bank owner `SCHOOL`, same school | `REJECTED` | `YES` |
| `status = SUBMITTED_FOR_REVIEW`, `visibility = REVIEWER_ONLY`, `createdBy <> currentUser`, bank owner `SCHOOL`, same school | `REVISION_REQUESTED` | `YES` |
| bat ky | `SUBMITTED_FOR_REVIEW / PUBLISHED / ARCHIVED / DRAFT` | `NO` |

### Important Note

- reviewer query hien tai KHONG check bank/topic status

## School Admin

### `SUBMITTED_FOR_REVIEW`

Duoc neu:

- question thuoc bank `SCHOOL`
- `qb.schoolId = current school`
- `locked = false`
- bank/topic `<> ARCHIVED`
- question current status trong `DRAFT / REVISION_REQUESTED / REJECTED`

### `APPROVED / REJECTED / REVISION_REQUESTED`

Duoc neu:

- question thuoc bank `SCHOOL`
- `qb.schoolId = current school`
- bank/topic `<> ARCHIVED`

### Important Consequence

Implementation hien tai KHONG check:

- question current status co phai `SUBMITTED_FOR_REVIEW` hay khong
- question `locked`
- `scope`
- `visibility`

Nghia la school admin hien tai co the day same-school question len `APPROVED`, `REJECTED`, `REVISION_REQUESTED` tu nhieu current status hon nghiep vu thuong muon.

### `PUBLISHED`

Duoc neu:

- question thuoc bank `SCHOOL`
- `qb.schoolId = current school`
- bank/topic `<> ARCHIVED`
- `locked = false`
- question current status = `APPROVED`

### `ARCHIVED`

Duoc neu:

- question thuoc bank `SCHOOL`
- `qb.schoolId = current school`
- question current status `<> ARCHIVED`

### Important Consequence

Implementation hien tai KHONG check:

- bank/topic archived hay khong
- `locked`
- `scope`
- current status co phai `PUBLISHED` hay khong

Nghia la school admin co the archive same-school question rat rong.

### `DRAFT`

Duoc neu:

- question thuoc bank `SCHOOL`
- `qb.schoolId = current school`
- bank/topic `<> ARCHIVED`

### Important Consequence

Implementation hien tai KHONG check current question status = `ARCHIVED`.

Nghia la school admin hien tai co the set same-school question ve `DRAFT` rat rong, khong chi restore archived.

## System Admin

Tat ca nhung action duoi day CHI ap dung voi question thuoc bank `SYSTEM`.

| Current constraints | Target status | Duoc? |
|---|---|---|
| bank/topic `<> ARCHIVED`, `locked = false`, current status trong `DRAFT / REVISION_REQUESTED / REJECTED` | `SUBMITTED_FOR_REVIEW` | `YES` |
| bank/topic `<> ARCHIVED`, `locked = false`, current status = `SUBMITTED_FOR_REVIEW` | `APPROVED` | `YES` |
| bank/topic `<> ARCHIVED`, `locked = false`, current status = `SUBMITTED_FOR_REVIEW` | `REJECTED` | `YES` |
| bank/topic `<> ARCHIVED`, `locked = false`, current status = `SUBMITTED_FOR_REVIEW` | `REVISION_REQUESTED` | `YES` |
| bank/topic `<> ARCHIVED`, `locked = false`, current status = `APPROVED` | `PUBLISHED` | `YES` |
| bank/topic `<> ARCHIVED`, current status trong `DRAFT / REVISION_REQUESTED / REJECTED / APPROVED / PUBLISHED / SUBMITTED_FOR_REVIEW` | `ARCHIVED` | `YES` |
| bank/topic `<> ARCHIVED`, current status = `ARCHIVED` | `DRAFT` | `YES` |

### Important Note

- voi `ARCHIVED` va `DRAFT` restore, system admin ignore `locked`

## C9. Scope-Specific Mutation Reality

### Current Implementation Important Reality

Question mutation hien tai KHONG chia workflow theo scope mot cach day du.

Cu the:

- `TEACHER_OWNER` edit content duoc cho BAT KY current scope, mien la creator va status gate hop le
- `SCHOOL_ADMIN` edit content CHI duoc khi CURRENT scope = `QUESTION_BANK`
- `SYSTEM_ADMIN` edit content khong bi scope gate, chi bi owner + status + locked
- review action cua `TEACHER_OWNER`, `SCHOOL_ADMIN`, `SYSTEM_ADMIN` da so KHONG check scope

### Consequence

- classroom/exam question van co the di qua review / publish / archive / restore neu role va status predicate hien tai cho phep
- neu nghiep vu muon scope nao do khong co workflow review chung, thi implementation hien tai chua enforce dieu do

## C10. Visibility Reality

### Current Implementation Important Reality

`visibility` hien tai anh huong rat manh toi READ, nhung mutation it check visibility.

### Read Summary

- `AUTHOR_ONLY`
  - creator thuong xem duoc
  - school admin thuong KHONG xem duoc same-school question bank author-only
  - school admin o cac scope khac cung bi chan boi helper `visibility <> AUTHOR_ONLY`
- `REVIEWER_ONLY`
  - reviewer queue dung visibility nay
- `BANK_VISIBLE`
  - la che do public de school/teacher khac thay qua list/detail published fallback
- `ASSESSMENT_ONLY`
  - hien tai khong co permission branch rieng
- `EXAM_PAPER_ONLY`
  - hien tai khong co permission branch rieng

### Mutation Summary

- create question cho phep gui bat ky visibility
- update content cho phep doi sang bat ky visibility
- review/status mutation khong dua vao visibility, tru reviewer flow

## C11. Archived Summary

### Question `ARCHIVED`

- `TEACHER_OWNER`
  - detail: duoc neu la own question va bank/topic khong `ARCHIVED` voi `QUESTION_BANK`
  - detail: duoc rat rong neu own question o `CLASSROOM_ASSESSMENT`, `CENTRAL_EXAM_DRAFT`, `CENTRAL_EXAM_PAPER`
  - restore: duoc ve `DRAFT` neu own question, current status = `ARCHIVED`, bank/topic khong `ARCHIVED`
- `SCHOOL_ADMIN`
  - detail:
    - `QUESTION_BANK`: duoc neu same-school, bank/topic khong archived, visibility != author_only
    - scope khac: duoc neu same-school va visibility != author_only, khong can question status hop le
  - restore:
    - implementation hien tai co the set same-school question ve `DRAFT` ma khong bat buoc current status = `ARCHIVED`
- `SYSTEM_ADMIN`
  - detail: duoc tren moi question
  - restore:
    - chi voi question thuoc bank `SYSTEM`
    - current status phai la `ARCHIVED`
- `TEACHER_REVIEWER` / `TEACHER_UNRELATED`
  - khong co quyen archived rieng

### Bank / Topic `ARCHIVED`

- bank/topic archived se chan nhieu list query cho teacher/school admin
- system admin list co the van thay archived bank/topic
- question detail va mutation co mot so nhanh van khong dong deu check bank/topic archived

## C12. Current Mismatches / Gaps To Know

### 1. List va detail khong dong nhat

- `teacherMyQuestions` la list raw theo creator, co the thay item ma `question(id)` van fail
- `schoolQuestions` loai question archived, nhung `question(id)` co the van cho school admin xem archived same-school question
- `adminQuestions(includeArchived = false)` van co the tra ve question archived neu bank/topic chua archived

### 2. Topic create/update hien tai dang long permission

- `POST /api/v1/question-topics` hien tai khong verify ownership/status qua `QuestionTopicPermissionQuery`
- `PUT /api/v1/question-topics/{id}` hien tai cung khong verify ownership/status qua `QuestionTopicPermissionQuery`

### 3. School admin question review flow hien tai rat rong

- school admin approve/reject/revision requested khong bat current status = `SUBMITTED_FOR_REVIEW`
- school admin restore `DRAFT` khong bat current status = `ARCHIVED`
- school admin archive khong check `locked`

### 4. Question mutation khong enforce combo scope-visibility

- create / update content co the tao cac combo scope-visibility ma nghiep vu co the khong mong muon

### 5. Scope workflow chua tach rieng that su

- classroom / central draft / central paper van dung chung nhieu review rule cua question workflow

## C13. Debug Checklist For 1 Case Cu The

Khi debug 1 case "tai sao user A xem duoc / khong xem duoc / sua duoc / khong sua duoc", nen di theo thu tu nay:

1. Xac dinh endpoint dang goi la list, detail hay mutation.
2. Xac dinh role thuc te cua user: `SYSTEM_ADMIN`, `SCHOOL_ADMIN`, `TEACHER`, hay `STUDENT`.
3. Neu la `TEACHER`, xac dinh teacher dang o vai tro nao trong case do:
   - `TEACHER_OWNER`
   - `TEACHER_REVIEWER`
   - `TEACHER_UNRELATED`
4. Xac dinh `QuestionBank.ownerType` la `SYSTEM` hay `SCHOOL`.
5. Neu la school resource, xac dinh `qb.schoolId` co trung `schoolId` cua current user khong.
6. Xac dinh day la flow nao:
   - bank-level
   - topic-level
   - question-level
   - asset / evaluation-guide
7. O question-level, phai ghi du 4 truong:
   - `scope`
   - `visibility`
   - `status`
   - `createdBy`
8. Kiem tra them:
   - `locked`
   - `bank.status`
   - `topic.status`
9. Neu la list GraphQL, doi chieu voi `JpaQuestionReadQueryRepository`, KHONG duoc suy tu detail.
10. Neu la `question(id)`, doi chieu voi `JpaQuestionViewPermissionQuery`, KHONG duoc suy tu list.
11. Neu la mutation, doi chieu voi:
   - `JpaQuestionPermissionQuery`
   - use case create/update/delete/review tuong ung
12. Neu thay "list thay duoc nhung detail fail", uu tien nghi ngay den:
   - `teacherMyQuestions`
   - `schoolQuestions`
   - `adminQuestions(includeArchived = false)`
13. Neu thay "topic create/update lam duoc du khong dung owner", uu tien nghi ngay den gap o:
   - `CreateQuestionTopicUseCase`
   - `UpdateQuestionTopicUseCase`
14. Neu thay "school admin doi status qua de", uu tien nghi ngay den `ReviewQuestionUseCase` + `JpaQuestionPermissionQuery` cho nhanh school admin.

## Appendix - Quick Read Tables

## Who Can Create What

| Resource | SYSTEM_ADMIN | SCHOOL_ADMIN | TEACHER |
|---|---|---|---|
| System bank | `YES` | `NO` | `NO` |
| School bank | `NO` | `YES` | `NO` |
| Topic | `YES` | `YES` | `NO` |
| System question | `YES` | `NO` | `NO` |
| School question | `NO` | `YES` | `YES` |
| Question assets | theo `canEditContent` | theo `canEditContent` | theo `canEditContent` |
| Evaluation guide | theo `canEditContent` | theo `canEditContent` | theo `canEditContent` |

## Who Can Edit Question Content

| Role flavor | Duoc sua content? |
|---|---|
| `SYSTEM_ADMIN` | chi system question, status `DRAFT/REVISION_REQUESTED/REJECTED`, unlocked |
| `SCHOOL_ADMIN` | chi same-school question co CURRENT scope `QUESTION_BANK`, status `DRAFT/REVISION_REQUESTED/REJECTED`, unlocked |
| `TEACHER_OWNER` | own question, status `DRAFT/REVISION_REQUESTED/REJECTED`, unlocked |
| `TEACHER_REVIEWER` | `NO` |
| `TEACHER_UNRELATED` | `NO` |

## Who Can Review Question

| Action | SYSTEM_ADMIN | SCHOOL_ADMIN | TEACHER_OWNER | TEACHER_REVIEWER | TEACHER_UNRELATED |
|---|---|---|---|---|---|
| Submit for review | system question dung status | same-school school question dung status | own question dung status | `NO` | `NO` |
| Approve | system submitted question | same-school school question, implementation rong | `NO` | same-school reviewer-only submitted question | `NO` |
| Reject | system submitted question | same-school school question, implementation rong | `NO` | same-school reviewer-only submitted question | `NO` |
| Revision requested | system submitted question | same-school school question, implementation rong | `NO` | same-school reviewer-only submitted question | `NO` |
| Publish | system approved question | same-school approved school question | own approved question | `NO` | `NO` |
| Archive | system workflow question | same-school school question, implementation rong | own non-published non-archived question | `NO` | `NO` |
| Restore to draft | system archived question | same-school school question, implementation rong | own archived question | `NO` | `NO` |
