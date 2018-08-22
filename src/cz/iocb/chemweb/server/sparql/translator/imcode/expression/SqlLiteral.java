package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangStringExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteralExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.DatePatternClassWithConstantZone;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimePatternClassWithConstantZone;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringPatternClassWithConstantTag;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionLiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlLiteral extends SqlNodeValue
{
    private final Literal literal;
    private final Set<PatternResourceClass> patternResourceClasses;


    protected SqlLiteral(Literal literal, Set<ExpressionResourceClass> resourceClasses,
            Set<PatternResourceClass> patternResourceClasses, boolean isBoxed)
    {
        super(resourceClasses, isBoxed, false);
        this.literal = literal;
        this.patternResourceClasses = patternResourceClasses;
    }


    public static SqlExpressionIntercode create(Literal literal)
    {
        ExpressionResourceClass resourceClass = unsupportedLiteralExpr;

        if(literal.getLanguageTag() != null)
        {
            resourceClass = rdfLangStringExpr;
        }
        else
        {
            if(literal.getValue() != null)
            {
                IRI iri = literal.getTypeIri();

                if(iri != null)
                    for(ExpressionLiteralClass literalClass : BuiltinClasses.getExpressionLiteralClasses())
                        if(literalClass.getTypeIri().equals(iri))
                            resourceClass = literalClass;
            }
        }

        Set<ExpressionResourceClass> resourceClasses = new HashSet<ExpressionResourceClass>();
        resourceClasses.add(resourceClass);

        boolean isBoxed = resourceClass == rdfLangStringExpr || resourceClass == unsupportedLiteralExpr;


        PatternResourceClass patternResourceClass = resourceClass.getPatternResourceClass();

        if(patternResourceClass == rdfLangString)
            patternResourceClass = new LangStringPatternClassWithConstantTag(literal.getLanguageTag());
        else if(patternResourceClass == xsdDateTime)
            patternResourceClass = new DateTimePatternClassWithConstantZone(
                    ((OffsetDateTime) literal.getValue()).getOffset().getTotalSeconds());
        else if(patternResourceClass == xsdDate)
            patternResourceClass = new DatePatternClassWithConstantZone(
                    DatePatternClassWithConstantZone.getZone(literal));

        Set<PatternResourceClass> patternResourceClasses = new HashSet<PatternResourceClass>();
        patternResourceClasses.add(patternResourceClass);


        return new SqlLiteral(literal, resourceClasses, patternResourceClasses, isBoxed);
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        return this;
    }


    @Override
    public String translate()
    {
        ExpressionLiteralClass resourceClass = (ExpressionLiteralClass) getResourceClasses().iterator().next();

        return resourceClass.getSqlLiteralValue(literal);
    }


    public Literal getLiteral()
    {
        return literal;
    }


    @Override
    public Set<PatternResourceClass> getPatternResourceClasses()
    {
        return patternResourceClasses;
    }


    @Override
    public String nodeAccess(PatternResourceClass resourceClass, int part)
    {
        return resourceClass.getSqlValue(literal, part);
    }
}
