package com.sem.pmiautoevaluacion.integralManagement;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OfficialCatalogMigrationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;

    @Test
    void officialCatalogMatchesSourceCountsAndRelationships() {
        assertEquals(List.of(4, 15, 45), catalogCounts());
        assertEquals(45, count("SELECT count(DISTINCT name) FROM component WHERE id::text LIKE '34002026-0003-%'"));
        assertEquals(0, count("""
                SELECT count(*) FROM (
                    SELECT state FROM area WHERE id::text LIKE '34002026-0001-%'
                    UNION ALL SELECT state FROM process WHERE id::text LIKE '34002026-0002-%'
                    UNION ALL SELECT state FROM component WHERE id::text LIKE '34002026-0003-%'
                ) catalog WHERE state IS NULL OR state <> 'ACTIVE'
                """));

        Map<String, Integer> componentsByArea = jdbc.query("""
                SELECT a.name, count(c.id) AS components FROM area a
                JOIN process p ON p.area_id = a.id
                JOIN component c ON c.process_id = p.id
                WHERE c.id::text LIKE '34002026-0003-%'
                GROUP BY a.name
                """, (rs, row) -> Map.entry(rs.getString("name"), rs.getInt("components")))
                .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        // Recuento independiente de la matriz del anexo 2, pp. 164-167.
        assertEquals(Map.of(
                "Gestión directiva y estratégica", 16,
                "Gestión académica y pedagógica", 11,
                "Gestión administrativa y financiera", 10,
                "Gestión de la comunidad", 8), componentsByArea);

        List<Integer> componentsByProcess = jdbc.query("""
                SELECT count(c.id) AS components FROM process p
                LEFT JOIN component c ON c.process_id = p.id
                WHERE p.id::text LIKE '34002026-0002-%'
                GROUP BY p.id ORDER BY p.id
                """, (rs, row) -> rs.getInt("components"));
        assertEquals(List.of(4, 4, 2, 4, 2, 5, 3, 3, 2, 2, 4, 2, 2, 3, 3), componentsByProcess);
    }

    @Test
    void restartingFlywayDoesNotDuplicateCatalogOrChangeValuations() {
        List<Integer> originalCounts = catalogCounts();
        int originalValuations = count("SELECT count(*) FROM component_valuation");

        assertEquals(0, flyway.migrate().migrationsExecuted);

        assertEquals(originalCounts, catalogCounts());
        assertEquals(originalValuations, count("SELECT count(*) FROM component_valuation"));
        assertEquals(1, count("SELECT count(*) FROM flyway_schema_history WHERE version = '6' AND success"));
    }

    private List<Integer> catalogCounts() {
        return List.of(
                count("SELECT count(*) FROM area WHERE id::text LIKE '34002026-0001-%'"),
                count("SELECT count(*) FROM process WHERE id::text LIKE '34002026-0002-%'"),
                count("SELECT count(*) FROM component WHERE id::text LIKE '34002026-0003-%'"));
    }

    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
}
