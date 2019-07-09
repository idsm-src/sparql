package cz.iocb.chemweb.server.sparql.config.nextprot;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;



public class NeXtProtConfiguration extends SparqlDatabaseConfiguration
{
    int blankNodeSegment = 1;


    public NeXtProtConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool);

        loadPrefixes();
        loadClasses();
        loadQuadMapping();
    }


    private void loadPrefixes()
    {
        prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
        prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");

        prefixes.put("", "http://nextprot.org/rdf#");
        prefixes.put("dc", "http://purl.org/dc/elements/1.1/");
        prefixes.put("up", "http://purl.uniprot.org/core/");
        prefixes.put("entry", "http://nextprot.org/rdf/entry/");
        prefixes.put("isoform", "http://nextprot.org/rdf/isoform/");
        prefixes.put("annotation", "http://nextprot.org/rdf/annotation/");
        prefixes.put("evidence", "http://nextprot.org/rdf/evidence/");
        prefixes.put("xref", "http://nextprot.org/rdf/xref/");
        prefixes.put("publication", "http://nextprot.org/rdf/publication/");
        prefixes.put("identifier", "http://nextprot.org/rdf/identifier/");
        prefixes.put("cv", "http://nextprot.org/rdf/terminology/");
        prefixes.put("gene", "http://nextprot.org/rdf/gene/");
        prefixes.put("source", "http://nextprot.org/rdf/source/");
        prefixes.put("db", "http://nextprot.org/rdf/db/");
        prefixes.put("context", "http://nextprot.org/rdf/context/");
        prefixes.put("interaction", "http://nextprot.org/rdf/interaction/");
        prefixes.put("skos", "http://www.w3.org/2004/02/skos/core#");
        prefixes.put("uniprot", "http://purl.uniprot.org/uniprot/");
        prefixes.put("unipage", "http://www.uniprot.org/uniprot/");
        prefixes.put("proteoform", "http://nextprot.org/rdf/proteoform/");
        prefixes.put("chebi", "http://purl.obolibrary.org/obo/");
        prefixes.put("drugbankdrugs", "http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/drugs/");
    }


    private void loadClasses() throws SQLException
    {
        Chromosome.addIriClasses(this);
        Context.addIriClasses(this);
        Expression.addIriClasses(this);
        Publication.addIriClasses(this);
        Schema.addIriClasses(this);
        Terminology.addIriClasses(this);
    }


    private void loadQuadMapping()
    {
        Chromosome.addQuadMapping(this);
        Context.addQuadMapping(this);
        Expression.addQuadMapping(this);
        Publication.addQuadMapping(this);
        Schema.addQuadMapping(this);
        Terminology.addQuadMapping(this);
    }
}
