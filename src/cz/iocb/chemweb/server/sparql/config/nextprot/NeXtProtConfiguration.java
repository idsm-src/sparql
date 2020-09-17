package cz.iocb.chemweb.server.sparql.config.nextprot;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.common.Common;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;



public class NeXtProtConfiguration extends SparqlDatabaseConfiguration
{
    static final String schema = "nextprot";


    public NeXtProtConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool);

        addPrefixes(this);

        addResourceClasses(this);
        Common.addResourceClasses(this);
        Ontology.addResourceClasses(this);

        addQuadMapping(this);
    }


    public static void addPrefixes(SparqlDatabaseConfiguration config)
    {
        config.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        config.addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        config.addPrefix("owl", "http://www.w3.org/2002/07/owl#");
        config.addPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
        config.addPrefix("template", "http://bioinfo.iocb.cz/0.9/template#");

        config.addPrefix("", "http://nextprot.org/rdf#");
        config.addPrefix("dc", "http://purl.org/dc/elements/1.1/");
        config.addPrefix("up", "http://purl.uniprot.org/core/");
        config.addPrefix("entry", "http://nextprot.org/rdf/entry/");
        config.addPrefix("isoform", "http://nextprot.org/rdf/isoform/");
        config.addPrefix("annotation", "http://nextprot.org/rdf/annotation/");
        config.addPrefix("evidence", "http://nextprot.org/rdf/evidence/");
        config.addPrefix("xref", "http://nextprot.org/rdf/xref/");
        config.addPrefix("publication", "http://nextprot.org/rdf/publication/");
        config.addPrefix("identifier", "http://nextprot.org/rdf/identifier/");
        config.addPrefix("cv", "http://nextprot.org/rdf/terminology/");
        //config.addPrefix("gene", "http://nextprot.org/rdf/gene/");
        //config.addPrefix("source", "http://nextprot.org/rdf/source/");
        config.addPrefix("db", "http://nextprot.org/rdf/db/");
        config.addPrefix("context", "http://nextprot.org/rdf/context/");
        config.addPrefix("interaction", "http://nextprot.org/rdf/interaction/");
        config.addPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        config.addPrefix("uniprot", "http://purl.uniprot.org/uniprot/");
        config.addPrefix("unipage", "http://www.uniprot.org/uniprot/");
        config.addPrefix("proteoform", "http://nextprot.org/rdf/proteoform/");
        //config.addPrefix("chebi", "http://purl.obolibrary.org/obo/");
        config.addPrefix("drugbankdrugs", "http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/drugs/");
    }


    public static void addResourceClasses(SparqlDatabaseConfiguration config) throws SQLException
    {
        Chromosome.addResourceClasses(config);
        Context.addResourceClasses(config);
        Expression.addResourceClasses(config);
        Publication.addResourceClasses(config);
        Schema.addResourceClasses(config);
        Terminology.addResourceClasses(config);
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        Chromosome.addQuadMapping(config);
        Context.addQuadMapping(config);
        Expression.addQuadMapping(config);
        Publication.addQuadMapping(config);
        Schema.addQuadMapping(config);
        Terminology.addQuadMapping(config);
    }
}
