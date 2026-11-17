package cz.iocb.sparql.engine.config;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.database.VirtualTableDefinition;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * A configuration without a database schema: it maps constant quads, whereas a mapping of a table and a virtual table,
 * whose columns are checked against the schema and whose facts the translation of a query reads from it, are refused
 * when they are registered.
 */
public class SchemaRequirementTest
{
    @Test
    void tablesNeedSchema() throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, null, null, false);

        ConstantIriMapping subject = config.createIriMapping(new Iri("http://example.org/s"));
        ConstantIriMapping predicate = config.createIriMapping(new Iri("http://example.org/p"));
        TermMapping object = config.createLiteralMapping(xsdString, "o");
        SourceTable table = new DatabaseTable("schema_requirement_test", "item");

        config.addQuadMapping(null, subject, predicate, subject);
        assertEquals(1, config.getMappings(null).size());

        assertThrows(IllegalStateException.class,
                () -> config.addQuadMapping(table, null, config.createIriMapping(iri, "s"), predicate, object));

        assertThrows(IllegalStateException.class, () -> config.addQuadMapping(List.of(table, table),
                List.of(new JoinColumns(new TableColumn("s", VARCHAR), new TableColumn("s", VARCHAR))), null, subject,
                predicate, object, List.of(new Conditions(true), new Conditions(true)), List.of(false, false)));

        assertThrows(IllegalStateException.class,
                () -> config.addVirtualTable(new VirtualTable("items"), new VirtualTableDefinition("SELECT 1")));

        assertEquals(1, config.getMappings(null).size());
    }
}
