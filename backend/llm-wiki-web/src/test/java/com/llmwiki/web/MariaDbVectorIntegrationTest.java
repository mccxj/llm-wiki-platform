package com.llmwiki.web;

import org.junit.jupiter.api.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MariaDB 11.8+ native VECTOR integration test.
 * Verifies that vectors can be stored via VEC_FromText() and
 * queried via VEC_DISTANCE() — the core requirement of issue #71.
 * Requires Docker to be available; skipped otherwise.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MariaDbVectorIntegrationTest {

    private static final int VECTOR_DIMENSION = 3;

    @Container
    static final MariaDBContainer<?> mariadb = new MariaDBContainer<>(
            DockerImageName.parse("mariadb:11.8"))
            .withDatabaseName("llmwiki")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", mariadb::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                mariadb.getJdbcUrl(), mariadb.getUsername(), mariadb.getPassword());
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Create kg_nodes and kg_vectors tables with VECTOR column")
    void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS kg_nodes (" +
                "  id UUID PRIMARY KEY," +
                "  name VARCHAR(255) NOT NULL," +
                "  node_type VARCHAR(20) NOT NULL" +
                ")"
            );
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS kg_vectors (" +
                "  node_id UUID PRIMARY KEY," +
                "  vector VECTOR(" + VECTOR_DIMENSION + ") NOT NULL," +
                "  model VARCHAR(100) DEFAULT 'test-model'" +
                ")"
            );
        }

        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tables = metaData.getTables(null, null, "kg_vectors", null);
        assertTrue(tables.next(), "kg_vectors table should exist");
    }

    @Test
    @Order(2)
    @DisplayName("Insert a vector using VEC_FromText()")
    void insertVector() throws SQLException {
        UUID nodeId = UUID.randomUUID();

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO kg_nodes (id, name, node_type) VALUES (?, ?, ?)")) {
            ps.setString(1, nodeId.toString());
            ps.setString(2, "TestNode");
            ps.setString(3, "ENTITY");
            ps.executeUpdate();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO kg_vectors (node_id, vector, model) VALUES (?, VEC_FromText(?), ?)")) {
            ps.setString(1, nodeId.toString());
            ps.setString(2, "[0.1, 0.2, 0.3]");
            ps.setString(3, "test-model");
            int rows = ps.executeUpdate();
            assertEquals(1, rows, "Should insert exactly one vector row");
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT node_id, VEC_AsText(vector) as vec_text, model FROM kg_vectors WHERE node_id = ?")) {
            ps.setString(1, nodeId.toString());
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "Vector should be retrievable after insert");
            assertEquals("test-model", rs.getString("model"));
            String vecText = rs.getString("vec_text");
            assertNotNull(vecText, "VEC_AsText should return non-null");
            assertTrue(vecText.contains("0.1"), "Vector text should contain 0.1");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Query vector similarity using VEC_DISTANCE()")
    void queryVectorSimilarity() throws SQLException {
        insertTestNodeWithVector(UUID.randomUUID(), "NodeA", "[0.1, 0.2, 0.3]");
        insertTestNodeWithVector(UUID.randomUUID(), "NodeB", "[0.9, 0.8, 0.7]");
        UUID targetId = UUID.randomUUID();
        insertTestNodeWithVector(targetId, "Target", "[0.1, 0.2, 0.3]");

        String sql = "SELECT v.node_id, n.name, " +
                     "VEC_DISTANCE(v.vector, VEC_FromText(?)) as distance " +
                     "FROM kg_vectors v " +
                     "JOIN kg_nodes n ON v.node_id = n.id " +
                     "ORDER BY distance ASC " +
                     "LIMIT 5";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "[0.1, 0.2, 0.3]");
            ResultSet rs = ps.executeQuery();

            assertTrue(rs.next(), "Should have at least one result");

            double closestDistance = rs.getDouble("distance");
            assertEquals(0.0, closestDistance, 0.001,
                    "Distance to identical vector should be ~0");

            String closestName = rs.getString("name");
            assertTrue(closestName.equals("NodeA") || closestName.equals("Target"),
                    "Closest node should be NodeA or Target (identical vectors)");
        }
    }

    @Test
    @Order(4)
    @DisplayName("Verify VEC_DISTANCE ordering is correct (closer = smaller distance)")
    void verifyDistanceOrdering() throws SQLException {
        UUID closeId = UUID.randomUUID();
        UUID farId = UUID.randomUUID();
        insertTestNodeWithVector(closeId, "Close", "[0.1, 0.2, 0.3]");
        insertTestNodeWithVector(farId, "Far", "[0.9, 0.9, 0.9]");

        String sql = "SELECT v.node_id, n.name, " +
                     "VEC_DISTANCE(v.vector, VEC_FromText(?)) as distance " +
                     "FROM kg_vectors v " +
                     "JOIN kg_nodes n ON v.node_id = n.id " +
                     "WHERE v.node_id IN (?, ?) " +
                     "ORDER BY distance ASC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "[0.1, 0.2, 0.3]");
            ps.setString(2, closeId.toString());
            ps.setString(3, farId.toString());
            ResultSet rs = ps.executeQuery();

            assertTrue(rs.next());
            assertEquals("Close", rs.getString("name"),
                    "Closer vector should come first");
            double closeDist = rs.getDouble("distance");

            assertTrue(rs.next());
            assertEquals("Far", rs.getString("name"),
                    "Farther vector should come second");
            double farDist = rs.getDouble("distance");

            assertTrue(closeDist < farDist,
                    "Close distance (" + closeDist + ") should be less than far distance (" + farDist + ")");
        }
    }

    private void insertTestNodeWithVector(UUID nodeId, String name, String vectorStr) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO kg_nodes (id, name, node_type) VALUES (?, ?, ?)")) {
            ps.setString(1, nodeId.toString());
            ps.setString(2, name);
            ps.setString(3, "ENTITY");
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO kg_vectors (node_id, vector, model) VALUES (?, VEC_FromText(?), ?)")) {
            ps.setString(1, nodeId.toString());
            ps.setString(2, vectorStr);
            ps.setString(3, "test-model");
            ps.executeUpdate();
        }
    }
}
