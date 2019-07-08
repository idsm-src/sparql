package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LANG;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LANGSTRING;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.rdfLangStringIri;
import java.util.Arrays;
import java.util.Hashtable;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class LangStringConstantTagClass extends LiteralClass
{
    private static final Hashtable<String, LangStringConstantTagClass> instances = new Hashtable<String, LangStringConstantTagClass>();

    private final String lang;


    LangStringConstantTagClass(String lang)
    {
        super("lang-" + lang, Arrays.asList("varchar"), Arrays.asList(LANGSTRING, LANG), rdfLangStringIri);
        this.lang = lang;
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return rdfLangString;
    }


    public static LangStringConstantTagClass get(String lang)
    {
        LangStringConstantTagClass instance = instances.get(lang);

        if(instance == null)
        {
            synchronized(instances)
            {
                instance = instances.get(lang);

                if(instance == null)
                {
                    instance = new LangStringConstantTagClass(lang);
                    instances.put(lang, instance);
                }
            }
        }

        return instance;
    }


    @Override
    public String getLiteralPatternCode(Literal literal, int part)
    {
        return "'" + ((String) literal.getValue()).replaceAll("'", "''") + "'::varchar";
    }


    @Override
    public String getGeneralisedPatternCode(String table, String var, int part, boolean check)
    {
        if(part == 0)
        {
            return (table != null ? table + "." : "") + getSqlColumn(var, part);
        }
        else if(check == false)
        {
            return "'" + lang + "'::varchar";
        }
        else
        {
            StringBuilder builder = new StringBuilder();

            builder.append("CASE WHEN ");

            if(table != null)
                builder.append(table).append(".");

            builder.append(getSqlColumn(var, part));
            builder.append(" IS NOT NULL THEN '");
            builder.append(lang);
            builder.append("'::varchar END");

            return builder.toString();
        }
    }


    @Override
    public String getSpecialisedPatternCode(String table, String var, int part)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("CASE WHEN ");

        if(table != null)
            builder.append(table).append(".");

        builder.append(rdfLangString.getSqlColumn(var, 1));
        builder.append(" = '");
        builder.append(lang);
        builder.append("'::varchar THEN ");

        if(table != null)
            builder.append(table).append(".");

        builder.append(rdfLangString.getSqlColumn(var, 0));
        builder.append(" END");

        return builder.toString();
    }


    @Override
    public String getPatternCode(String column, int part, boolean isBoxed)
    {
        if(isBoxed == false)
            throw new IllegalArgumentException();

        return "sparql.rdfbox_extract_lang_string_string" + "(" + column + ")";
    }


    @Override
    public String getExpressionCode(Literal literal)
    {
        return "'" + ((String) literal.getValue()).replaceAll("'", "''") + "'::varchar";
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        String code = variableAccessor.getSqlVariableAccess(variable, this, 0);

        if(rdfbox)
            code = "sparql.cast_as_rdfbox_from_lang_string(" + code + ", '" + lang + "'::varchar)";

        return code;
    }


    @Override
    public String getResultCode(String variable, int part)
    {
        if(part == 0)
            return getSqlColumn(variable, part);
        else
            return "CASE WHEN " + getSqlColumn(variable, 0) + "IS NOT NULL THEN '" + lang + "'::varchar END";
    }


    @Override
    public boolean match(Node node, Request request)
    {
        if(!super.match(node, request))
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
