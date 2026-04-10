package cz.iocb.sparql.nextprot.combined;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;



public class NeXtProtCombinedConfiguration extends SparqlDatabaseConfiguration
{
    static final String schema = "nextprot_indirect";


    public NeXtProtCombinedConfiguration(String service, DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        super(service, connectionPool, schema);

        addPrefixes();
        addResourceClasses();
        addQuadMappings();
    }


    private void addPrefixes()
    {
        addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        addPrefix("owl", "http://www.w3.org/2002/07/owl#");
        addPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");

        addPrefix("", "http://nextprot.org/rdf#");
        addPrefix("dc", "http://purl.org/dc/elements/1.1/");
        addPrefix("up", "http://purl.uniprot.org/core/");
        addPrefix("entry", "http://nextprot.org/rdf/entry/");
        addPrefix("isoform", "http://nextprot.org/rdf/isoform/");
        addPrefix("annotation", "http://nextprot.org/rdf/annotation/");
        addPrefix("evidence", "http://nextprot.org/rdf/evidence/");
        addPrefix("xref", "http://nextprot.org/rdf/xref/");
        addPrefix("publication", "http://nextprot.org/rdf/publication/");
        addPrefix("identifier", "http://nextprot.org/rdf/identifier/");
        addPrefix("cv", "http://nextprot.org/rdf/terminology/");
        addPrefix("gene", "http://nextprot.org/rdf/gene/");
        addPrefix("source", "http://nextprot.org/rdf/source/");
        addPrefix("db", "http://nextprot.org/rdf/db/");
        addPrefix("context", "http://nextprot.org/rdf/context/");
        addPrefix("interaction", "http://nextprot.org/rdf/interaction/");
        addPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        addPrefix("uniprot", "http://purl.uniprot.org/uniprot/");
        addPrefix("unipage", "http://www.uniprot.org/uniprot/");
        addPrefix("proteoform", "http://nextprot.org/rdf/proteoform/");
        addPrefix("chebi", "http://purl.obolibrary.org/obo/");
        addPrefix("drugbankdrugs", "http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/drugs/");
    }


    private void addResourceClasses() throws SQLException
    {
        addIriClass(new StringUserIriClass("uniprot", "http://purl.uniprot.org/uniprot/"));
        addIriClass(new StringUserIriClass("uniprotpage", "http://www.uniprot.org/uniprot/"));
        addIriClass(new IntegerUserIriClass("chebi", "int4", "http://purl.obolibrary.org/obo/CHEBI_"));
        addIriClass(new StringUserIriClass("drugbank",
                "http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/drugs/DB"));

        Chromosome.addResourceClasses(this);
        Context.addResourceClasses(this);
        Expression.addResourceClasses(this);
        Publication.addResourceClasses(this);
        Schema.addResourceClasses(this);
        Terminology.addResourceClasses(this);
    }


    private void addQuadMappings()
    {
        Chromosome.addQuadMappings(this);
        Context.addQuadMappings(this);
        Expression.addQuadMappings(this);
        Publication.addQuadMappings(this);
        Schema.addQuadMappings(this);
        Terminology.addQuadMappings(this);
    }
}
