package io.opengemini.client.jdk;

import io.opengemini.client.api.Address;
import io.opengemini.client.api.Query;
import io.opengemini.client.api.QueryResult;
import io.opengemini.client.api.Series;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class OpenGeminiJdkClientTest {

    private OpenGeminiJdkClient openGeminiJdkClient;

    @BeforeEach
    void setUp() {
        Configuration configuration = Configuration.builder()
                .addresses(Collections.singletonList(new Address("127.0.0.1", 8086)))
                .connectTimeout(Duration.ofSeconds(3))
                .timeout(Duration.ofSeconds(5))
                .build();
        this.openGeminiJdkClient = new OpenGeminiJdkClient(configuration);
    }

    @Test
    void queryWithWrongAddress() {
        Configuration configuration = Configuration.builder()
                .addresses(Collections.singletonList(new Address("127.0.0.1", 28086)))
                .connectTimeout(Duration.ofSeconds(3))
                .timeout(Duration.ofSeconds(5))
                .build();

        OpenGeminiJdkClient wrongClient = new OpenGeminiJdkClient(configuration);
        Query showDatabasesQuery = new Query("SHOW DATABASES");
        CompletableFuture<QueryResult> rstFuture = wrongClient.query(showDatabasesQuery);
        Assertions.assertThrows(ExecutionException.class, rstFuture::get);
    }

    @Test
    void testDatabase() throws Exception {
        String databaseTestName = "testDatabase_0001";
        CompletableFuture<Void> createdb = openGeminiJdkClient.createDatabase(databaseTestName);
        createdb.get();

        CompletableFuture<List<String>> rstFuture = openGeminiJdkClient.showDatabases();
        List<String> rst = rstFuture.get();
        Assertions.assertTrue(rst.contains(databaseTestName));

        CompletableFuture<Void> dropdb = openGeminiJdkClient.dropDatabase(databaseTestName);
        dropdb.get();
    }

    @Test
    void testShowField() throws Exception {
        String databaseTestName = "database_test_0001";
        CompletableFuture<Void> createdb = openGeminiJdkClient.createDatabase(databaseTestName);
        createdb.get();

        String measureTestName = "measure_test";
        String rpTestName = "";
        Query createMeasurementQuery = new Query(("CREATE MEASUREMENT %s (tag1 TAG,tag2 TAG,tag3 TAG, "
                + "field1 INT64 FIELD, field2 BOOL, field3 STRING, field4 FLOAT64)")
                .formatted(measureTestName), databaseTestName, rpTestName);
        CompletableFuture<QueryResult> rstFuture = openGeminiJdkClient.query(createMeasurementQuery);
        QueryResult rst = rstFuture.get();
        Assertions.assertEquals(1, rst.getResults().size());

        Query showFieldQuery = new Query("SHOW TAG KEYS FROM %s limit 3 OFFSET 0".formatted(measureTestName),
                databaseTestName, rpTestName);
        CompletableFuture<QueryResult> showRstFuture = openGeminiJdkClient.query(showFieldQuery);
        QueryResult showRst = showRstFuture.get();
        Assertions.assertEquals(1, showRst.getResults().size());
        Series series = showRst.getResults().get(0).getSeries().get(0);
        Assertions.assertEquals(series.getName(), measureTestName);
        Assertions.assertEquals(series.getColumns(), Collections.singletonList("tagKey"));
        Assertions.assertEquals(series.getValues(), List.of(
                List.of("tag1"), List.of("tag2"), List.of("tag3")));

        CompletableFuture<Void> dropdb = openGeminiJdkClient.dropDatabase(databaseTestName);
        dropdb.get();
    }
}
