package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedIri;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlIri extends SqlNodeValue
{
    private final IRI iri;
    private final Set<ResourceClass> patternResourceClasses;


    protected SqlIri(IRI iri, IriClass iriClass, Set<ResourceClass> resourceClasses)
    {
        super(resourceClasses, false, false);
        this.iri = iri;
        this.patternResourceClasses = new HashSet<ResourceClass>();
        this.patternResourceClasses.add(iriClass);
    }


    public static SqlExpressionIntercode create(IRI iri, LinkedHashMap<String, UserIriClass> iriClasses)
    {
        IriClass iriClass = unsupportedIri;

        for(UserIriClass userClass : iriClasses.values())
            if(userClass.match(iri))
                iriClass = userClass;

        Set<ResourceClass> resourceClasses = new HashSet<ResourceClass>();
        resourceClasses.add(iriClass);

        return new SqlIri(iri, iriClass, resourceClasses);
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        return this;
    }


    @Override
    public String translate()
    {
        return BuiltinClasses.iri.getPatternCode(iri, 0);
    }


    @Override
    public String getNodeAccess(ResourceClass resourceClass, int part)
    {
        return resourceClass.getPatternCode(iri, part);
    }


    public IRI getIri()
    {
        return iri;
    }
}
