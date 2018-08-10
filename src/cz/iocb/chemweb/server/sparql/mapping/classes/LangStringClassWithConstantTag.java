package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LANG;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LANGSTRING;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.rdfLangStringIri;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class LangStringClassWithConstantTag extends LiteralClass
{
    private final String lang;


    public LangStringClassWithConstantTag(String lang)
    {
        super("lang-" + lang, Arrays.asList("varchar"), Arrays.asList(LANGSTRING, LANG), rdfLangStringIri);
        this.lang = lang;
    }


    @Override
    public String getResultValue(String variable, int part)
    {
        if(part == 0)
            return getSqlColumn(variable, part);
        else
            return "'" + lang + "'";
    }


    @Override
    public String getSqlLiteralValue(Literal literal, int part)
    {
        return "'" + ((String) literal.getValue()).replaceAll("'", "''") + "'";
    }


    @Override
    public boolean match(Node node)
    {
        if(!super.match(node))
            return false;

        if(!(node instanceof Literal))
            return true;

        if(!lang.equals(((Literal) node).getLanguageTag()))
            return false;

        return true;
    }
}
