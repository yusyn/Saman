# Automated tests (`feature/automated-tests`)

## Seed volume

| Fixture | Content |
|---------|---------|
| `seedBase` | **12** employees (1001–1012), **10** cars — all free |
| `seedWithHistory` | same catalog + **8** closed trips + **3** open missions |

Constants: `EMP_COUNT`, `CAR_COUNT`, `HISTORY_*` in `TestDataSeed`.

Each test uses a **temporary SQLite file** (not `CarRental.db`).

## Run

```bash
git fetch origin
git checkout feature/automated-tests
git pull origin feature/automated-tests
mvn test
```

Do **not** merge to `master` until you decide.

## Layout

Tests under `tests/java` (outside main `src` tree).
