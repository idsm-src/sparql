package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.LANG;
import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.LANGSTRING;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.rdfLangStringIri;
import java.util.Arrays;
import java.util.Hashtable;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.PatternLiteralBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.LangStringClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class LangStringPatternClassWithConstantTag extends PatternLiteralBaseClass implements LangStringClass
{
    private static final Hashtable<String, LangStringPatternClassWithConstantTag> instances = new Hashtable<String, LangStringPatternClassWithConstantTag>();

    private final String lang;


    LangStringPatternClassWithConstantTag(String lang)
    {
        super("lang-" + lang, Arrays.asList("varchar"), Arrays.asList(LANGSTRING, LANG), rdfLangStringIri);
        this.lang = lang;
    }


    public static LangStringPatternClassWithConstantTag get(String lang)
    {
        LangStringPatternClassWithConstantTag instance = instances.get(lang);

        if(instance == null)
        {
            synchronized(instances)
            {
                instance = instances.get(lang);

                if(instance == null)
                {
                    instance = new LangStringPatternClassWithConstantTag(lang);
                    instances.put(lang, instance);
                }
            }
        }

        return instance;
    }


    @Override
    public String getResultValue(String variable, int part)
    {
        if(part == 0)
            return getSqlColumn(variable, part);
        else
            return "'" + lang + "'::varchar";
    }


    @Override
    public String getSqlPatternLiteralValue(Literal literal, int part)
    {
        return "'" + ((String) literal.getValue()).replaceAll("'", "''") + "'::varchar";
    }


    @Override
    public ExpressionResourceClass getExpressionResourceClass()
    {
        return BuiltinClasses.rdfLangStringExpr;
    }


    @Override
    public String getSqlExpressionValue(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        return "sparql.cast_as_rdfbox_from_lang_string(" + variableAccessor.variableAccess(variable, this, 0) + ", '"
                + lang + "')";
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


    public String getTag()
    {
        return lang;
    }
}
