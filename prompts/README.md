# Project Prompts Index

This directory contains all prompts and instructions for the Order Management System project.

## Initial Setup
- [01-initial-setup.md](01-initial-setup.md) - 🟢 **Complete** - Core system implementation with orders, subscriptions, and REST API

## Code Reviews & Improvements
- [01-postgres-store-improvements.md](reviews/01-postgres-store-improvements.md) - 🟢 **Complete** - Resource management pattern fixes
- [02-transaction-handling.md](reviews/02-transaction-handling.md) - 🟢 **Complete** - Single transaction wrapping for operations
- [03-local-development-setup.md](reviews/03-local-development-setup.md) - 🟢 **Complete** - Docker scripts and local dev documentation
- [04-readme-corrections.md](reviews/04-readme-corrections.md) - 🟢 **Complete** - Development vs CI command clarification
- [05-postgres-store-test-coverage.md](reviews/05-postgres-store-test-coverage.md) - 🟢 **Complete** - Add missing tests for updateOrder and findActiveSubscriptionsByUser
- [06-postgres-test-optimization.md](reviews/06-postgres-test-optimization.md) - 🟢 **Complete** - Optimize PostgresStore tests by reducing unnecessary commits
- [07-postgres-timestamp-precision-fix.md](reviews/07-postgres-timestamp-precision-fix.md) - 🟢 **Complete** - Fix intermittent CI failure due to timestamp precision differences

## Feature Prompts
- [01-user-subscription-status.md](features/01-user-subscription-status.md) - 🟢 **Complete** - Check whether a user is currently subscribed
- [02-order-cancellation-with-subscription-management.md](features/02-order-cancellation-with-subscription-management.md) - ⚪ **Pending** - Comprehensive order cancellation with subscription management

## Maintenance Prompts
*No maintenance prompts yet - add dependency updates, refactoring tasks here*

## How to Use

1. **For new features**: Create a new file in `prompts/features/` with format `NN-feature-name.md`
2. **For reviews/fixes**: Create a new file in `prompts/reviews/` with format `NN-description.md`
3. **For maintenance**: Create a new file in `prompts/maintenance/` with format `NN-task-name.md`
4. **Update this index**: Add your new prompt to the appropriate section above

## Template Structure

Each prompt should include:
- **Objective**: What you want to achieve
- **Requirements**: Detailed specifications
- **Acceptance Criteria**: Checklist of completion criteria
- **Status**: Current state with emoji indicator

## Status Indicators
- 🟢 **Complete** - Task finished and tested
- 🟡 **In Progress** - Currently being worked on
- ⚪ **Pending** - Not started yet
- ❌ **Blocked** - Cannot proceed due to dependencies