package cz.iocb.sparql.engine.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.provider.Arguments;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.test.SparqlTest.Quad;
import cz.iocb.sparql.engine.translator.TranslateVisitor;



/**
 * Development tool, enabled by the {@code sqldump.file} system property: dumps the explanation and the translated SQL
 * of every test query into that file, so that the generated code can be compared between refactoring steps. The blank
 * nodes of the test data are renamed and the quads ordered deterministically, but the order of otherwise symmetric
 * blank nodes still varies between runs.
 *
 * <pre>
 * mvn -pl engine test -Dtest=SqlDumpTest -Dsqldump.file=/tmp/sqldump.txt
 * </pre>
 */
@EnabledIfSystemProperty(named = "sqldump.file", matches = ".+")
public class SqlDumpTest
{
    private static final Model bnodes = ModelFactory.createDefaultModel();


    @BeforeAll
    static void init() throws IOException, SQLException
    {
        SparqlTest.init();
    }


    private static void dump(PrintWriter out, String section, String name, Engine engine, String query)
    {
        out.println("### " + section + " :: " + name);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(query, null);

            TranslateVisitor translateVisitor = new TranslateVisitor(request);
            SqlSelect imcode = translateVisitor.translate(prepared.getSyntaxTree(), null, null, List.of());

            imcode = imcode.optimize(request, false);
            imcode = imcode.optimize(request, true);

            String code = imcode.translate(request);

            out.println(imcode.getExplanation());
            out.println(code);
        }
        catch(Throwable e)
        {
            out.println("EXCEPTION " + e.getClass().getName() + ": " + e.getMessage());
        }

        out.println();
    }


    /**
     * Jena assigns random labels to blank nodes of the test data; rename them in first-seen order so that the dump is
     * deterministic.
     */
    private static List<Quad> normalize(List<Quad> quads)
    {
        // order the quads independently of the random labels, then rename the labels in that order, then order again
        List<Quad> sorted = new ArrayList<>(quads);
        sorted.sort(Comparator.comparing(q -> key(q, false)));

        Map<String, RDFNode> labels = new HashMap<>();
        List<Quad> result = new ArrayList<>();

        for(Quad quad : sorted)
            result.add(new Quad(normalize(labels, quad.graph()), normalize(labels, quad.subject()),
                    normalize(labels, quad.predicate()), normalize(labels, quad.object())));

        result.sort(Comparator.comparing(q -> key(q, true)));

        return result;
    }


    private static String key(Quad quad, boolean withLabels)
    {
        return key(quad.graph(), withLabels) + "|" + key(quad.subject(), withLabels) + "|"
                + key(quad.predicate(), withLabels) + "|" + key(quad.object(), withLabels);
    }


    private static String key(RDFNode node, boolean withLabels)
    {
        if(node == null)
            return "";

        if(node.isAnon())
            return withLabels ? "_:" + node.asResource().getId().getLabelString() : "_:";

        return node.toString();
    }


    private static RDFNode normalize(Map<String, RDFNode> labels, RDFNode node)
    {
        if(node == null || !node.isAnon())
            return node;

        return labels.computeIfAbsent(node.asResource().getId().getLabelString(),
                _ -> bnodes.createResource(AnonId.create("b" + labels.size())));
    }


    @Test
    void dumpAll() throws Exception
    {
        Path file = Path.of(System.getProperty("sqldump.file"));

        try(PrintWriter out = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8)))
        {
            for(Arguments args : SparqlTest.getQueryEvaluationSyntaxTests())
            {
                Object[] a = args.get();
                SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, SparqlTest.connectionPool,
                        SparqlTest.schema, false);
                dump(out, "syntax", (String) a[0], new Engine(config), (String) a[1]);
            }

            for(Arguments args : SparqlTest.getQueryEvaluationTests())
            {
                Object[] a = args.get();

                @SuppressWarnings("unchecked")
                List<Quad> quads = normalize((List<Quad>) a[2]);

                SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, SparqlTest.connectionPool,
                        SparqlTest.schema, false);

                for(Quad quad : quads)
                    config.addQuadMapping((ConstantIriMapping) SparqlTest.getMapping(quad.graph(), config),
                            SparqlTest.getMapping(quad.subject(), config),
                            (ConstantIriMapping) SparqlTest.getMapping(quad.predicate(), config),
                            SparqlTest.getMapping(quad.object(), config));

                dump(out, "eval", (String) a[0], new Engine(config), (String) a[1]);


                SparqlDatabaseConfiguration mapConfig = new SparqlDatabaseConfiguration(null, SparqlTest.connectionPool,
                        SparqlTest.schema, false);

                for(Quad quad : quads)
                    mapConfig.addQuadMapping((ConstantIriMapping) SparqlTest.getMapping(quad.graph(), mapConfig),
                            SparqlTest.getMapping(quad.subject(), mapConfig),
                            (ConstantIriMapping) SparqlTest.getMapping(quad.predicate(), mapConfig),
                            SparqlTest.getMapping(quad.object(), mapConfig, SparqlTest.literalClassMap));

                dump(out, "eval-map", (String) a[0], new Engine(mapConfig), (String) a[1]);
            }

            for(Arguments args : SparqlTest.getNextProtTests())
            {
                Object[] a = args.get();
                dump(out, "nextprot-string", (String) a[0], SparqlTest.stringEngine, (String) a[1]);
                dump(out, "nextprot-integer", (String) a[0], SparqlTest.integerEngine, (String) a[1]);
                dump(out, "nextprot-combined", (String) a[0], SparqlTest.combinedEngine, (String) a[1]);
            }
        }
    }
}
