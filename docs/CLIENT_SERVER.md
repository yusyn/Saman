# Saman — مسیر Client / Server (مدل ۲)

## ایده

- **سرور:** منطق + دیتابیس + اثر انگشت + HTTP API
- **کلاینت:** بعداً فقط UI؛ داده را با HTTP می‌گیرد

در این مرحله UI و API هنوز در یک jar هستند، اما می‌توان jar را **بدون Swing** روی سرور اجرا کرد.

## دو حالت اجرا

| حالت | دستور |
|------|--------|
| دسکتاپ | `java -jar target/saman-1.0.0-SNAPSHOT.jar` |
| فقط سرور | `java -jar target/saman-1.0.0-SNAPSHOT.jar --server-only` |

```bash
export SAMAN_UI_ENABLED=false
java -jar target/saman-1.0.0-SNAPSHOT.jar
```

## API این مرحله

| متد | مسیر |
|-----|------|
| GET | `/api/health` |
| GET | `/api/cars/available` |
| GET | `/api/cars` |

```bash
curl -s http://127.0.0.1:8080/api/health
curl -s http://127.0.0.1:8080/api/cars/available
```

## لایه‌ها

```
HTTP → api/*Controller → service/* → DatabaseManager
            ↓
         dto/* (JSON)
```
