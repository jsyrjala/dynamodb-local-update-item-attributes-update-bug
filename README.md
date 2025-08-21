# DynamoDB Local UpdateItem Bug Reproduction

This repository demonstrates a bug in AWS DynamoDB Local where `UpdateItem` with `attributeUpdates` parameter does not default to PUT action when no explicit action is specified, unlike the real AWS DynamoDB service.

## Repository Contents

- **Test Cases**: Three JUnit tests that prove the bug and demonstrate workarounds
- **Bug Report**: Detailed bug report ready for submission to AWS (`BUG-REPORT.md`)
- **Java/Gradle Project**: Minimal reproducible example using modern tooling

## The Bug

DynamoDB Local fails to apply the default PUT action when using `attributeUpdates` without explicit `AttributeAction.PUT`. Real AWS DynamoDB correctly defaults to PUT action in this scenario.

## Project Structure

- **Language**: Java 21
- **Build Tool**: Gradle 
- **Testing**: JUnit 5.9.2
- **AWS SDK**: Java V2 (BOM 2.21.29)
- **DynamoDB Local**: Version 3.0.0 (embedded in-memory)
- **Test Location**: `src/test/java/com/example/dynamodb/DynamoDBLocalUpdateItemBugTest.java`

## Test Cases

1. **`testUpdateItemWithAttributeUpdates()`** ❌ **FAILS** - Demonstrates the bug using `attributeUpdates` without explicit action
2. **`testUpdateItemWithUpdateExpression()`** ✅ **PASSES** - Shows working alternative using `updateExpression`
3. **`testUpdateItemWithAttributeUpdatesExplicitPutAction()`** ✅ **PASSES** - Shows workaround with explicit PUT action

## Quick Start

```bash
# Run all tests to see the bug
./gradlew test

# Run specific test class
./gradlew test --tests DynamoDBLocalUpdateItemBugTest

# Clean and build
./gradlew clean build
```

## Expected Results

- Test 1 should fail (demonstrates the bug)
- Tests 2 and 3 should pass (show working alternatives)

This inconsistency proves that DynamoDB Local does not behave like real AWS DynamoDB when handling `attributeUpdates` without explicit actions.