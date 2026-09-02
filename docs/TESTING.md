# Automated tests (`feature/automated-tests`)

## Seed data

### `seedBase` — catalog only
- **8 employees:** 1001 … 1008 (Free, Busy, Reza, Sara, Omid, Nima, Leila, Karim)
- **6 cars:** Pride, Samand, Peugeot 206, Dena, Quick, Tiba

### `seedWithHistory` — catalog + trips
- 4 closed rentals (20, 22–23, 24, 25 Mordad 1405)
- 2 open rentals on 26 (Busy/Samand → Isfahan, Omid/Quick → Shiraz)

## Test classes

| Class | Focus |
|-------|--------|
| `RentalLifecycleTest` | pickup/return rules on empty history |
| `ReportFilterTest` | filters on history seed |
| `HistorySeedIntegrationTest` | fixture integrity + parallel open trips |
| `EmployeeCatalogTest` | list/find/soft-delete/duplicate id |
| `CarServiceTest` | fleet size, plate, update, delete |
| `JalaliDateTest` / `InputValidatorsTest` | pure unit |

## Run

```bash
cd <project-root>   # folder with pom.xml
git pull origin feature/automated-tests
mvn test
```

Do **not** merge to `master` until explicitly requested.
