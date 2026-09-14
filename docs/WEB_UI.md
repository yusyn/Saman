# پنل وب سامان

صفحات استاتیک داخل Spring Boot:

- `src/main/resources/static/index.html`
- `css/app.css`
- `js/api.js` + `js/app.js`

## اجرا

```bash
mvn spring-boot:run
# یا
java -jar target/saman-1.0.0-SNAPSHOT.jar
```

مرورگر:

```text
http://127.0.0.1:8080/
# از کلاینت دیگر: http://IP-سرور:8080/
```

همان APIهای `/api/...` را صدا می‌زند (same-origin).

> رابط Swing حذف شده است؛ فقط Web UI + REST API.
