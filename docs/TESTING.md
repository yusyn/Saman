# Automated tests

## Layout

Production code stays under `src/com/...` (legacy).

Tests live under **`tests/java`** (not under `src/`), so they are never compiled as main sources without JUnit.

## Run with Maven (recommended)

```bash
git fetch origin
git checkout feature/automated-tests
git pull origin feature/automated-tests

mvn test
```

## IntelliJ

1. Open the project from the folder that contains `pom.xml` (Maven project).
2. Maven tool window → Reload All Maven Projects.
3. If `tests/java` is not green (test root):
   - Right-click `tests/java` → Mark Directory as → **Test Sources Root**.
4. If old `src/test` exists and is marked as Sources, unmark it or delete that folder locally.
5. Do **not** run tests with the old `Pr` / non-Maven module if it has no Maven test classpath.

Prefer: right-click `pom.xml` context → Maven → Reload, then run `mvn test` in the Terminal tool window.

## What is covered

- Jalali / validators (unit)
- Pickup / return / mission locks
- Report filters
- Duplicate plate / next device user id

Not covered: real ZK device, Swing UI.
