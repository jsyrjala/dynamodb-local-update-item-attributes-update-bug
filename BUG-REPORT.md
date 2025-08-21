# DynamoDB Local Bug Report: UpdateItem with attributeUpdates Missing Default PUT Action

## Summary

DynamoDB Local version 3.0.0 does not properly default to PUT action when using `UpdateItem` with `attributeUpdates` parameter and no explicit action is specified. This behavior differs from the real AWS DynamoDB service, which correctly defaults to PUT action when the action parameter is omitted.

## Environment

- **DynamoDB Local Version**: 3.0.0
- **AWS SDK for Java**: V2 (BOM version 2.21.29)
- **Java Version**: 21
- **Test Framework**: JUnit 5.9.2

## Expected Behavior

According to AWS DynamoDB documentation, when using `attributeUpdates` with `UpdateItem`, if no action is specified, it should default to `PUT` action. This is the behavior of the actual AWS DynamoDB service in the cloud.

## Actual Behavior

DynamoDB Local does not apply the update when using `attributeUpdates` without an explicit action. The item remains unchanged, indicating that the default PUT action is not being applied.

## Test Case Evidence

This repository contains three test cases that demonstrate the bug:

### Test Case 1: `testUpdateItemWithAttributeUpdates()` - **FAILS (Bug)**
- Uses `attributeUpdates` without specifying an explicit action
- Should default to PUT action and update the item
- **Result**: Item is not updated (bug present)

### Test Case 2: `testUpdateItemWithUpdateExpression()` - **PASSES (Working Alternative)**
- Uses `updateExpression` instead of `attributeUpdates`
- Successfully updates the item
- **Result**: Item is correctly updated

### Test Case 3: `testUpdateItemWithAttributeUpdatesExplicitPutAction()` - **PASSES (Workaround)**
- Uses `attributeUpdates` with explicit `AttributeAction.PUT`
- Successfully updates the item
- **Result**: Item is correctly updated

## Code Example

The failing case (Test Case 1):

```java
Map<String, AttributeValueUpdate> attributeUpdates = new HashMap<>();
attributeUpdates.put("name", AttributeValueUpdate.builder()
        .value(AttributeValue.builder().s("Updated Name").build())
        .build()); // No .action(AttributeAction.PUT) specified

UpdateItemRequest updateRequest = UpdateItemRequest.builder()
        .tableName(TABLE_NAME)
        .key(key)
        .attributeUpdates(attributeUpdates)
        .build();

dynamoDbClient.updateItem(updateRequest);
// Item is NOT updated in DynamoDB Local (should be updated)
```

The working workaround (Test Case 3):

```java
Map<String, AttributeValueUpdate> attributeUpdates = new HashMap<>();
attributeUpdates.put("name", AttributeValueUpdate.builder()
        .value(AttributeValue.builder().s("Updated Name").build())
        .action(AttributeAction.PUT) // Explicit PUT action
        .build());

UpdateItemRequest updateRequest = UpdateItemRequest.builder()
        .tableName(TABLE_NAME)
        .key(key)
        .attributeUpdates(attributeUpdates)
        .build();

dynamoDbClient.updateItem(updateRequest);
// Item IS updated in DynamoDB Local
```

## Impact

This bug affects applications that:
1. Use DynamoDB Local for local development/testing
2. Rely on the default PUT behavior for `attributeUpdates`
3. Work correctly against real AWS DynamoDB but fail with DynamoDB Local

## Reproduction Steps

1. Clone the reproduction repository: https://github.com/jsyrjala/dynamodb-local-update-item-attributes-update-bug
2. Run `./gradlew test`
3. Observe that `testUpdateItemWithAttributeUpdates()` fails
4. Observe that `testUpdateItemWithUpdateExpression()` and `testUpdateItemWithAttributeUpdatesExplicitPutAction()` pass

## Expected Fix

DynamoDB Local should behave consistently with AWS DynamoDB service by defaulting to PUT action when no action is specified in `attributeUpdates`.

## Workarounds

Until this bug is fixed, developers can:
1. Always explicitly specify `AttributeAction.PUT` when using `attributeUpdates`
2. Use `updateExpression` instead of `attributeUpdates` (recommended approach for new code)