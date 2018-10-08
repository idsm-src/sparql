package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LANG;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LANGSTRING;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.rdfLangStringIri;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class LangStringClass extends LiteralClass
{
    LangStringClass()
    {
        super("lang", Arrays.asList("varchar", "varchar"), Arrays.asList(LANGSTRING, LANG), rdfLangStringIri);
    }


    @Override
    public String getLiteralPatternCode(Literal literal, int part)
    {
        if(part == 0)
            return "'" + ((String) literal.getValue()).replaceAll("'", "''") + "'::varchar";
        else
            return "'" + literal.getLanguageTag() + "'::varchar";
    }


    @Override
    public String getPatternCode(String column, int part, boolean isBoxed)
    {
        if(isBoxed == false)
            throw new IllegalArgumentException();

        return "sparql.rdfbox_extract_lang_string_" + (part == 0 ? "string" : "lang") + "(" + column + ")";
    }


    @Override
    public String getExpressionCode(Literal literal)
    {
        return "sparql.cast_as_rdfbox_from_lang_string('" + ((String) literal.getValue()).replaceAll("'", "''")
                + "'::varchar, '" + literal.getLanguageTag() + "'::varchar)";
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        return "sparql.cast_as_rdfbox_from_lang_string(" + variableAccessor.getSqlVariableAccess(variable, this, 0)
                + ", " + variableAccessor.getSqlVariableAccess(variable, this, 1) + ")";
    }


    @Override
    public String getResultCode(String variable, int part)
    {
        return getSqlColumn(variable, part);
    }
}
