# Database Migration Improvements

## Current Implementation

The current `DatabaseMigration` object has several areas for improvement:

1. **Error Handling**: Uses `Try` and `Either` instead of more functional abstractions
2. **Logging**: No logging of migration status or errors
3. **Configuration**: Hardcoded migration location and missing connection retry configuration
4. **Type Safety**: Direct access to password without proper encapsulation

## Proposed Improvements

Replace the current implementation with a more robust solution:

```scala
protected[database] def flywayFromConfig(dbConfig: JDBCConfig): Flyway =
  Flyway
    .configure()
    .dataSource(dbConfig.url, dbConfig.user, dbConfig.password.value)
    .connectRetries(10)
    .load()

def migrate[F[_]: Sync: StructuredLogger](dbConfig: JDBCConfig): F[MigrateResult] =
  Sync[F]
    .blocking(flywayFromConfig(dbConfig).migrate())
    .handleErrorWith { t =>
      error(s"Failed to migrate database. ${t.getMessage}", t) >> t.raiseError[F, MigrateResult]
    }
    .flatTap { result =>
      info(s"Database migrated. Warnings: ${result.warnings}")
    }
```

## Benefits

1. **Functional Effects**: Uses `F[_]: Sync` for proper effect management
2. **Structured Logging**: Integrates with the existing `StructuredLogger` system
3. **Connection Resilience**: Adds `connectRetries(10)` for better reliability
4. **Proper Error Handling**: Uses `handleErrorWith` and `raiseError` for functional error management
5. **Result Logging**: Logs migration results including warnings
6. **Separation of Concerns**: Separates Flyway configuration from migration execution

## Implementation Notes

- The `flywayFromConfig` method is marked `protected[database]` for controlled access
- Password access uses `.value` method suggesting a secure wrapper type
- Migration results are properly logged with structured information
- Error messages include both custom context and original exception details

## Acceptance Criteria

- [x] Replace current `Try`/`Either` error handling with functional effects
- [x] Add `F[_]: Sync` type parameter for effect management
- [x] Integrate with existing `StructuredLogger` system
- [x] Add connection retry configuration (`connectRetries(10)`)
- [x] Use proper functional error handling with `handleErrorWith`
- [x] Log migration results including warnings
- [x] Separate Flyway configuration into `protected[db]` method
- [x] Update password access to use `.value` method
- [x] Ensure all existing tests continue to pass
- [x] Update any dependent code to use new signature

## Status

🟢 **Complete** - Functional database migration improvements implemented with structured logging and error handling