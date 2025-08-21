package com.example.dynamodb;

import software.amazon.dynamodb.services.local.main.ServerRunner;
import software.amazon.dynamodb.services.local.server.DynamoDBProxyServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DynamoDBLocalUpdateItemBugTest {

    private static final String TABLE_NAME = "TestTable";
    private static final String PARTITION_KEY = "id";
    private static final int DYNAMODB_LOCAL_PORT = 8000;
    
    private static DynamoDBProxyServer server;
    private static DynamoDbClient dynamoDbClient;

    @BeforeAll
    static void setUpDynamoDBLocal() throws Exception {
        // Disable DynamoDB Local telemetry
        System.setProperty("com.amazonaws.services.dynamodbv2.local.disableTelemetry", "true");
        
        // Start DynamoDB Local in-memory
        server = ServerRunner.createServerFromCommandLineArgs(
                new String[]{"-inMemory", "-port", String.valueOf(DYNAMODB_LOCAL_PORT)});
        server.start();
        
        // Create DynamoDB client pointing to local instance
        dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:" + DYNAMODB_LOCAL_PORT))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy", "dummy")))
                .build();
        
        // Create table
        createTable();
    }

    @AfterAll
    static void tearDownDynamoDBLocal() throws Exception {
        if (dynamoDbClient != null) {
            dynamoDbClient.close();
        }
        if (server != null) {
            server.stop();
        }
    }

    @BeforeEach
    void setUp() {
        // Put initial item
        putInitialItem();
    }

    private static void createTable() {
        // Create table
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(KeySchemaElement.builder()
                        .attributeName(PARTITION_KEY)
                        .keyType(KeyType.HASH)
                        .build())
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName(PARTITION_KEY)
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        dynamoDbClient.createTable(createTableRequest);

        // Wait for table to be created
        DynamoDbWaiter waiter = dynamoDbClient.waiter();
        WaiterResponse<DescribeTableResponse> waiterResponse = waiter.waitUntilTableExists(
                DescribeTableRequest.builder().tableName(TABLE_NAME).build());
    }

    private void putInitialItem() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(PARTITION_KEY, AttributeValue.builder().s("test-id").build());
        item.put("name", AttributeValue.builder().s("Initial Name").build());
        item.put("count", AttributeValue.builder().n("10").build());

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(putItemRequest);
    }

    @Test
    void testUpdateItemWithAttributeUpdates() {
        // Test case 1: Update using attributeUpdates without explicit action (should default to PUT)
        Map<String, AttributeValueUpdate> attributeUpdates = new HashMap<>();
        attributeUpdates.put("name", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s("Updated Name with AttributeUpdates").build())
                .build());
        attributeUpdates.put("count", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().n("20").build())
                .build());

        Map<String, AttributeValue> key = new HashMap<>();
        key.put(PARTITION_KEY, AttributeValue.builder().s("test-id").build());

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .attributeUpdates(attributeUpdates)
                .build();

        dynamoDbClient.updateItem(updateRequest);

        // Verify the update with consistent read
        GetItemRequest getRequest = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .consistentRead(true)
                .build();

        GetItemResponse response = dynamoDbClient.getItem(getRequest);
        
        assertTrue(response.hasItem(), "Item should exist");
        Map<String, AttributeValue> retrievedItem = response.item();
        
        // Verify updates were applied (this should work but fails due to bug)
        assertEquals("Updated Name with AttributeUpdates", 
                retrievedItem.get("name").s(),
                "Name should be updated when using attributeUpdates without explicit action");
        assertEquals("20", 
                retrievedItem.get("count").n(),
                "Count should be updated when using attributeUpdates without explicit action");
    }

    @Test
    void testUpdateItemWithUpdateExpression() {
        // Test case 2: Update using updateExpression
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(PARTITION_KEY, AttributeValue.builder().s("test-id").build());

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":name", AttributeValue.builder().s("Updated Name with UpdateExpression").build());
        expressionAttributeValues.put(":count", AttributeValue.builder().n("30").build());

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .updateExpression("SET #n = :name, #c = :count")
                .expressionAttributeNames(Map.of("#n", "name", "#c", "count"))
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        dynamoDbClient.updateItem(updateRequest);

        // Verify the update with consistent read
        GetItemRequest getRequest = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .consistentRead(true)
                .build();

        GetItemResponse response = dynamoDbClient.getItem(getRequest);
        
        assertTrue(response.hasItem(), "Item should exist");
        Map<String, AttributeValue> retrievedItem = response.item();
        
        // Verify updates were applied
        assertEquals("Updated Name with UpdateExpression", 
                retrievedItem.get("name").s(),
                "Name should be updated when using updateExpression");
        assertEquals("30", 
                retrievedItem.get("count").n(),
                "Count should be updated when using updateExpression");
    }

    @Test
    void testUpdateItemWithAttributeUpdatesExplicitPutAction() {
        // Test case 3: Update using attributeUpdates with explicit PUT action
        Map<String, AttributeValueUpdate> attributeUpdates = new HashMap<>();
        attributeUpdates.put("name", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s("Updated Name with Explicit PUT").build())
                .action(AttributeAction.PUT)
                .build());
        attributeUpdates.put("count", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().n("40").build())
                .action(AttributeAction.PUT)
                .build());

        Map<String, AttributeValue> key = new HashMap<>();
        key.put(PARTITION_KEY, AttributeValue.builder().s("test-id").build());

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .attributeUpdates(attributeUpdates)
                .build();

        dynamoDbClient.updateItem(updateRequest);

        // Verify the update with consistent read
        GetItemRequest getRequest = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .consistentRead(true)
                .build();

        GetItemResponse response = dynamoDbClient.getItem(getRequest);
        
        assertTrue(response.hasItem(), "Item should exist");
        Map<String, AttributeValue> retrievedItem = response.item();
        
        // Verify updates were applied
        assertEquals("Updated Name with Explicit PUT", 
                retrievedItem.get("name").s(),
                "Name should be updated when using attributeUpdates with explicit PUT action");
        assertEquals("40", 
                retrievedItem.get("count").n(),
                "Count should be updated when using attributeUpdates with explicit PUT action");
    }
}