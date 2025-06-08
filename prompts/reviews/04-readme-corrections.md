# README Corrections

## Objective
Correct the README documentation regarding development vs CI commands.

## Current Issue
The README says:
```
The project includes a CI alias for running all checks:
```
```
```
But that is just for CI, developers should use `sbt commitCheck`.

## Required Changes
- Update README to clarify the difference between CI and development commands
- Replace references to `sbt ci` with `sbt commitCheck` for development workflow
- Ensure developers understand the correct command to use before committing

## Acceptance Criteria
- [ ] README correctly documents `sbt commitCheck` for development
- [ ] Clear distinction between CI and development commands
- [ ] All references updated consistently

## Status
🟢 **Completed** - README corrected and committed.