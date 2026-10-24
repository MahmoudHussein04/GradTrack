# GradTrack — توثيق المشروع وخطة التنفيذ (v2)

> ⚠️ **تم استبدال هذا الملف** بـ `GradTrack-Graduation-Documentation.md` (الوثيقة الكاملة، وفيها الجدول الزمني المحدث: واجهات أولاً ثم Backend). أُبقي هذا الملف للمرجعية فقط.

> نظام إدارة مشاريع التخرج: **Backend واحد (REST API)** يخدم **Web** و**Android** — بدون iOS.
> المصدر: ملف Figma (13 شاشة، Desktop 1440×1000) + مخططات Use Case للأدوار الثلاثة (Student / Team Leader / Academic Supervisor).
> الفريق: **4 أشخاص** · المدة: 10 أسابيع تنفيذ + أسبوعان Buffer (≤ 12 أسبوعاً ≈ شهرين ونصف).
> التاريخ المقترح للبداية: الأسبوع الذي يبدأ 26 أكتوبر 2026 ← التسليم ~3 يناير 2027 ← نهاية الـ Buffer ~17 يناير 2027.

---

## 1. نظرة عامة

**GradTrack** يربط ثلاثة أدوار في رحلة مشروع التخرج، من تسجيل الفكرة إلى المناقشة:

| الدور | الوصف |
|---|---|
| **Student** | طالب عضو في فريق: يتابع المشروع والمهام المسندة له ويحدّث حالتها |
| **Team Leader** | الطالب الذي أنشأ الفريق: يدير الفريق والمشروع والمقترح والمهام والتسليمات |
| **Academic Supervisor** | يراجع المقترحات والتسليمات ويعطي Feedback |

الهوية البصرية من Figma: خلفية `#F8F4EA`، Sidebar أخضر غامق (~`#26453A`)، أزرار أخضر (~`#3B6B52`)، خط Inter، بطاقات مدورة وStatus pills وStepper وProgress bars.

---

## 2. الصلاحيات (Use Cases المعتمدة)

هذه المخططات هي **المرجع الأعلى** عند أي تعارض مع Figma.

| Use Case | Student | Team Leader | Supervisor |
|---|:-:|:-:|:-:|
| Register Account | ✅ | — | — |
| Verify University ID | ✅ | — | — |
| Log In / Log Out | ✅ | ✅ | ✅ |
| Recover Password | ✅ | ✅ | ✅ |
| View Dashboard | ✅ | ✅ | ✅ |
| Manage Profile | ✅ | ✅ | ✅ |
| View Notifications | ✅ | ✅ | ✅ |
| View Team Details | ✅ | ✅ | — |
| View Invitations / Respond to Invitation | ✅ | — | — |
| Create Team | — | ✅ | — |
| Send Team Invitation | — | ✅ | — |
| Remove Team Member | — | ✅ | — |
| Register Graduation Project | — | ✅ | — |
| Update Project Information | — | ✅ | — |
| Submit Project Proposal | — | ✅ | — |
| Revise and Resubmit Proposal | — | ✅ | — |
| View Proposal Status | ✅ | (ضمن المشروع) | — |
| Create Task / Assign Task | — | ✅ | — |
| Edit or Cancel Task | — | ✅ | — |
| View Assigned Tasks | ✅ | ✅ | — |
| View Task Details | ✅ | ✅ | — |
| Update Own Task Status | ✅ | ✅ | — |
| View Project Progress | ✅ | ✅ | ✅ |
| Submit Project Deliverable | — | ✅ | — |
| View Submission History | ✅ | ✅ | ✅ |
| View Feedback | ✅ | ✅ | — |
| View Project Details | — | ✅ | ✅ |
| Review Project Proposal | — | — | ✅ |
| View Submission Details | — | ✅ | ✅ |
| Review Project Deliverable | — | — | ✅ |
| Provide Feedback | — | — | ✅ |

> ملاحظة: العلامة «(ضمن المشروع)» و«—» تعني أن الـ Use Case غير مرسوم للدور في المخطط. إن كان الغياب مقصوداً فهو القرار؛ وإلا عدّلوا المخطط. القائد يملك عملياً كل ما يملكه الطالب في هذه الميزات، فاعتبرتُه كذلك.

---

## 3. قرارات يجب حسمها الآن (تعارضات بين Figma والـ Use Cases)

| # | التعارض | الاقتراح المعتمد في هذه الخطة |
|---|---|---|
| 1 | Figma فيه Create account لـ Team Leader وللـ Supervisor، والـ Use Case يسمح بالتسجيل للطالب فقط | **التسجيل للطلاب فقط.** حسابات المشرفين تُنشأ بـ Seed/Script. شاشة Create account في Figma = تسجيل طالب |
| 2 | كيف يصبح الطالب Team Leader؟ | **الدور يُحدد لكل فريق، لا على مستوى الحساب:** أي طالب موثّق ينفذ Create Team فيصبح Leader لفريقه. (المتعارف أن Team Leader Use Case يملك Create Team) |
| 3 | Verify University ID — مقابل ماذا؟ | جدول `university_students` (ID، الاسم، البريد، القسم) يُستورد من CSV من الجامعة أو يُملأ بالـ Seed؛ التسجيل يفشل إن لم يوجد الـ ID |
| 4 | Figma فيه Milestone Management للمشرف، ولا Use Case له | **حذفها من النسخة الأولى.** المراحل الخمس ثابتة (Proposal, Design, Development, Testing, Defense) بمواعيد تُضبط من Config/Seed للفصل |
| 5 | عدد المراحل 6 في شاشة 04 و5 في شاشة 06 | 5 مراحل، والتقدم يُحسب تلقائياً من المراحل المعتمدة |
| 6 | اسم المشرف يختلف بين الشاشات | بيانات مثال فقط؛ الاسم يأتي من حقل المشرف في المشروع |
| 7 | من يعيّن المشرف على المشروع؟ | عند Register Project يختار القائد المشرف من قائمة المشرفين (أو يُسند بالـ Seed) |
| 8 | حد أقصى لأعضاء الفريق (Figma: 4/4) | قيمة قابلة للضبط، الافتراضي 4 |
| 9 | لا Admin | غير مطلوب في النسخة الأولى (Seed بدله) |

---

## 4. تحليل الشاشات (13 شاشة Figma)

| # | الشاشة | الدور | الوظائف | Use Cases المقابلة |
|---|---|---|---|---|
| 01 | Login & Role Selection | الكل | Login، Forgot password، Create account | Log In, Recover Password, Register |
| 02 | Student Dashboard | Student/Leader | 4 بطاقات (تقدم، مواعيد، Feedback، فريق)، مشروعي، Deadlines، Activity | View Dashboard |
| 03 | Register Project | Leader | Wizard (Info ← Members ← Review)، رفع PDF | Register Project, Submit Proposal |
| 04 | Project Details & Progress | Student/Leader | تقدم، Milestones، Feedback، آخر التسليمات | View Progress, View Feedback |
| 05 | Supervisor Dashboard | Supervisor | إحصائيات، Pending Proposals، Deadlines، Activity | View Dashboard |
| 06 | Milestones & Submissions | Student/Leader | Timeline من 5 مراحل، جدول Deliverables، Upload | Submit Deliverable, Submission History |
| 07 | Team Management | Leader | أعضاء، Invite member، تفاصيل الفريق | Create Team, Invite, Remove, View Team |
| 08 | Proposal Status | Leader/Student | حالة المقترح + Review timeline + Feedback | View Proposal Status |
| 09 | Proposal Review | Supervisor | تفاصيل + Tabs + Approve / Request Changes / Reject + Feedback | Review Proposal, Provide Feedback |
| 10 | Submission Review | Supervisor | جدول التسليمات، فتح التسليم | View Submission Details, Review Deliverable |
| 11 | Tasks | Student/Leader | Task board، Add task (للقائد فقط) | Create/Assign/Edit/Cancel Task, Update Status |
| 12 | Notifications | الكل | قائمة مصنفة + Mark all as read | View Notifications |
| 13 | Profile & Settings | الكل | بيانات شخصية + تغيير كلمة المرور | Manage Profile |

**شاشات ناقصة يجب تصميمها (أسبوع 1، يتولاها شخص واحد):**
1. Register Account + Verify University ID، وReset Password.
2. Create Team، وInvitations (قائمة + Accept/Decline) للطالب.
3. Add/Edit/Cancel Task (modal)، وTask Details.
4. Upload Deliverable (modal)، وSubmission Details (للقائد والمشرف) مع Review + Feedback.
5. Revise & Resubmit Proposal.
6. Empty / Loading / Error states.
7. **تخطيط Android** لكل الشاشات (Figma كله Desktop): Bottom Navigation بدل Sidebar، الجداول تتحول إلى Cards، الـ Wizard خطوة بخطوة.

---

## 5. الرحلات الأساسية

1. **الطالب** يسجّل (Register) ← يتحقق من الـ University ID ← يدخل.
2. **القائد** ينشئ الفريق ← يرسل دعوات ← **الطلاب** يقبلون من View Invitations.
3. القائد يسجل المشروع (يختار المشرف) ← يرسل المقترح (Submit).
4. **المشرف** يراجع: Approve / Request Changes / Reject + Feedback ← إشعار للفريق.
5. عند Request Changes: القائد **Revise and Resubmit**.
6. بعد الموافقة: القائد ينشئ المهام ويسندها ← كل عضو يحدّث حالة مهمته.
7. القائد يرفع Deliverables للمراحل ← المشرف يراجع ويكتب Feedback ← يتحدث التقدم.
8. إشعارات في كل خطوة حتى المناقشة.

---

## 6. النطاق

**MVP (إلزامي، ينتهي أسبوع 9):** كل Use Cases في القسم 2 + Notifications داخل التطبيق + Push على Android.

**Nice-to-have (إن بقي وقت):** Email notifications، Dark mode، Export تقرير PDF، بحث وفلترة، تعليقات على المهام.

**خارج النطاق:** iOS، Admin panel، Chat مباشر، SSO مع نظام الجامعة، Analytics متقدمة.

---

## 7. المعمارية التقنية

```
        ┌────────────┐   ┌──────────────┐
        │  Web (SPA) │   │ Android App  │
        └─────┬──────┘   └──────┬───────┘
              └────── HTTPS / JSON (REST + JWT) ──────┐
                                              ┌───────▼────────┐
                                              │  Backend API   │
                                              │ (واحد للجميع)  │
                                              └─┬─────┬─────┬──┘
                                    PostgreSQL ◄┘     │     └► FCM (Push Android)
                                           File Storage (S3/MinIO/Disk)
```

| الطبقة | الاقتراح | ملاحظة |
|---|---|---|
| Backend | **NestJS (TypeScript) + PostgreSQL + Prisma** | أو Spring Boot / Laravel حسب خبرة من سيبني الـ Backend؛ المهم عقد API واحد (OpenAPI/Swagger) |
| Auth | JWT (access + refresh)، bcrypt، Rate limiting، RBAC عبر Guards | الدور في الفريق يُفحص من جدول العضوية |
| Files | رفع PDF/ZIP/صور ≤ 25MB، Storage منفصل عن DB | فحص النوع والحجم في الخادم |
| Push | Firebase Cloud Messaging لـ Android فقط | الويب: In-app notifications (+ Polling كل 30–60 ثانية بدل WebSockets) |
| Web | **React + Vite + TypeScript + Tailwind** | يطابق Figma بسهولة |
| Android | **Kotlin + Jetpack Compose** (MVVM + Retrofit) | بدل ذلك: Flutter يغطي Web + Android بقاعدة كود واحدة ويحرر شخصاً واحداً، لكنه يفرض نفس التقنية على الاثنين |
| Deploy | Docker Compose على VPS واحد (API + Postgres + Nginx) أو Railway/Render | |
| Docs/Contract | OpenAPI يُكتب **في الأسبوع الأول** ويعتمد عليه الجميع | |

> إن كان لدى الفريق خبرة Flutter: استبدل Web + Android بتطبيق Flutter واحد ويصير شخصان على الـ Backend أو على التصميم والجودة. القرار يوفّر ~2 أسبوع تقريباً من جهد الواجهات.

### وحدات الـ API (Endpoints الرئيسية)
| الوحدة | أمثلة |
|---|---|
| Auth | `POST /auth/register`, `/auth/verify-university-id`, `/auth/login`, `/auth/logout`, `/auth/forgot-password`, `/auth/reset-password` |
| Users | `GET/PATCH /me`, `PATCH /me/password` |
| Teams | `POST /teams`, `GET /teams/me`, `POST /teams/:id/invitations`, `DELETE /teams/:id/members/:userId` |
| Invitations | `GET /invitations`, `POST /invitations/:id/accept`, `/decline` |
| Projects | `POST /projects`, `PATCH /projects/:id`, `POST /projects/:id/submit`, `POST /projects/:id/resubmit`, `GET /projects/:id` |
| Reviews (مشرف) | `GET /supervisor/proposals`, `POST /projects/:id/review` (approve/changes/reject + comment) |
| Milestones | `GET /projects/:id/milestones`, `GET /projects/:id/progress` |
| Submissions | `POST /milestones/:id/submissions`, `GET /projects/:id/submissions`, `GET /submissions/:id`, `POST /submissions/:id/review` |
| Feedback | `GET /projects/:id/feedback`, `POST /feedback` |
| Tasks | `POST /projects/:id/tasks`, `PATCH /tasks/:id` (قائد: تعديل/إلغاء/إسناد)، `PATCH /tasks/:id/status` (المسؤول عنها) |
| Dashboard | `GET /dashboard` (يتغير حسب الدور) |
| Notifications | `GET /notifications`, `POST /notifications/read-all`, `POST /devices` (FCM token) |

---

## 8. نموذج البيانات

```
university_students (university_id PK, full_name, email, department)
users        (id, university_id?, email, password_hash, full_name, department, academic_year,
              system_role[student|supervisor], is_verified)
teams        (id, name, leader_id → users, max_members, created_at)
team_members (team_id, user_id, role[leader|member], status[active|removed])
invitations  (id, team_id, invited_user_id, status[pending|accepted|declined|cancelled], created_at)
projects     (id, team_id, supervisor_id, title, category, problem_statement, objectives,
              technologies[], duration_months, semester, proposal_file_url,
              status[draft|submitted|under_review|changes_requested|approved|rejected|completed],
              progress_pct, submitted_at)
proposal_reviews (id, project_id, supervisor_id, decision[approve|changes|reject], comment, created_at)
milestones   (id, project_id, stage[proposal|design|development|testing|defense], due_date,
              status[upcoming|in_progress|completed], order)
submissions  (id, milestone_id, project_id, submitted_by, file_url, file_name, type,
              status[review|needs_revision|approved], reviewer_id, submitted_at)
feedback     (id, project_id, submission_id?, author_id, body, reply_to?, created_at)
tasks        (id, project_id, title, description, assignee_id, status[todo|in_progress|done|cancelled],
              due_date, created_by)
notifications(id, user_id, type[proposal|feedback|deadline|team|task|submission], title, body,
              ref_type, ref_id, is_read, created_at)
device_tokens(id, user_id, fcm_token)
```

**قواعد حرجة (تُكتب كاختبارات):**
- `Student` لا ينفذ إلا ما في العمود الخاص به؛ الفحص في الـ Backend وليس الواجهة فقط.
- الطالب يرى فريقه ومشروعه فقط؛ المشرف يرى المشاريع المسندة إليه فقط.
- الطالب يغيّر حالة مهامه **المسندة له فقط**؛ القائد يعدل/يلغي أي مهمة في مشروعه.
- التقدم يُحسب من المراحل الخمس (يمكن إضافة وزن للمهام لاحقاً).
- الدعوة تُرسل لطالب موثّق ومنضم إلى فريق واحد فقط كحد أقصى.
- إنشاء Notification عند: دعوة، قرار على المقترح، Feedback، تسليم جديد، مراجعة تسليم، إسناد/تعديل مهمة، اقتراب موعد (Job يومي).

---

## 9. توزيع الفريق (4 أشخاص)

| الشخص | المسؤولية الرئيسية | يشارك أيضاً |
|---|---|---|
| **A — Backend Lead** | Schema، Auth/RBAC، Teams، Projects، المراجعات، Deploy، OpenAPI | يراجع كل PR يمس الصلاحيات |
| **B — Backend + QA** | Submissions/Files، Tasks، Notifications، Dashboard، Jobs، Push، Seed، اختبارات API/RBAC | التوثيق وسكربت الـ Demo |
| **C — Web** | تطبيق React لكل الأدوار | تصاميم Figma الناقصة (Desktop) |
| **D — Android** | تطبيق Kotlin لكل الأدوار | تصاميم Android وFCM |

العمل المتوازي: الأسبوع 1 يُكتب OpenAPI + Mock server (Prism)؛ C وD يبنيان على الـ Mock ولا ينتظران الـ Backend. كل Endpoint يُعلن «جاهز» على بيئة Staging مشتركة.

---

## 10. الخطة الزمنية

| الأسبوع | Backend (A+B) | Web (C) | Android (D) | معيار الإنجاز |
|---|---|---|---|---|
| **1** | Repo + CI + DB + OpenAPI كامل + Seed (مشرفون + طلاب الجامعة) | Design system + Router + Shell + Mock | Theme + Navigation + Mock + FCM setup | **كل الشاشات الناقصة مصممة** + Contract معتمد |
| **2** | Auth: Register، Verify ID، Login/Logout، Recover Password، Profile | Auth screens + Profile | Auth screens + Profile | تسجيل ودخول بالأدوار على Web وAndroid |
| **3** | Teams: Create، Invite، Respond، Remove، Details | Team Management + Invitations | Team Details + Invitations | فريق كامل بدعوات حقيقية |
| **4** | Projects: Register، Update، Submit، Resubmit، Proposal Status، رفع PDF | Register Project Wizard + Proposal Status | نفس الشاشات | القائد يرسل مقترحاً |
| **5** | Supervisor: قائمة المقترحات، Review (3 قرارات)، Feedback، Notifications (أساسي) | Supervisor Dashboard + Proposal Review | Supervisor flows | **دورة المقترح كاملة End-to-end — Demo 1 للمشرف الأكاديمي** |
| **6** | Milestones + Progress، Submit Deliverable، Submission History/Details | Milestones + Upload + Project Details | نفس الشاشات | تسليم يظهر عند المشرف |
| **7** | Review Deliverable + Feedback threads، Tasks (Create/Assign/Edit/Cancel/Status) | Submission Review + Tasks board | Submission Review + Tasks | دورة تسليم ← مراجعة ← إعادة رفع، ومهام تعمل |
| **8** | Dashboards (حسب الدور)، Deadline Job، Push FCM، Notifications كاملة | Dashboards + Notifications | Dashboards + Notifications + Push | **Feature freeze** — كل Use Cases مكتملة |
| **9** | اختبارات RBAC شاملة، Performance، Security (Rate limit، Validation) | Responsive + Empty/Error states + Bug fixes | Polish + اختبار على أجهزة حقيقية + Bug fixes | كل الرحلات تعمل على Web + Android |
| **10** | Deploy Production، Backups، مراقبة، Seed للعرض | Build/Deploy الويب | APK/AAB موقّع | نسخة منشورة وقابلة للتسليم — **Demo 2 + UAT** |
| **11** | Buffer: إصلاحات + Nice-to-have | Buffer | Buffer | |
| **12** | توثيق (README، ER، OpenAPI، User guide)، تقرير، Slides، Dry-run المناقشة | | | جاهزون للمناقشة |

### Milestones
- نهاية أسبوع 1: Contract + تصاميم كاملة.
- نهاية أسبوع 5: أول دورة كاملة (مقترح ← قرار).
- نهاية أسبوع 8: Feature freeze.
- نهاية أسبوع 10: نسخة منشورة + UAT.
- نهاية أسبوع 12: التسليم والمناقشة.

### إدارة المخاطر
| الخطر | الإجراء |
|---|---|
| تأخر الـ Backend | الواجهات تعمل على Mock؛ تُعطى الأولوية للـ Endpoints التي تسد الرحلة الحالية |
| تأخر أسبوع | يستهلك Buffer أسبوع 11 |
| تأخر أكبر | قص بالترتيب: Push ← Deadline Job ← Feedback threads (تعليق واحد فقط) ← Task details المتقدمة |
| ملفات الطلاب غير متاحة للتحقق من الـ ID | Seed بقائمة طلاب تجريبية + CSV import، ولا يتوقف التطوير |
| اختلاف أسلوب العمل بين الأشخاص | Contract OpenAPI ثابت + Code review على كل PR + Daily 15 دقيقة |

---

## 11. الاختبار والجودة

- **API tests** (Jest/Supertest) لكل Use Case، مع **مصفوفة RBAC**: 3 أدوار × كل Endpoint = رفض/قبول كما في القسم 2.
- **Unit tests** لحساب التقدم وتحولات حالة المقترح والتسليم.
- **قائمة اختبار يدوي** لكل رحلة في القسم 5 على Chrome وAndroid حقيقي.
- **UAT** مع مشرف حقيقي في أسبوعي 5 و10.

## 12. معايير القبول النهائية

- [ ] كل Use Cases في القسم 2 تعمل لكل دور على Web وAndroid.
- [ ] الرحلة الكاملة بين 3 حسابات (قائد، طالب، مشرف) من التسجيل إلى مراجعة التسليم.
- [ ] لا يستطيع مستخدم رؤية أو تعديل ما لا يخصه (اختبارات RBAC خضراء).
- [ ] Push يصل على Android خلال ثوانٍ، وIn-app على الويب.
- [ ] نسخة Backend منشورة، وAPK موقّع، وWeb منشور، مع Seed للعرض.
- [ ] التوثيق والتقرير والـ Slides جاهزة.

## 13. أسئلة مفتوحة (تؤثر على الخطة)

1. من يملك خبرة بأي Stack؟ (يحدد Backend وWeb وAndroid، وهل Flutter خيار؟)
2. هل يتوفر ملف الجامعة لطلاب التحقق من الـ University ID أم نستخدم Seed؟
3. هل تأكدتم من قرار أن الـ Leader هو من ينشئ الفريق (القرار 2 في القسم 3)؟
4. هل يختار القائد المشرف أم يُسند المشرف من الجامعة؟
5. هل الواجهة إنجليزية فقط كما في Figma أم تحتاجون العربية/RTL؟
