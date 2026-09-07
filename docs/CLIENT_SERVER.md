# Saman — Client / Server API

## اجرا فقط سرور

```bash
pkill -f 'saman-1.0.0-SNAPSHOT.jar' || true
cd ~/Saman
git pull origin feature/client-server
mvn -DskipTests package
export SAMAN_UI_ENABLED=false
export FINGERPRINT_HOST=192.168.30.200   # در صورت نیاز
nohup java -jar target/saman-1.0.0-SNAPSHOT.jar --server-only > /tmp/saman.log 2>&1 &
sleep 8
curl -s http://127.0.0.1:8080/api/health; echo
```

## Endpointها

| متد | مسیر | توضیح |
|-----|------|--------|
| GET | `/api/health` | زنده بودن |
| GET | `/api/cars` | همه ماشین‌ها |
| GET | `/api/cars/available` | آزاد |
| GET | `/api/employees` | لیست کارمند |
| GET | `/api/employees/{deviceUserId}` | یک کارمند |
| POST | `/api/rentals/pickup` | تحویل |
| POST | `/api/rentals/return` | برگشت |
| GET | `/api/rentals/active/{deviceUserId}` | اجاره فعال |
| GET | `/api/rentals/report` | گزارش |
| POST | `/api/fingerprint/verify` | انتظار اثر انگشت روی دستگاه سرور |

### مثال‌ها

```bash
curl -s http://127.0.0.1:8080/api/employees; echo

curl -s -X POST http://127.0.0.1:8080/api/rentals/pickup \
  -H "Content-Type: application/json" \
  -d '{"deviceUserId":"1001","plate":"11B22233","destination":"Tehran"}'
echo

curl -s -X POST http://127.0.0.1:8080/api/rentals/return \
  -H "Content-Type: application/json" \
  -d '{"deviceUserId":"1001"}'
echo

# تا حدود 40 ثانیه صبر می‌کند تا انگشت روی دستگاه زده شود
curl -s -X POST http://127.0.0.1:8080/api/fingerprint/verify \
  -H "Content-Type: application/json" \
  -d '{"timeoutSeconds":40}'
echo
```

اگر دستگاه در دسترس نباشد یا انگشت نزنید، پاسخ خطا (مثلاً 504) با پیام timeout می‌آید.
