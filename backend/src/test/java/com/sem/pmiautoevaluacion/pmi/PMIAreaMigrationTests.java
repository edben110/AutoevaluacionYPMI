package com.sem.pmiautoevaluacion.pmi;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PMIAreaMigrationTests {
    @Autowired JdbcTemplate jdbc;

    @Test
    void splitsAnExistingAnnualPlanWithoutLosingPrioritiesOrFormulation() throws Exception {
        // Un esquema aislado y transaccional reproduce V10 sin tocar los datos locales de usuario.
        String schema = "pmi_migration_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("CREATE SCHEMA " + schema);
        jdbc.execute("SET LOCAL search_path TO " + schema);
        jdbc.execute("""
                CREATE TABLE area (id UUID PRIMARY KEY, state VARCHAR(20));
                CREATE TABLE process (id UUID PRIMARY KEY, area_id UUID REFERENCES area(id));
                CREATE TABLE component (id UUID PRIMARY KEY, process_id UUID REFERENCES process(id));
                CREATE TABLE self_evaluation (id UUID PRIMARY KEY);
                CREATE TABLE component_valuation (id UUID PRIMARY KEY, component_id UUID REFERENCES component(id));
                """);
        migration("V9__create_pmi_prioritization.sql");
        migration("V10__create_pmi_formulation.sql");
        UUID original = UUID.randomUUID(), evaluation = UUID.randomUUID();
        UUID areaA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID areaB = UUID.fromString("00000000-0000-0000-0000-000000000002");
        jdbc.update("INSERT INTO self_evaluation VALUES (?)", evaluation);
        jdbc.update("INSERT INTO pmi VALUES (?, ?, 1, DATE '2026-10-21', TIMESTAMPTZ '2026-10-21 10:00:00Z', TIMESTAMPTZ '2026-10-21 11:00:00Z')", original, evaluation);
        UUID[] priorityIds = {UUID.randomUUID(), UUID.randomUUID()};
        UUID[] objectiveIds = {UUID.randomUUID(), UUID.randomUUID()};
        UUID[] goalIds = {UUID.randomUUID(), UUID.randomUUID()};
        UUID[] indicatorIds = {UUID.randomUUID(), UUID.randomUUID()};
        UUID[] rowIds = {UUID.randomUUID(), UUID.randomUUID()};
        for (int index = 0; index < 2; index++) {
            UUID area = index == 0 ? areaA : areaB;
            UUID process = UUID.randomUUID(), component = UUID.randomUUID(), valuation = UUID.randomUUID();
            jdbc.update("INSERT INTO area VALUES (?, 'ACTIVE')", area);
            jdbc.update("INSERT INTO process VALUES (?, ?)", process, area);
            jdbc.update("INSERT INTO component VALUES (?, ?)", component, process);
            jdbc.update("INSERT INTO component_valuation VALUES (?, ?)", valuation, component);
            jdbc.update("INSERT INTO prioritary_component VALUES (?, ?, ?, 'Conservar factores', 5, 5, 5, ?)", priorityIds[index], original, valuation, index + 1);
            jdbc.update("INSERT INTO pmi_objective VALUES (?, ?, 'Conservar objetivo')", objectiveIds[index], priorityIds[index]);
            jdbc.update("INSERT INTO pmi_goal VALUES (?, ?, 'Conservar meta', 0)", goalIds[index], objectiveIds[index]);
            jdbc.update("INSERT INTO pmi_indicator VALUES (?, 'Conservar indicador')", indicatorIds[index]);
            jdbc.update("""
                    INSERT INTO pmi_activity (id, goal_id, indicator_id, description, total_cost, fse_amount,
                        other_amount, start_date, finish_date, display_order)
                    VALUES (?, ?, ?, 'Conservar actividades', 123.45, NULL, 0.00, DATE '2027-02-03', DATE '2027-11-30', 0)
                    """, rowIds[index], goalIds[index], indicatorIds[index]);
        }
        migration("V11__scope_pmi_by_area.sql");
        assertEquals(2, count("pmi"));
        assertEquals(areaA, jdbc.queryForObject("SELECT area_id FROM pmi WHERE id = ?", UUID.class, original));
        assertEquals(2, jdbc.queryForObject("SELECT count(DISTINCT pmi_id) FROM prioritary_component", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM pmi WHERE filled_on = DATE '2026-10-21' AND created_at = TIMESTAMPTZ '2026-10-21 10:00:00Z' AND updated_at = TIMESTAMPTZ '2026-10-21 11:00:00Z'", Integer.class));
        for (String table : new String[] {"prioritary_component", "pmi_objective", "pmi_goal", "pmi_activity", "pmi_indicator"}) assertEquals(2, count(table));
        for (int index = 0; index < 2; index++) {
            assertEquals(index == 0 ? areaA : areaB, jdbc.queryForObject("SELECT p.area_id FROM pmi p JOIN prioritary_component c ON c.pmi_id=p.id WHERE c.id=?", UUID.class, priorityIds[index]));
            assertEquals(priorityIds[index], jdbc.queryForObject("SELECT prioritary_component_id FROM pmi_objective WHERE id=?", UUID.class, objectiveIds[index]));
            assertEquals(objectiveIds[index], jdbc.queryForObject("SELECT objective_id FROM pmi_goal WHERE id=?", UUID.class, goalIds[index]));
            assertEquals(indicatorIds[index], jdbc.queryForObject("SELECT indicator_id FROM pmi_activity WHERE id=?", UUID.class, rowIds[index]));
            assertEquals("123.45", jdbc.queryForObject("SELECT total_cost::text FROM pmi_activity WHERE id=?", String.class, rowIds[index]));
            assertNull(jdbc.queryForObject("SELECT fse_amount FROM pmi_activity WHERE id=?", java.math.BigDecimal.class, rowIds[index]));
            assertEquals("Conservar actividades", jdbc.queryForObject("SELECT description FROM pmi_activity WHERE id=?", String.class, rowIds[index]));
            assertEquals("2027-02-03", jdbc.queryForObject("SELECT start_date::text FROM pmi_activity WHERE id=?", String.class, rowIds[index]));
        }
    }

    private int count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class); }
    private void migration(String name) throws Exception {
        jdbc.execute(new ClassPathResource("db/migration/" + name).getContentAsString(StandardCharsets.UTF_8));
    }
}
