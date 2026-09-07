# Saman API — ثبت ماشین و کارمند (رسمی)

## پیش‌نیاز کارمند با اثر انگشت

- سرور در حالت `--server-only`
- `FINGERPRINT_HOST` به IP دستگاه روی LAN سرور
- نام کارمند **انگلیسی**
- هنگام `register` تا پایان enroll روی دستگاه صبر کنید (۳۰–۶۰ ثانیه یا بیشتر)

## ثبت ماشین

```bash
curl -s -X POST http://127.0.0.1:8080/api/cars \
  -H "Content-Type: application/json" \
  -d '{"name":"Pride","plate":"11B22233","color":"White"}'
echo
```

## ثبت کارمند + اثر انگشت (مسیر رسمی)

`fingerIndex`: 0 انگشت کوچک چپ … 4 شست چپ، 5 شست راست … 9 کوچک راست.

```bash
curl -s -X POST http://127.0.0.1:8080/api/employees/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Ali Rezaei","phone":"09120000001","fingerIndex":5}'
echo
```

با شناسه ثابت:

```bash
curl -s -X POST http://127.0.0.1:8080/api/employees/register \
  -H "Content-Type: application/json" \
  -d '{"deviceUserId":"1001","name":"Ali Rezaei","phone":"09120000001","fingerIndex":5}'
echo
```

ترتیب داخلی: اتصال ZK → ساخت user + enroll → در صورت موفقیت INSERT دیتابیس → در شکست enroll حذف از دستگاه؛ در شکست DB بعد از enroll تلاش برای حذف از دستگاه.

## انگشت اضافه

```bash
curl -s -X POST http://127.0.0.1:8080/api/employees/1001/fingers \
  -H "Content-Type: application/json" \
  -d '{"fingerIndex":6}'
echo
```

## Verify (تحویل/برگشت)

```bash
curl -s -X POST http://127.0.0.1:8080/api/fingerprint/verify \
  -H "Content-Type: application/json" \
  -d '{"timeoutSeconds":40}'
echo
```

## نکته

`POST /api/seed/demo` فقط برای دیتای آزمایشی بدون دستگاه است و مسیر رسمی ثبت کارمند نیست.
