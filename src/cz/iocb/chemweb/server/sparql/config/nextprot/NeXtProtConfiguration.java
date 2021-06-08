package cz.iocb.chemweb.server.sparql.config.nextprot;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.common.Common;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;



public class NeXtProtConfiguration extends SparqlDatabaseConfiguration
{
    static final String schema = "nextprot";


    public NeXtProtConfiguration(String service, DataSource connectionPool, DatabaseSchema schema) throws SQLException
    {
        super(service, connectionPool, schema);

        addPrefixes();
        addResourceClasses();
        addQuadMappings();
    }


    private void addPrefixes()
    {
        Common.addPrefixes(this);

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
        //addPrefix("gene", "http://nextprot.org/rdf/gene/");
        //addPrefix("source", "http://nextprot.org/rdf/source/");
        addPrefix("db", "http://nextprot.org/rdf/db/");
        addPrefix("context", "http://nextprot.org/rdf/context/");
        addPrefix("interaction", "http://nextprot.org/rdf/interaction/");
        addPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        addPrefix("uniprot", "http://purl.uniprot.org/uniprot/");
        addPrefix("unipage", "http://www.uniprot.org/uniprot/");
        addPrefix("proteoform", "http://nextprot.org/rdf/proteoform/");
        //addPrefix("chebi", "http://purl.obolibrary.org/obo/");
        addPrefix("drugbankdrugs", "http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/drugs/");
    }


    private void addResourceClasses() throws SQLException
    {
        Common.addResourceClasses(this);
        Ontology.addResourceClasses(this);

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
