package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intScalarBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLtrLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfRtlLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strScalarBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZoneBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.DirLangStringWithTagClass;
import cz.iocb.sparql.engine.mapping.classes.IntBlankNodeInSegmentClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LangStringWithTagClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.TripleTermClass;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.mapping.datatypes.DateDatatype;
import cz.iocb.sparql.engine.mapping.datatypes.DateTimeDatatype;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral.Direction;
import cz.iocb.sparql.engine.rdf.IntBlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.test.SparqlTest.Quad;



/**
 * Test data of a query evaluation test stored in one table whose columns hold the terms in unboxed built-in classes
 * chosen by a {@link Variant}: the graph and the predicate in an IRI column each (a NULL graph standing for the default
 * graph), the subject and the object in one group of columns per class occurring at the position, of which a row fills
 * the group of the class of its term. A mapping has a single class per position, so one quad mapping is registered per
 * combination of the graph kind, the subject class and the object class occurring in the data.
 */
public final class TypedQuadTable
{
    /**
     * Classes of the literals whose datatypes distinguish the canonical lexical forms.
     */
    public enum LiteralMode
    {
        /**
         * The general class of the datatype ({@code gen...}), holding any valid literal together with its lexical form.
         */
        GENERAL,

        /**
         * The canonical class ({@code xsd...}) for a literal in the canonical form, the class of the other forms
         * ({@code lex...}) otherwise.
         */
        CANONICAL
    }


    /**
     * Classes of the xsd:dateTime and xsd:date literals.
     */
    public enum TemporalMode
    {
        /**
         * The composite classes, holding the instant (or the date) and the zone in separate columns.
         */
        COMPOSITE,

        /**
         * The scalar classes, holding the value with its zone in one column.
         */
        SCALAR,

        /**
         * The classes of a fixed zone, the zone of the literal deciding its class.
         */
        IN_ZONE
    }


    /**
     * Classes of the blank nodes.
     */
    public enum BlankNodeMode
    {
        /**
         * The general composite class, holding the label and the segment in separate columns.
         */
        COMPOSITE,

        /**
         * The general scalar class, holding the label and the segment in one column.
         */
        SCALAR,

        /**
         * The class of the segment shared by all blank nodes of the data.
         */
        IN_SEGMENT
    }


    /**
     * Choice of the classes of the terms: the classes of the literals distinguishing canonical forms, of the temporal
     * literals and of the blank nodes, whether the blank nodes are numbered (their string labels replaced by integers)
     * and whether the language-tagged strings get the classes of their tags instead of the general ones.
     *
     * @param literals classes of the literals distinguishing canonical forms
     * @param temporals classes of the temporal literals
     * @param blankNodes classes of the blank nodes
     * @param intBlankNodes whether the blank nodes are numbered
     * @param taggedLangStrings whether the language-tagged strings get the classes of their tags
     */
    public record Variant(LiteralMode literals, TemporalMode temporals, BlankNodeMode blankNodes, boolean intBlankNodes,
            boolean taggedLangStrings)
    {
        @Override
        public String toString()
        {
            return String.join("/", literals.name(), temporals.name(),
                    blankNodes.name() + (intBlankNodes ? "-INT" : "-STR"), taggedLangStrings ? "TAGGED" : "GENERIC")
                    .toLowerCase(ROOT);
        }
    }


    /**
     * Combination of the classes of a quad mapping: the default graph or a named one, the subject class and the object
     * class.
     *
     * @param defaultGraph true for the default graph, false for the named graphs
     * @param subject the subject class
     * @param object the object class
     */
    private record Combination(boolean defaultGraph, ResourceClass subject, ResourceClass object)
    {
    }


    /**
     * Quad of the data with its terms in the stored form (with numbered blank nodes when the variant asks for them) and
     * the classes of its subject and object.
     *
     * @param graph the graph, or null for the default graph
     * @param subject the subject
     * @param subjectClass class of the subject
     * @param predicate the predicate
     * @param object the object
     * @param objectClass class of the object
     */
    private record Row(RdfTerm graph, RdfTerm subject, ResourceClass subjectClass, RdfTerm predicate, RdfTerm object,
            ResourceClass objectClass)
    {
    }


    /**
     * The table, created anew for every test.
     */
    public static final DatabaseTable table = new DatabaseTable("sparql_test", "typed_quads");

    /**
     * The graph column; NULL for the default graph.
     */
    private static final TableColumn graphColumn = new TableColumn("g", VARCHAR);

    /**
     * The predicate column.
     */
    private static final TableColumn predicateColumn = new TableColumn("p", VARCHAR);

    /**
     * Class of the numbered blank nodes in the shared segment.
     */
    private static final IntBlankNodeInSegmentClass intSegmentClass = new IntBlankNodeInSegmentClass(0);

    /**
     * The configuration receiving the columns and the mappings.
     */
    private final SparqlDatabaseConfiguration config;

    /**
     * Request whose configuration classifies the literals and renders the constants.
     */
    private final Request request;

    /**
     * The choice of the classes.
     */
    private final Variant variant;

    /**
     * Numbers of the blank nodes, by their string labels.
     */
    private final Map<String, Integer> blankNodeNumbers = new HashMap<>();

    /**
     * Column groups of the subject classes, in the order of occurrence.
     */
    private final Map<ResourceClass, List<TableColumn>> subjectColumns = new LinkedHashMap<>();

    /**
     * Column groups of the object classes, in the order of occurrence.
     */
    private final Map<ResourceClass, List<TableColumn>> objectColumns = new LinkedHashMap<>();


    /**
     * Creates the builder of the table of one test.
     *
     * @param config the configuration receiving the columns and the mappings
     * @param request request whose configuration classifies the literals and renders the constants
     * @param variant the choice of the classes
     */
    private TypedQuadTable(SparqlDatabaseConfiguration config, Request request, Variant variant)
    {
        this.config = config;
        this.request = request;
        this.variant = variant;
    }


    /**
     * The variants of the evaluation tests: every combination of the classes of the literals, of the temporal literals
     * and of the blank nodes. The numbered blank nodes and the classes of the language tags are distributed by parity,
     * so that each of their values occurs with each value of every dimension and with each value of the other one.
     *
     * @return the variants
     */
    public static List<Variant> getVariants()
    {
        List<Variant> variants = new ArrayList<>();

        for(LiteralMode literals : LiteralMode.values())
        {
            for(TemporalMode temporals : TemporalMode.values())
            {
                for(BlankNodeMode blankNodes : BlankNodeMode.values())
                {
                    int parity = literals.ordinal() + temporals.ordinal();

                    variants.add(new Variant(literals, temporals, blankNodes, parity % 2 == 1,
                            (parity + blankNodes.ordinal()) % 2 == 1));
                }
            }
        }

        return variants;
    }


    /**
     * Creates the table for the quads in the classes of the variant, stores the quads in it, declares its columns to
     * the schema of the configuration and registers the quad mappings of the class combinations occurring in the data.
     *
     * @param config the configuration receiving the columns and the mappings
     * @param request request whose configuration classifies the literals and renders the constants
     * @param variant the choice of the classes
     * @param quads the quads, a null graph standing for the default graph
     * @throws SQLException on database errors
     */
    public static void store(SparqlDatabaseConfiguration config, Request request, Variant variant, List<Quad> quads)
            throws SQLException
    {
        TypedQuadTable builder = new TypedQuadTable(config, request, variant);
        List<Row> rows = new ArrayList<>(quads.size());

        for(Quad quad : quads)
            rows.add(builder.getRow(quad));

        builder.createTable(rows);
        builder.declareColumns();
        builder.addMappings(rows);
    }


    /**
     * The quad in the stored form, with the column groups of the classes of its subject and object allocated.
     *
     * @param quad the quad
     * @return the quad in the stored form
     */
    private Row getRow(Quad quad)
    {
        RdfTerm subject = getStoredTerm(SparqlTest.getTerm(quad.subject(), true));
        RdfTerm object = getStoredTerm(SparqlTest.getTerm(quad.object(), true));

        ResourceClass subjectClass = getResourceClass(subject);
        ResourceClass objectClass = getResourceClass(object);

        subjectColumns.computeIfAbsent(subjectClass, c -> createColumns("s" + subjectColumns.size(), c));
        objectColumns.computeIfAbsent(objectClass, c -> createColumns("o" + objectColumns.size(), c));

        return new Row(SparqlTest.getTerm(quad.graph(), true), subject, subjectClass,
                SparqlTest.getTerm(quad.predicate(), true), object, objectClass);
    }


    /**
     * The term in the stored form: blank nodes numbered when the variant asks for it, inside triple terms too.
     *
     * @param term the term
     * @return the term in the stored form
     */
    private RdfTerm getStoredTerm(RdfTerm term)
    {
        return switch(term)
        {
            case StrBlankNode bnode when variant.intBlankNodes() ->
            {
                int number = blankNodeNumbers.computeIfAbsent(bnode.getValue(), _ -> blankNodeNumbers.size());
                yield new IntBlankNode(number, bnode.getSegment());
            }
            case TripleTerm triple -> new TripleTerm(getStoredTerm(triple.getSubject()),
                    getStoredTerm(triple.getPredicate()), getStoredTerm(triple.getObject()));
            case null, default -> term;
        };
    }


    /**
     * Class of the term in the variant: the general IRI class, the blank node class of the variant, the class of a
     * literal (see {@link #getLiteralClass}) or the triple term class of the classes of the components.
     *
     * @param term the term in the stored form
     * @return class of the term in the variant
     */
    private ResourceClass getResourceClass(RdfTerm term)
    {
        return switch(term)
        {
            case Iri _ -> iri;
            case StrBlankNode _ -> getBlankNodeClass(false);
            case IntBlankNode _ -> getBlankNodeClass(true);
            case DirLangStringLiteral literal -> variant.taggedLangStrings() ?
                    DirLangStringWithTagClass.get(literal.getDirection(), literal.getTag()) :
                    literal.getDirection() == Direction.LTR ? rdfLtrLangString : rdfRtlLangString;
            case LangStringLiteral literal -> variant.taggedLangStrings() ?
                    LangStringWithTagClass.get(literal.getTag()) : rdfLangString;
            case TypedLiteral literal -> getLiteralClass(literal);
            case TripleTerm triple -> new TripleTermClass(getResourceClass(triple.getSubject()),
                    getResourceClass(triple.getPredicate()), getResourceClass(triple.getObject()));
            default -> throw new IllegalArgumentException("unexpected term " + term);
        };
    }


    /**
     * Blank node class of the variant.
     *
     * @param numbered whether the blank nodes are numbered
     * @return blank node class of the variant
     */
    private ResourceClass getBlankNodeClass(boolean numbered)
    {
        return switch(variant.blankNodes())
        {
            case COMPOSITE -> numbered ? intBlankNode : strBlankNode;
            case SCALAR -> numbered ? intScalarBlankNode : strScalarBlankNode;
            case IN_SEGMENT -> numbered ? intSegmentClass : SparqlTest.bnodeClass;
        };
    }


    /**
     * Class of the typed literal in the variant: the unsupported literal class for an unknown datatype or an invalid
     * lexical form, the only class of a datatype without non-canonical forms (xsd:string), and otherwise the general
     * class, or the canonical class or the class of the other forms, of the datatype, the temporal ones in the kind of
     * classes the variant chooses.
     *
     * @param literal the literal
     * @return class of the typed literal in the variant
     */
    private ResourceClass getLiteralClass(TypedLiteral literal)
    {
        Datatype datatype = config.getDatatype(literal.getType());

        if(datatype == null || !datatype.isValidForm(literal.getValue()))
            return unsupportedType;

        ResourceClass general = datatype.getBaseLiteralClass();
        ResourceClass canonical = datatype.getCanonicalLiteralClass();

        if(datatype instanceof DateTimeDatatype)
        {
            int zone = DateTimeDatatype.getZone(literal);

            general = switch(variant.temporals())
            {
                case COMPOSITE -> genDateTime;
                case SCALAR -> genScalarDateTime;
                case IN_ZONE -> DateTimeInZoneBaseClass.get(zone);
            };

            canonical = switch(variant.temporals())
            {
                case COMPOSITE -> xsdDateTime;
                case SCALAR -> xsdScalarDateTime;
                case IN_ZONE -> DateTimeInZoneClass.get(zone);
            };
        }
        else if(datatype instanceof DateDatatype)
        {
            int zone = DateDatatype.getZone(literal);

            general = switch(variant.temporals())
            {
                case COMPOSITE -> genDate;
                case SCALAR -> genScalarDate;
                case IN_ZONE -> DateInZoneBaseClass.get(zone);
            };

            canonical = switch(variant.temporals())
            {
                case COMPOSITE -> xsdDate;
                case SCALAR -> xsdScalarDate;
                case IN_ZONE -> DateInZoneClass.get(zone);
            };
        }

        if(general.equals(canonical))
            return canonical;

        if(variant.literals() == LiteralMode.GENERAL)
            return general;

        return datatype.isCanonicalForm(literal.getValue()) ? canonical : subtract(general, canonical);
    }


    /**
     * Column group of the class at the position, the columns named by the prefix and their index.
     *
     * @param prefix prefix of the names of the columns
     * @param resClass the class
     * @return column group of the class at the position
     */
    private static List<TableColumn> createColumns(String prefix, ResourceClass resClass)
    {
        List<TableColumn> columns = new ArrayList<>(resClass.getColumnCount());

        for(int i = 0; i < resClass.getColumnCount(); i++)
            columns.add(new TableColumn(prefix + "_" + i, resClass.getSqlTypes().get(i)));

        return columns;
    }


    /**
     * All columns of the table: the graph, the predicate and the column groups of the subject and the object classes.
     *
     * @return all columns of the table
     */
    private List<TableColumn> getColumns()
    {
        List<TableColumn> columns = new ArrayList<>();

        columns.add(graphColumn);
        columns.add(predicateColumn);
        subjectColumns.values().forEach(columns::addAll);
        objectColumns.values().forEach(columns::addAll);

        return columns;
    }


    /**
     * Creates the table anew and inserts the rows; the character columns use the code-point collation, as SPARQL orders
     * strings by code points.
     *
     * @param rows the rows
     * @throws SQLException on database errors
     */
    private void createTable(List<Row> rows) throws SQLException
    {
        List<TableColumn> columns = getColumns();

        String definition = columns.stream()
                .map(c -> c.getName() + " " + c.getType() + (c.getType().equals(VARCHAR) ? " collate \"C\"" : "")
                        + (c.equals(predicateColumn) ? " not null" : ""))
                .collect(joining(", "));

        try(Connection connection = config.getConnectionPool().getConnection();
                Statement statement = connection.createStatement())
        {
            statement.execute("drop table if exists " + table);
            statement.execute("create table " + table + " (" + definition + ")");

            if(rows.isEmpty())
                return;

            List<String> values = new ArrayList<>(rows.size());

            for(Row row : rows)
                values.add(getValues(row).stream().map(Object::toString).collect(joining(", ", "(", ")")));

            String names = columns.stream().map(Column::getName).collect(joining(", "));

            statement.execute("insert into " + table + " (" + names + ") values " + String.join(", ", values));
        }
    }


    /**
     * Values of all columns of the table for the row: the constants of its terms in their classes, NULL in the column
     * groups of the other classes.
     *
     * @param row the row
     * @return values of all columns of the table for the row
     */
    private List<Object> getValues(Row row)
    {
        List<Object> values = new ArrayList<>();

        values.add(row.graph() == null ? "NULL" : iri.toColumns(request, row.graph()).get(0));
        values.add(iri.toColumns(request, row.predicate()).get(0));
        addValues(values, subjectColumns, row.subjectClass(), row.subject());
        addValues(values, objectColumns, row.objectClass(), row.object());

        return values;
    }


    /**
     * Appends the values of the column groups of a position: the constants of the term in the group of its class, NULL
     * elsewhere.
     *
     * @param values the values to extend
     * @param groups the column groups of the position
     * @param resClass class of the term
     * @param term the term
     */
    private void addValues(List<Object> values, Map<ResourceClass, List<TableColumn>> groups, ResourceClass resClass,
            RdfTerm term)
    {
        for(Map.Entry<ResourceClass, List<TableColumn>> group : groups.entrySet())
        {
            if(group.getKey().equals(resClass))
                values.addAll(resClass.toColumns(request, term));
            else
                group.getValue().forEach(_ -> values.add("NULL"));
        }
    }


    /**
     * Declares the columns of the table to the schema of the configuration as the catalog would describe them: all of
     * them nullable but the predicate, the character ones with the code-point collation, which the translator keeps
     * consistent in recursive queries.
     */
    private void declareColumns()
    {
        DatabaseSchema schema = config.getDatabaseSchema();

        for(TableColumn column : getColumns())
        {
            schema.addColumn(table, column);

            if(!column.equals(predicateColumn))
                schema.addNullableColumn(table, column);

            if(column.getType().equals(VARCHAR))
                schema.addCollation(table, column, "\"C\"");
        }
    }


    /**
     * Registers one quad mapping per combination of the graph kind, the subject class and the object class occurring in
     * the rows; the mapping of the default graph requires a NULL graph.
     *
     * @param rows the rows
     */
    private void addMappings(List<Row> rows)
    {
        Set<Combination> combinations = new LinkedHashSet<>();

        for(Row row : rows)
            combinations.add(new Combination(row.graph() == null, row.subjectClass(), row.objectClass()));

        for(Combination combination : combinations)
        {
            TermMapping subject = createMapping(combination.subject(), subjectColumns.get(combination.subject()));
            TermMapping predicate = config.createIriMapping(iri, predicateColumn.getName());
            TermMapping object = createMapping(combination.object(), objectColumns.get(combination.object()));

            if(combination.defaultGraph())
                config.addQuadMapping(table, null, subject, predicate, object,
                        config.createIsNullCondition(table, graphColumn.getName()));
            else
                config.addQuadMapping(table, config.createIriMapping(iri, graphColumn.getName()), subject, predicate,
                        object);
        }
    }


    /**
     * Mapping of the class over its column group, by the factory of the kind of the class.
     *
     * @param resClass the class
     * @param columns the column group
     * @return mapping of the class over its column group
     */
    private TermMapping createMapping(ResourceClass resClass, List<TableColumn> columns)
    {
        String[] names = columns.stream().map(Column::getName).toArray(String[]::new);

        return switch(resClass)
        {
            case IriClass iriClass -> config.createIriMapping(iriClass, names);
            case LiteralClass literalClass -> config.createLiteralMapping(literalClass, names);
            case BlankNodeClass blankNodeClass -> config.createBlankNodeMapping(blankNodeClass, names);
            default -> config.createTermMapping(resClass, names);
        };
    }
}
