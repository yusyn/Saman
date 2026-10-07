# امنیت Saman (Session-based)

## احراز هویت

- روش: **Session cookie** (`SAMANSESSION`) + Spring Security 6
- نقش: حداقل `ROLE_ADMIN` برای تمام عملیات تغییردهنده (POST/PUT/PATCH/DELETE)
- ورود: `POST /api/auth/login` با JSON `{ "username", "password" }` → ایجاد session
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
| `/`, `/index.html`, `/css/**`, `/js/**`, `/icons/**`, `/favicon.svg` | UI استاتیک |
| `GET /api/health` | سلامت |
| `GET /api/vehicles/**` | خواندن ناوگان و سابقه |
| `GET /api/employees/**` | خواندن کارمندان |
| `GET /api/rentals/**` | گزارش و rental فعال |
| `POST /api/auth/login` | ورود |
| `GET /api/auth/csrf`, `GET /api/auth/me` | کمکی auth |
| `POST /api/auth/logout` | خروج (بدون session هم 200 امن) |

## مسیرهای محافظت‌شده

تمام `POST` / `PUT` / `PATCH` / `DELETE` زیر `/api/**` نیازمند احراز هویت `ROLE_ADMIN` و CSRF معتبر:

- خودرو، کارمند، اجاره، سابقه، اثرانگشت، seed (فقط dev)

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

## تصمیم محصولی (باز)

داده‌های GET کارمندان و گزارش اجاره فعلاً عمومی‌اند تا UI بدون لاگین قابل مشاهده بماند. در صورت نیاز محصولی می‌توان آن‌ها را نیز محافظت کرد.
