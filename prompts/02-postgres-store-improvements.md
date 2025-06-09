# PostgreSQL Store Test Improvements

## Objective
Improve the PostgresStoreTest to use proper resource management patterns.

## Current Issue
Currently using:
```scala
store <- PostgresStore.resource[IO](config).use(IO.pure)
```

## Required Change
Create the store and then use it in the body of the test:
```scala
PostgresStore.resource[IO](config).use { store =>
  // the test
}
```

## Acceptance Criteria
- [ ] All PostgresStoreTest methods use the resource pattern correctly
- [ ] Tests still pass after the change
- [ ] Resource cleanup is properly handled

## Status
🟢 **Completed** - Fixed in previous implementation.