# Automated tests (feature/automated-tests)

## What is covered

| Area | Class | Notes |
|------|--------|--------|
| Jalali conversion | `JalaliDateTest` | Pure unit tests |
| English name validation | `InputValidatorsTest` | Pure unit tests |
| Pickup / return / status | `RentalLifecycleTest` | Temp SQLite + seed |
| Report filters | `ReportFilterTest` | Open/closed, name, day overlap |
| Cars / device id | `CarServiceTest` | Duplicate plate, next id |

**Not covered:** real ZK device, Swing UI clicks.

## Seed data

See `TestDataSeed`: employees `1001` / `1002`, cars `11B22233` / `22C33344`.

Each test uses a **temporary SQLite file** (not `CarRental.db`).

## How to run

```bash
mvn test
```

Or in IntelliJ: open as Maven project → run any `*Test` class.

## Branch workflow

```bash
git fetch origin
git checkout feature/automated-tests
git pull origin feature/automated-tests
mvn test
```

After review, merge into `master` via PR or:

```bash
git checkout master
git merge feature/automated-tests
git push origin master
```
