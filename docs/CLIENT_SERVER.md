# Saman API — seed and CRUD basics

## Empty database

After server is running:

```bash
# Demo pack (3 employees + 3 cars)
curl -s -X POST http://127.0.0.1:8080/api/seed/demo; echo

curl -s http://127.0.0.1:8080/api/employees; echo
curl -s http://127.0.0.1:8080/api/cars; echo
```

### One car

```bash
curl -s -X POST http://127.0.0.1:8080/api/cars \
  -H "Content-Type: application/json" \
  -d '{"name":"Pride","plate":"11B22233","color":"White"}'
echo
```

### One employee (DB only — no fingerprint on device yet)

```bash
curl -s -X POST http://127.0.0.1:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{"deviceUserId":"1001","name":"Ali Rezaei","phone":"09120000001"}'
echo

# or let server assign next id:
curl -s -X POST http://127.0.0.1:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"Sara Mohammadi","phone":"09120000002"}'
echo
```

### Then test rental

```bash
curl -s -X POST http://127.0.0.1:8080/api/rentals/pickup \
  -H "Content-Type: application/json" \
  -d '{"deviceUserId":"1001","plate":"11B22233","destination":"Tehran"}'
echo
```

Note: employee create does **not** enroll on ZK. Fingerprint verify still needs the user on the device.
