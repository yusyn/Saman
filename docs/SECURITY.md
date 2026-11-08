# امنیت Saman (Session-based)

## احراز هویت

- روش: **Session cookie** (`SAMANSESSION`) + Spring Security 6
- نقش: حداقل `ROLE_ADMIN` برای تمام عملیات تغییردهنده (POST/PUT/PATCH/DELETE)
- ورود: `POST /api/auth/login` با JSON `{ "username", "password" }` → ایجاد session (دارای Rate Limiter: حداکثر ۵ تلاش ناموفق در هر ۵ دقیقه به ازای IP و بازگشت 429)
- خروج: `POST /api/auth/logout` → باطل کردن session
- وضعیت: `GET /api/auth/me`
- CSRF token: `GET /api/auth/csrf` یا کوکی `XSRF-TOKEN` + هدر `X-XSRF-TOKEN`

پاسخ‌های 401/403 برای API همیشه JSON هستند و به صفحه HTML login ریدایرکت نمی‌شوند.

## Bootstrap مدیر اولیه

فقط وقتی جدول `AppUser` خالی است:

```bash
export SAMAN_ADMIN_USERNAME=admin
export SAMAN_ADMIN_PASSWORD='choose-a-long-secret'
# حداقل ۸ کاراکتر؛ هرگز در git commit نشود
mvn spring-boot:run
```

اگر هیچ کاربری نباشد و متغیرها تنظیم نشوند، برنامه بالا می‌آید ولی endpointهای نوشتنی برای همه 401 می‌مانند (fail-closed).

رمزها فقط به‌صورت BCrypt در SQLite ذخیره می‌شوند.

## پروفایل dev و Seed

`SeedController` فقط با پروفایل `dev` ثبت می‌شود:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
# یا
export SPRING_PROFILES_ACTIVE=dev
```

در production این پروفایل را فعال نکنید. مسیر `/api/seed/demo` خارج از `dev` endpoint فعالی ندارد.

## مسیرهای عمومی (allowlist)

| مسیر | توضیح |
|------|--------|
| `/`, `/index.html`, `/css/**`, `/js/**`, `/icons/**`, `/favicon.svg`, `/favicon.ico` | UI استاتیک |
| `GET /api/health` | سلامت |
| `GET /api/vehicles/**` | خواندن کاتالوگ عمومی ناوگان (به‌جز history) |
| `POST /api/auth/login` | ورود (محدود به ۵ تلاش ناموفق در ۵ دقیقه به ازای IP) |
| `GET /api/auth/csrf`, `GET /api/auth/me` | کمکی auth |
| `POST /api/auth/logout` | خروج (بدون session هم 200 امن) |

## مسیرهای محافظت‌شده

### نیازمند احراز هویت (`authenticated`)
- `GET /api/employees/**`: خواندن اطلاعات و فهرست کارمندان
- `GET /api/rentals/**`: گزارش و وضعیت اجاره‌های فعال
- `GET /api/vehicles/history/**`: سابقه تغییرات خودروها

### نیازمند دسترسی مدیر (`ROLE_ADMIN`)
- تمام متدهای زیر `/api/admin-users/**`
- `GET /api/system-events/**`: مشاهده وقایع سیستم
- `GET /api/device-operations/**`: مشاهده وقایع دستگاه اثرانگشت
- تمام متدهای تغییردهنده (`POST` / `PUT` / `PATCH` / `DELETE`) زیر `/api/**` با اعتبارسنجی CSRF معتبر: خودرو، کارمند، اجاره، سابقه، اثرانگشت، seed (فقط dev)

## CSRF در UI

`static/js/api.js` برای متدهای تغییردهنده:

1. کوکی `XSRF-TOKEN` را می‌خواند یا از `/api/auth/csrf` می‌گیرد
2. هدر `X-XSRF-TOKEN` را می‌فرستد
3. `credentials: "same-origin"` برای ارسال session cookie

## Session

- Timeout پیش‌فرض: ۳۰ دقیقه (`SESSION_TIMEOUT`)
- `HttpOnly` روی session cookie
- `SameSite=Lax`
- پشت HTTPS: `SAMAN_SESSION_SECURE=true`
- Session fixation: `migrateSession`

## امنیت داده‌های پرسنلی و اجاره

دسترسی‌های `GET` برای کارمندان (`/api/employees/**`) و گزارش اجاره (`/api/rentals/**`) نیازمند احراز هویت هستند و بدون ورود کاربر در دسترس نخواهند بود تا حریم خصوصی پرسنل و اطلاعات قراردادها حفظ گردد.
