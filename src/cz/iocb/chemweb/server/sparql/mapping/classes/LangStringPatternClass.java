package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.LANG;
import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.LANGSTRING;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.rdfLangStringIri;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.PatternLiteralBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.LangStringClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class LangStringPatternClass extends PatternLiteralBaseClass implements LangStringClass
{
    LangStringPatternClass()
    {
        super("lang", Arrays.asList("varchar", "varchar"), Arrays.asList(LANGSTRING, LANG), rdfLangStringIri);
    }


    @Override
    public String getResultValue(String variable, int part)
    {
        return getSqlColumn(variable, part);
    }


    @Override
    public String getSqlPatternLiteralValue(Literal literal, int part)
    {
        if(part == 0)
            return "'" + ((String) literal.getValue()).replaceAll("'", "''") + "'::varchar";
        else
            return "'" + literal.getLanguageTag() + "'::varchar";
    }


    @Override
    public ExpressionResourceClass getExpressionResourceClass()
    {
        return BuiltinClasses.rdfLangStringExpr;
    }


    @Override
    public String getSqlExpressionValue(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        return "sparql.cast_as_rdfbox_from_lang_string(" + variableAccessor.variableAccess(variable, this, 0) + ", "
                + variableAccessor.variableAccess(variable, this, 1) + ")";
    }
}
