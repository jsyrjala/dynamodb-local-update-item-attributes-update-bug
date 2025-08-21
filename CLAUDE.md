# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Purpose

This repository contains a test case to demonstrate a bug in AWS DynamoDB Local where UpdateItem with `attributeUpdates` doesn't properly update items, while using `updateExpression` works correctly.

## Project Structure

- Java project using Gradle build tool
- JUnit 5 for testing
- AWS SDK for Java V2
- DynamoDB Local 3.0.0 as embedded in-memory database
- Test cases in `src/test/java/com/example/dynamodb/`

## Bug Description

The bug occurs when using UpdateItem with `attributeUpdates` parameter - the update is not reflected in the item. However, using `updateExpression` instead works correctly.

## Development Commands

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests DynamoDBLocalUpdateItemBugTest

# Clean and build
./gradlew clean build
```

## Test Cases

1. **testUpdateItemWithAttributeUpdates**: Demonstrates the bug using `attributeUpdates` parameter
2. **testUpdateItemWithUpdateExpression**: Shows the working alternative using `updateExpression`

Both tests:
- Set up DynamoDB Local as embedded in-memory instance
- Create a test table
- Put an initial item
- Update the item using different methods
- Verify the update with consistent read