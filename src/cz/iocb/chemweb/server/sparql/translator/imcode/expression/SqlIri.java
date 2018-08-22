package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedIri;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlIri extends SqlNodeValue
{
    private final IRI iri;
    private final Set<PatternResourceClass> patternResourceClasses;


    protected SqlIri(IRI iri, IriClass iriClass, Set<ExpressionResourceClass> resourceClasses)
    {
        super(resourceClasses, false, false);
        this.iri = iri;
        this.patternResourceClasses = new HashSet<PatternResourceClass>();
        this.patternResourceClasses.add(iriClass);
    }


    public static SqlExpressionIntercode create(IRI iri, LinkedHashMap<String, UserIriClass> iriClasses)
    {
        IriClass iriClass = unsupportedIri;

        for(UserIriClass userClass : iriClasses.values())
            if(userClass.match(iri))
                iriClass = userClass;

        Set<ExpressionResourceClass> resourceClasses = new HashSet<ExpressionResourceClass>();
        resourceClasses.add(BuiltinClasses.iri);

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
        return BuiltinClasses.iri.getSqlValue(iri, 0);
    }


    @Override
    public Set<PatternResourceClass> getPatternResourceClasses()
    {
        return patternResourceClasses;
    }


    @Override
    public String nodeAccess(PatternResourceClass resourceClass, int part)
    {
        return resourceClass.getSqlValue(iri, part);
    }


    public IRI getIri()
    {
        return iri;
    }
}
