package cz.iocb.chemweb.server.sparql.config.ontology;

import static cz.iocb.chemweb.server.sparql.config.ontology.OntologyConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.config.ontology.OntologyConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.GeneralUserIriClass;



public class Ontology
{
    public static final String unitUncategorized = "0::smallint";
    public static final String unitBlank = "1::smallint";
    public static final String unitSIO = "2::smallint";
    public static final String unitCHEMINF = "3::smallint";
    public static final String unitBAO = "4::smallint";
    public static final String unitGO = "5::smallint";
    public static final String unitPR = "6::smallint";
    public static final String unitCHEBI = "7::smallint";
    public static final String unitTaxonomy = "11::smallint";
    public static final String unitNCBITaxon = "64::smallint";
    public static final String unitCL = "71::smallint";
    public static final String unitUO = "74::smallint";
    public static final String unitIAO = "75::smallint";
    public static final String unitCLO = "77::smallint";
    public static final String unitEFO = "92::smallint";
    public static final String unitStar = "95::smallint";


    public static void addResourceClasses(SparqlDatabaseConfiguration config) throws SQLException
    {
        StringBuilder builder = new StringBuilder();

        try(Connection connection = config.getConnectionPool().getConnection())
        {
            try(Statement statement = connection.createStatement())
            {
                try(ResultSet result = statement
                        .executeQuery("select pattern from ontology.resource_categories__reftable"))
                {
                    boolean hasResult = false;

                    while(result.next())
                    {
                        if(hasResult)
                            builder.append("|");

                        hasResult = true;

                        builder.append("(" + result.getString(1) + ")");
                    }
                }
            }
        }

        String ontologyPattern = builder.toString();

        config.addIriClass(new GeneralUserIriClass("ontology:resource", "ontology", "ontology_resource",
                Arrays.asList("smallint", "integer"), ontologyPattern, GeneralUserIriClass.SqlCheck.IF_NOT_MATCH));
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("dataset:ontology");

        {
            Table table = new Table(schema, "classes");
            NodeMapping subject = config.createIriMapping("ontology:resource", "class_unit", "class_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Class"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("base/Class.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("base/Class.vm"));
        }

        {
            Table table = new Table(schema, "properties");
            NodeMapping subject = config.createIriMapping("ontology:resource", "property_unit", "property_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("rdf:Property"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("base/Property.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("base/Property.vm"));
        }

        {
            Table table = new Table(schema, "individuals");
            NodeMapping subject = config.createIriMapping("ontology:resource", "individual_unit", "individual_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:NamedIndividual"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("base/NamedIndividual.vm"));
        }

        {
            Table table = new Table(schema, "resource_labels");
            NodeMapping subject = config.createIriMapping("ontology:resource", "resource_unit", "resource_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(rdfLangStringEn, "label"));
        }

        {
            Table table = new Table(schema, "superclasses");
            NodeMapping subject = config.createIriMapping("ontology:resource", "class_unit", "class_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:subClassOf"),
                    config.createIriMapping("ontology:resource", "superclass_unit", "superclass_id"));
        }


        {
            Table table = new Table(schema, "superproperties");
            NodeMapping subject = config.createIriMapping("ontology:resource", "property_unit", "property_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:subPropertyOf"),
                    config.createIriMapping("ontology:resource", "superproperty_unit", "superproperty_id"));
        }

        {
            Table table = new Table(schema, "property_domains");
            NodeMapping subject = config.createIriMapping("ontology:resource", "property_unit", "property_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:domain"),
                    config.createIriMapping("ontology:resource", "domain_unit", "domain_id"));
        }

        {
            Table table = new Table(schema, "property_ranges");
            NodeMapping subject = config.createIriMapping("ontology:resource", "property_unit", "property_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:range"),
                    config.createIriMapping("ontology:resource", "range_unit", "range_id"));
        }

        {
            Table table = new Table(schema, "somevaluesfrom_restrictions");
            NodeMapping subject = config.createIriMapping("ontology:resource", unitBlank, "restriction_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Restriction"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:onProperty"),
                    config.createIriMapping("ontology:resource", "property_unit", "property_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:someValuesFrom"),
                    config.createIriMapping("ontology:resource", "class_unit", "class_id"));
        }

        {
            Table table = new Table(schema, "allvaluesfrom_restrictions");
            NodeMapping subject = config.createIriMapping("ontology:resource", unitBlank, "restriction_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Restriction"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:onProperty"),
                    config.createIriMapping("ontology:resource", "property_unit", "property_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:allValuesFrom"),
                    config.createIriMapping("ontology:resource", "class_unit", "class_id"));
        }

        {
            Table table = new Table(schema, "cardinality_restrictions");
            NodeMapping subject = config.createIriMapping("ontology:resource", unitBlank, "restriction_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Restriction"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:onProperty"),
                    config.createIriMapping("ontology:resource", "property_unit", "property_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:cardinality"),
                    config.createLiteralMapping(xsdInt, "cardinality"));
        }

        {
            Table table = new Table(schema, "mincardinality_restrictions");
            NodeMapping subject = config.createIriMapping("ontology:resource", unitBlank, "restriction_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Restriction"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:onProperty"),
                    config.createIriMapping("ontology:resource", "property_unit", "property_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:minCardinality"),
                    config.createLiteralMapping(xsdInt, "cardinality"));
        }

        {
            Table table = new Table(schema, "maxcardinality_restrictions");
            NodeMapping subject = config.createIriMapping("ontology:resource", unitBlank, "restriction_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Restriction"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:onProperty"),
                    config.createIriMapping("ontology:resource", "property_unit", "property_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:maxCardinality"),
                    config.createLiteralMapping(xsdInt, "cardinality"));
        }
    }
}
