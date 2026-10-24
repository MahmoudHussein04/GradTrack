# GradTrack — نظرة سريعة للفريق

> الوثيقة الكاملة (المتطلبات، التحليل، التصميم، الاختبار): `GradTrack-Graduation-Documentation.md`

## ما هو المشروع؟
نظام لإدارة مشاريع التخرج: الطلاب والمشرف يتابعون المشروع من الفكرة حتى المناقشة.
**المنصات:** Web + Android (بدون iOS) · **Backend واحد** · **المدة:** 12 أسبوعاً

## المستخدمون
| الدور | ماذا يفعل؟ |
|---|---|
| **Student** | يسجّل، يقبل الدعوات، يتابع مهامه ويحدّث حالتها، يشوف التقدم والـ Feedback |
| **Team Leader** | ينشئ الفريق، يدعو، يسجل المشروع ويرسل المقترح، ينشئ المهام، يرفع التسليمات |
| **Supervisor** | يراجع المقترح (موافقة/تعديل/رفض) والتسليمات، ويكتب Feedback |

## كيف نعمل؟
**نبدأ بالواجهات على بيانات تجريبية، ثم نبني الـ Backend أربعتنا معاً، ثم ندمج.**

| العضو | المنصة | في مرحلة الواجهات |
|---|---|---|
| Malak | Web | Auth، Profile، Team، Invitations، Register Project، Proposal Status |
| Wahba | Web | Dashboards، Supervisor flows، Milestones، Submissions، Tasks، Notifications |
| Mahmoud | Android | نفس مهام Malak |
| Ibrahim | Android | نفس مهام Wahba |

**الـ Backend (أسبوع 6–9) — زوجان:**
- Malak + Mahmoud: Auth، Users، Teams، Invitations، Tasks، Notifications + FCM
- Wahba + Ibrahim: Projects، Reviews، Milestones، Submissions، Feedback، Dashboard، Deploy

## الجدول الزمني
| الأسبوع | المرحلة | المخرج |
|---|---|---|
| 1 | تأسيس: التصاميم الناقصة + **عقد الـ API (OpenAPI)** + Setup | تصاميم كاملة وعقد معتمد |
| 2 | واجهات: Auth، Profile، Team، Invitations | رحلة التسجيل والفريق |
| 3 | واجهات: Project، Proposal، مراجعة المشرف | **Demo 1 للمشرف الأكاديمي** |
| 4 | واجهات: Milestones، Submissions، Tasks | رحلة التسليم والمهام |
| 5 | واجهات: Dashboards، Notifications، تنسيق Web/Android | **Frontend Freeze** |
| 6 | Backend: Setup، Auth، Users، Teams | أول ربط حقيقي |
| 7 | Backend: Projects، Reviews، Milestones | دورة المقترح الحقيقية (Demo 2) |
| 8 | Backend: Submissions، Feedback، Tasks | دورة التسليم والمهام |
| 9 | Backend: Dashboard، Notifications، FCM | **Backend Freeze** |
| 10 | دمج كامل وإصلاح أخطاء | كل Use Cases تعمل |
| 11 | اختبار + أمان + نشر + تجربة مع مشرف حقيقي | نسخة منشورة وAPK |
| 12 | Buffer + توثيق + بروفة المناقشة | جاهزون |

## قواعد مهمة
- لا تربط الشاشات بالـ Mock مباشرة: استخدموا طبقة Repository/API Client لنبدّلها لاحقاً بسهولة.
- أي تغيير في عقد الـ API يمر بموافقة الأربعة.
- كل تغيير عبر Pull Request ويراجعه شخص آخر.
- اجتماع يومي 15 دقيقة، ومراجعة أسبوعية.
- لو تأخرنا نقطع: Push ← تنبيهات المواعيد ← فلاتر الجداول. **الرحلات الأساسية لا تُقص.**
