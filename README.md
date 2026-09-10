# Termux RTL

> A community-focused Termux build with native bidirectional text rendering and Arabic/Persian/Urdu/Hebrew cursive shaping.

[![Build](https://github.com/talbmax10/termux-app-rtl/actions/workflows/debug_build.yml/badge.svg?branch=rtl-native)](https://github.com/talbmax10/termux-app-rtl/actions/workflows/debug_build.yml)
[![License](https://img.shields.io/badge/project--owned%20additions-MIT-blue.svg)](LICENSE-MIT.md)
[![Upstream](https://img.shields.io/badge/upstream-Termux-black.svg)](https://github.com/termux/termux-app)

## لماذا Termux RTL؟

Termux ممتاز لتشغيل Linux على Android، لكن النص العربي في الطرفية يحتاج معالجة خاصة لأن الطرفية تعتمد على ترتيب الخلايا بينما العربية تعتمد على الكتابة ثنائية الاتجاه وربط الحروف.

يهدف هذا المشروع إلى جعل عرض العربية داخل الطرفية طبيعيًا من طبقة العرض نفسها، بدل الاعتماد على حلول خارجية أو إعادة كتابة مخرجات البرامج.

## أبرز المزايا

- **RTL أصلي:** معالجة اتجاه النص العربي/الفارسي/الأردي/العبري داخل مسار العرض.
- **Arabic Cursive Shaping:** ربط الحروف باستخدام محرك النص الأصلي في Android.
- **Mixed BiDi:** دعم النص المختلط عربي + English + numbers + paths + URLs.
- **Monospace Grid:** الحفاظ على محاذاة النص مع خلايا الطرفية.
- **Rendering Cache:** تقليل العمل والتخصيصات المتكررة أثناء تحديث الطرفية.
- **زر العربية:** تبديل واجهة التطبيق إلى العربية من الإعدادات بلمسة واحدة.
- **CI جاهز:** بناء APKs لكل المعماريات مع SHA-256 للتحقق من الملفات.

## مصدر دعم RTL

يعتمد الجزء الأساسي من دعم RTL/cursive shaping على العمل المنشور في:

- Termux PR #5179: https://github.com/termux/termux-app/pull/5179
- Upstream Termux: https://github.com/termux/termux-app

تمت المحافظة على الإسناد للمساهمين الأصليين. راجع `NOTICE` و`LICENSE.md` لمعلومات الترخيص.

## الحالة

**Experimental / Community Build**

المشروع مخصص للاختبار والتطوير قبل اعتباره بديلًا رسميًا لـTermux. لا ندّعي أنه إصدار رسمي من فريق Termux.

## العربية وRTL

راجع:

- [`docs/RTL.md`](docs/RTL.md) — السلوك وحالات الاختبار.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — مسار العرض والتصميم الداخلي.
- [`docs/TESTING.md`](docs/TESTING.md) — اختبار الجهاز وCI.

### زر اللغة العربية

افتح:

**Settings → العربية**

وسيتم تطبيق واجهة عربية باستخدام AndroidX AppCompat locale API. إذا كانت ترجمة عبارة معينة غير موجودة بعد، سيستخدم التطبيق النص الإنجليزي الاحتياطي؛ الترجمة يمكن توسيعها تدريجيًا.

## تنزيل APK

يتم بناء APKs تلقائيًا عبر GitHub Actions عند التحديث على فرع `rtl-native`.

الأفضل لجهاز Android حديث بمعمارية ARM64 هو:

`arm64-v8a` + `apt-android-7`

كما يتم إنشاء Universal وABI variants أخرى للاختبار.

كل Build ينشئ ملف SHA-256 للتحقق من سلامة APK.

> **تنبيه:** توقيع APK التجريبي يعتمد على آلية البناء الخاصة بالمشروع/Termux. لا تخلط APKs أو إضافات Termux الموقعة من مصادر مختلفة. عند الانتقال من F-Droid إلى Build مختلف قد تحتاج إلى إزالة تطبيقات وإضافات Termux القديمة بعد أخذ نسخة احتياطية.

## التثبيت بجانب Termux الرسمي

**غير مدعوم حاليًا بشكل كامل.**

لا يكفي تغيير `applicationId` لإنشاء نسخة جانبية آمنة من Termux، لأن bootstrap و`$PREFIX` وshared user والـplugins مرتبطة ببنية Termux. لذلك لا نعلن دعم Parallel Install حتى يتم تنفيذ واختبار بنية منفصلة بالكامل.

## التوافق

- Android 7+ للإصدار `apt-android-7`.
- Android 5/6 له مسار `apt-android-5` وفق بنية Termux الأصلية.
- يوصى بالاختبار على أجهزة Android الحديثة.

## الأمان

لا تضع مفاتيح API أو Tokens أو شهادات خاصة أو كلمات مرور في GitHub.

راجع [`SECURITY.md`](SECURITY.md) و[`NOTICE`](NOTICE) و[`LICENSE.md`](LICENSE.md).

## الترخيص

كود Termux الأصلي يخضع لترخيصه upstream، وهو **GPLv3-only** وفق `LICENSE.md`، مع الاستثناءات الموضحة هناك.

الملفات والإضافات الأصلية المحددة بوضوح كإضافات مملوكة للمشروع يمكن أن تستخدم MIT وفق [`LICENSE-MIT.md`](LICENSE-MIT.md). هذا الترخيص لا يعيد ترخيص كود Termux أو مساهمات الطرف الثالث.

## المساهمة

راجع [`CONTRIBUTING.md`](CONTRIBUTING.md).

المساهمات مرحب بها خصوصًا في:

- تحسين BiDi.
- Arabic/Persian/Urdu shaping.
- Cursor وselection.
- ANSI styles.
- الاختبارات على أجهزة حقيقية.
- الترجمة العربية.
- الأداء واستهلاك الذاكرة.

## Roadmap

- [x] دمج أساس دعم RTL/cursive shaping.
- [x] توثيق معماري واختبارات RTL.
- [x] زر تبديل الواجهة إلى العربية.
- [x] CI للـAPK وSHA-256.
- [ ] توسيع الترجمة العربية لكل واجهة التطبيق.
- [ ] مجموعة اختبارات BiDi آلية أوسع.
- [ ] اختبارات أداء على شاشات 120Hz.
- [ ] اختبار شامل على أجهزة Samsung الحديثة.
- [ ] تحسين cursor/selection في النص المختلط.
- [ ] دراسة دعم تثبيت منفصل حقيقي دون كسر bootstrap/plugins.

## شكر وتقدير

شكرًا لفريق Termux ولمساهمي المشروع، وبشكل خاص للمساهمين في أعمال RTL التي يعتمد عليها هذا الإصدار.

## Disclaimer

Termux RTL مشروع مجتمعي مستقل، وليس إصدارًا رسميًا أو منتجًا تابعًا لفريق Termux. استخدم إصدارات الاختبار على مسؤوليتك، واحتفظ بنسخة احتياطية من بيئة Termux قبل تغيير مصدر التثبيت.
