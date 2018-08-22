package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.rdfLangStringIri;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.ExpressionResourceBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.LangStringClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public class LangStringExpressionClass extends ExpressionResourceBaseClass implements LangStringClass
{
    LangStringExpressionClass()
    {
        super("lang_string");
    }


    @Override
    public IRI getTypeIri()
    {
        return rdfLangStringIri;
    }


    @Override
    public String getSqlLiteralValue(Literal literal)
    {
        return "sparql.cast_as_rdfbox_from_lang_string('" + ((String) literal.getValue()).replaceAll("'", "''")
                + "'::varchar, '" + literal.getLanguageTag() + "'::varchar)";
    }


    @Override
    public PatternResourceClass getPatternResourceClass()
    {
        return rdfLangString;
    }


    @Override
    public String getSqlPatternValue(String column, int part, boolean isBoxed)
    {
        if(isBoxed == false)
            throw new IllegalArgumentException();

        return "sparql.rdfbox_extract_lang_string_" + (part == 0 ? "string" : "lang") + "(" + column + ")";
    }
}
