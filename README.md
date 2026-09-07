# Air Controle

Hands-free Android phone control via camera hand gestures.  
تحكم بالهاتف الأندرويد دون لمس عبر إيماءات اليد والكاميرا.

**App / project name:** Air Controle  
**Product family:** AIR OS  
**Package:** `com.mulkallah.aircontrole`  
**Tagline:** Your hands are your controller / يداك هما جهاز التحكم.

Default language is **Arabic** with full **RTL**. English is a first-class second locale.

---

## English

### What this MVP does

1. Onboarding: Welcome → Camera → Accessibility → Overlay → Notifications → Ready
2. Home: Quick Access (Instagram, TikTok, YouTube, WhatsApp, Chrome + Add app) and an Air Control ON/OFF toggle with live state
3. Gesture state machine: `IDLE → HAND_DETECTED → TRACKING → GESTURE_RECOGNIZED → ACTION → COOLDOWN`
4. White fingertip overlay cursor with a pulse when a gesture is recognized
5. Foreground service so Air Control can stay on after you leave the app (within honest Android limits)
6. Accessibility actions: Home, Back, scroll, click at the cursor, open Quick Access apps
7. Settings stub: language, cursor, kill switch, one-time purchase note **29.99 SAR** (no billing yet)
8. Gesture training / full customization: “later” stub screens only

### Gestures

| Gesture | Pose / motion | Action |
| --- | --- | --- |
| Point + hold | Index extended | Move the white cursor |
| Click | Pinch thumb to index | Tap at the cursor |
| Scroll up / down | Point + fast vertical move | Scroll |
| Swipe left / right | Point + fast horizontal move | Back / Home shortcut |
| Palm pause | Open palm held | Pause / resume actions |
| Fist back | Closed fist held | System Back |
| Peace home | Index + middle held | System Home |

### Modules

| Module | Role |
| --- | --- |
| `:app` | Compose UI, onboarding, Home, Settings |
| `:core` | Models, DataStore, locale, process bridge |
| `:gestures` | Classifier + state machine |
| `:camera` | CameraX + MediaPipe Hands |
| `:overlay` | Pass-through white cursor window |
| `:accessibility` | Accessibility Service |
| `:control` | Foreground service that keeps Air Control alive |

### Build and run

Requirements:

- Android Studio Ladybug / Koala or newer, or command-line JDK 17+
- Android SDK Platform 35 and Build-Tools 35
- A physical device is strongly recommended (front camera + Accessibility)

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew :gestures:test
```

Install the debug APK from `app/build/outputs/apk/debug/` or use **Run** in Android Studio. For sideload testing, prefer the **release-signed** APK from `app/build/outputs/apk/release/` (see signing below).

Open the project as **Air Controle** (`settings.gradle.kts` `rootProject.name`).

### Release signing (DEV/TEST)

The keystore is **not** in git. Generate a local upload key, then build release:

```bash
cp keystore.properties.example keystore.properties
./scripts/generate-upload-keystore.sh
./gradlew assembleRelease
```

`aircontrole-upload.jks` and `keystore.properties` are gitignored. Treat this key as **DEV/TEST only** — replace it before any Play Console production upload (Play App Signing).

### Play Protect

A release-signed APK is less likely to trip Play Protect than a debug-signed sideload, but **Play Protect may still warn** for apps that use Accessibility and overlay, especially when installed outside Play. That is not fixed by turning Play Protect off.

The durable path is **Play Console → Internal testing** (or another Play track) so Protect sees a Play-distributed signing identity. Uninstall any previous debug-signed Air Controle build before installing a release-signed APK — Android will reject the upgrade (different signature).

### Permissions

| Permission | Why |
| --- | --- |
| Camera | Hand landmarks for the cursor and gestures. Processed on-device. |
| Accessibility | Home, Back, scroll, tap at the cursor. You must enable it in system settings. |
| Display over other apps | White cursor overlay. Touches pass through to the app underneath. |
| Notifications | Required so the foreground service can keep running after you leave Air Controle. |

Air Controle never promises to read your conversations or upload the camera feed.

### Android limits (read this)

On **stock Android** no third-party app can:

- Fully shut down or reboot the phone
- Survive every OEM battery / “optimization” killer
- Inject input without an Accessibility Service the user turned on
- Draw over other apps without the overlay permission
- Keep the camera running forever if the system reclaims resources

The persistent notification is the honest way Air Control stays on. The **kill switch** in Settings stops Air Control, hides the cursor, and releases the camera. It does **not** force-close other apps or power the device off.

If a manufacturer kills background camera work, turn Air Control on again from Home. That is an Android limit, not a missing button.

### Stack

Kotlin, Jetpack Compose, Material 3, CameraX, MediaPipe Hands (`tasks-vision`), DataStore, minSdk 26.

---

## العربية

### ماذا يفعل هذا الإصدار الأول

1. تهيئة: ترحيب → كاميرا → إمكانية الوصول → الطبقة → الإشعارات → جاهز
2. الرئيسية: وصول سريع (إنستغرام، تيك توك، يوتيوب، واتساب، كروم + إضافة تطبيق) ومفتاح Air Control مع حالة واضحة
3. آلة حالات الإيماءات: `IDLE → HAND_DETECTED → TRACKING → GESTURE_RECOGNIZED → ACTION → COOLDOWN`
4. مؤشر أبيض يتابع طرف الإصبع مع نبضة عند رصد إيماءة
5. خدمة أمامية لتبقى Air Control بعد مغادرة التطبيق (ضمن حدود أندرويد الصريحة)
6. إمكانية الوصول: الرئيسية، رجوع، تمرير، نقر عند المؤشر، فتح تطبيقات الوصول السريع
7. إعدادات أولية: اللغة، المؤشر، قاطع الطوارئ، ملاحظة شراء لمرة واحدة **29.99 ر.س** (بدون فوترة حقيقية بعد)
8. تدريب الإيماءات / التخصيص الكامل: شاشات «لاحقًا» فقط

### الإيماءات

| الإيماءة | الوضعية | الإجراء |
| --- | --- | --- |
| إشارة + تثبيت | السبابة ممدودة | تحريك المؤشر الأبيض |
| نقر | إغلاق الإبهام والسبابة | ضغط عند المؤشر |
| تمرير لأعلى / لأسفل | إشارة + حركة رأسية سريعة | تمرير |
| سحب يسار / يمين | إشارة + حركة أفقية سريعة | اختصار رجوع / الرئيسية |
| إيقاف بالكف | كف مفتوح ثابت | إيقاف / استئناف |
| رجوع بالقبضة | قبضة ثابتة | زر الرجوع |
| الرئيسية بعلامة النصر | السبابة والوسطى | زر الرئيسية |

### البناء والتشغيل

المتطلبات: Android Studio حديث أو JDK 17+، منصة SDK 35، وجهاز حقيقي مفضّل.

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew :gestures:test
```

اسم المشروع في Gradle هو **Air Controle**. اللغة الافتراضية العربية مع اتجاه من اليمين إلى اليسار.

### توقيع الإصدار (تطوير/اختبار)

ملف المفاتيح **ليس** في git. أنشئ مفتاحًا محليًا ثم ابنِ نسخة الإصدار:

```bash
cp keystore.properties.example keystore.properties
./scripts/generate-upload-keystore.sh
./gradlew assembleRelease
```

`aircontrole-upload.jks` و`keystore.properties` مستبعدان من git. هذا المفتاح **للتطوير/الاختبار فقط** — استبدله قبل الرفع الإنتاجي إلى Play.

### حماية Google Play

توقيع الإصدار يقلّل احتمال حظر Play Protect مقارنة بتوقيع debug، لكن **قد يظهر تحذير بعد** لأن التطبيق يستخدم إمكانية الوصول والطبقة، خاصة عند التثبيت خارج Play. إيقاف حماية Play ليس الحل المطلوب.

المسار الثابت: **Play Console → الاختبار الداخلي**. أزل أي نسخة debug سابقة قبل تثبيت APK موقّع للإصدار — أندرويد يرفض الترقية عند اختلاف التوقيع.

### الأذونات وحدود أندرويد

- **الكاميرا:** لرصد اليد على الجهاز فقط.
- **إمكانية الوصول:** لتنفيذ الرئيسية والرجوع والنقر. تفعّلها أنت من إعدادات النظام.
- **الظهور فوق التطبيقات:** للمؤشر الأبيض.
- **الإشعارات:** حتى تبقى الخدمة الأمامية بعد مغادرة التطبيق.

أندرويد العادي **لا** يسمح لأي تطبيق بإيقاف الهاتف بالكامل، أو النجاة من كل قاتل بطارية لدى الشركات، أو إدخال اللمس دون خدمة إمكانية وصول فعّلتها بنفسك. قاطع الطوارئ يوقف Air Control ويحرّر الكاميرا — ولا يُغلق بقية التطبيقات ولا يُطفئ الجهاز.

---

## License / الترخيص

Project files in this repository. MediaPipe Hands model (`hand_landmarker.task`) is provided by Google under its own terms.
