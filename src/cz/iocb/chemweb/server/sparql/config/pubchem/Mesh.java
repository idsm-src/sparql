package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.StringUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Mesh
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(
                new StringUserIriClass("mesh", "http://id.nlm.nih.gov/mesh/", "[A-Z][0-9]+(\\.[0-9]+|[A-Z][0-9]+)*"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass mesh = config.getIriClass("mesh");
        ConstantIriMapping graph = config.createIriMapping("pubchem:mesh");

        config.addQuadMapping("mesh", "mesh_bases", graph, config.createIriMapping(mesh, "id"),
                config.createIriMapping("rdf:type"),
                config.createIriMapping("ontology_resource", Ontology.unitUncategorized, "type_id"));
        config.addQuadMapping("mesh", "mesh_bases", graph, config.createIriMapping(mesh, "id"),
                config.createIriMapping("template:itemTemplate"), config.createLiteralMapping("mesh/Mesh.vm"));

        config.addQuadMapping("mesh", "alt_labels", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:altLabel"), config.createLiteralMapping(rdfLangStringEn, "label"));
        config.addQuadMapping("mesh", "previous_indexing_values", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:previousIndexing"),
                config.createLiteralMapping(rdfLangStringEn, "value"));
        config.addQuadMapping("mesh", "sources", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:source"), config.createLiteralMapping(rdfLangStringEn, "source"));
        config.addQuadMapping("mesh", "thesauruses", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:thesaurusID"),
                config.createLiteralMapping(rdfLangStringEn, "thesaurus"));
        config.addQuadMapping("mesh", "labels", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("rdfs:label"), config.createLiteralMapping(rdfLangStringEn, "label"));
        config.addQuadMapping("mesh", "abbreviations", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:abbreviation"),
                config.createLiteralMapping(rdfLangStringEn, "abbreviation"));
        config.addQuadMapping("mesh", "annotations", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:annotation"),
                config.createLiteralMapping(rdfLangStringEn, "annotation"));
        config.addQuadMapping("mesh", "casn1_labels", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:casn1_label"), config.createLiteralMapping(rdfLangStringEn, "label"));
        config.addQuadMapping("mesh", "consider_also_values", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:considerAlso"), config.createLiteralMapping(rdfLangStringEn, "value"));
        config.addQuadMapping("mesh", "entry_versions", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:entryVersion"), config.createLiteralMapping(rdfLangStringEn, "version"));
        config.addQuadMapping("mesh", "history_notes", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:historyNote"), config.createLiteralMapping(rdfLangStringEn, "note"));
        config.addQuadMapping("mesh", "last_active_years", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:lastActiveYear"), config.createLiteralMapping(rdfLangStringEn, "year"));
        config.addQuadMapping("mesh", "lexical_tags", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:lexicalTag"), config.createLiteralMapping(rdfLangStringEn, "tag"));
        config.addQuadMapping("mesh", "notese_notes", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:note"), config.createLiteralMapping(rdfLangStringEn, "note"));
        config.addQuadMapping("mesh", "online_notes", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:onlineNote"), config.createLiteralMapping(rdfLangStringEn, "note"));
        config.addQuadMapping("mesh", "pref_labels", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:prefLabel"), config.createLiteralMapping(rdfLangStringEn, "label"));
        config.addQuadMapping("mesh", "public_mesh_notes", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:publicMeSHNote"), config.createLiteralMapping(rdfLangStringEn, "note"));
        config.addQuadMapping("mesh", "scope_notes", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:scopeNote"), config.createLiteralMapping(rdfLangStringEn, "note"));
        config.addQuadMapping("mesh", "sort_versions", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:sortVersion"), config.createLiteralMapping(rdfLangStringEn, "version"));

        config.addQuadMapping("mesh", "related_registry_numbers", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:relatedRegistryNumber"),
                config.createLiteralMapping(xsdString, "number"));
        config.addQuadMapping("mesh", "identifiers", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:identifier"), config.createLiteralMapping(xsdString, "identifier"));
        config.addQuadMapping("mesh", "nlm_cassification_numbers", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:nlmClassificationNumber"),
                config.createLiteralMapping(xsdString, "number"));
        config.addQuadMapping("mesh", "registry_numbers", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:registryNumber"), config.createLiteralMapping(xsdString, "number"));

        config.addQuadMapping("mesh", "frequencies", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:frequency"), config.createLiteralMapping(xsdInt, "frequency"));

        config.addQuadMapping("mesh", "active_property", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:active"), config.createLiteralMapping(xsdBoolean, "value"));

        config.addQuadMapping("mesh", "created_dates", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:dateCreated"), config.createLiteralMapping(xsdDate, "date", "timezone"));
        config.addQuadMapping("mesh", "revised_dates", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:dateRevised"), config.createLiteralMapping(xsdDate, "date", "timezone"));
        config.addQuadMapping("mesh", "established_dates", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:dateEstablished"),
                config.createLiteralMapping(xsdDate, "date", "timezone"));

        config.addQuadMapping("mesh", "allowable_qualifiers", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:allowableQualifier"), config.createIriMapping(mesh, "qualifier"));
        config.addQuadMapping("mesh", "broader_concepts", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:broaderConcept"), config.createIriMapping(mesh, "concept"));
        config.addQuadMapping("mesh", "broader_descriptors", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:broaderDescriptor"), config.createIriMapping(mesh, "descriptor"));
        config.addQuadMapping("mesh", "broader_qualifiers", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:broaderQualifier"), config.createIriMapping(mesh, "qualifier"));
        config.addQuadMapping("mesh", "concepts", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:concept"), config.createIriMapping(mesh, "concept"));
        config.addQuadMapping("mesh", "indexer_consider_also_relations", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:indexerConsiderAlso"), config.createIriMapping(mesh, "value"));
        config.addQuadMapping("mesh", "mapped_to_relations", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:mappedTo"), config.createIriMapping(mesh, "value"));
        config.addQuadMapping("mesh", "narrower_concepts", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:narrowerConcept"), config.createIriMapping(mesh, "concept"));
        config.addQuadMapping("mesh", "pharmacological_actions", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:pharmacologicalAction"), config.createIriMapping(mesh, "action"));
        config.addQuadMapping("mesh", "preferred_mapped_to_relations", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:preferredMappedTo"), config.createIriMapping(mesh, "value"));
        config.addQuadMapping("mesh", "related_concepts", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:relatedConcept"), config.createIriMapping(mesh, "concept"));
        config.addQuadMapping("mesh", "see_also_relations", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:seeAlso"), config.createIriMapping(mesh, "reference"));
        config.addQuadMapping("mesh", "terms", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:term"), config.createIriMapping(mesh, "term"));
        config.addQuadMapping("mesh", "tree_numbers", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:treeNumber"), config.createIriMapping(mesh, "number"));
        config.addQuadMapping("mesh", "descriptors", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:hasDescriptor"), config.createIriMapping(mesh, "descriptor"));
        config.addQuadMapping("mesh", "qualifiers", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:hasQualifier"), config.createIriMapping(mesh, "qualifier"));
        config.addQuadMapping("mesh", "parent_tree_numbers", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:parentTreeNumber"), config.createIriMapping(mesh, "number"));
        config.addQuadMapping("mesh", "preferred_concept", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:preferredConcept"), config.createIriMapping(mesh, "concept"));
        config.addQuadMapping("mesh", "preferred_term", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:preferredTerm"), config.createIriMapping(mesh, "term"));
        config.addQuadMapping("mesh", "use_instead_relations", graph, config.createIriMapping(mesh, "mesh"),
                config.createIriMapping("meshv:useInstead"), config.createIriMapping(mesh, "value"));
    }
}
