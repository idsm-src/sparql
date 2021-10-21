package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LANG;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LANGSTRING;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.rdfLangStringIri;
import static java.util.Arrays.asList;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class LangStringConstantTagClass extends LiteralClass
{
    private static final Hashtable<String, LangStringConstantTagClass> instances = new Hashtable<String, LangStringConstantTagClass>();

    private final String lang;


    private LangStringConstantTagClass(String lang)
    {
        super("lang-" + lang, asList("varchar"), asList(LANGSTRING, LANG), rdfLangStringIri);
        this.lang = lang;
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return rdfLangString;
    }


    public static LangStringConstantTagClass get(String lang)
    {
        lang = lang.toLowerCase();

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
    public List<Column> toColumns(Node node)
    {
        String code = "'" + ((String) ((Literal) node).getValue()).replaceAll("'", "''") + "'::varchar";
        return asList(new ConstantColumn(code));
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("CASE WHEN ");
        builder.append(columns.get(1));
        builder.append(" = '");
        builder.append(lang);
        builder.append("'::varchar THEN ");
        builder.append(columns.get(0));
        builder.append(" END");

        return asList(new ExpressionColumn(builder.toString()));
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        List<Column> result = new ArrayList<Column>(getGeneralClass().getColumnCount());

        result.add(columns.get(0));

        if(check == false)
        {
            result.add(new ConstantColumn("'" + lang + "'::varchar"));
        }
        else
        {
            StringBuilder builder = new StringBuilder();

            builder.append("CASE WHEN ");
            builder.append(columns.get(0));
            builder.append(" IS NOT NULL THEN '");
            builder.append(lang);
            builder.append("'::varchar END");

            result.add(new ExpressionColumn(builder.toString()));
        }

        return result;
    }


    @Override
    public List<Column> fromExpression(Column column, boolean isBoxed, boolean check)
    {
        if(isBoxed == false)
            throw new IllegalArgumentException();

        StringBuilder builder = new StringBuilder();

        if(check)
        {
            builder.append("CASE ");
            builder.append("sparql.rdfbox_extract_lang_string_lang" + "(" + column + ")");
            builder.append(" WHEN '");
            builder.append(getTag());
            builder.append("'::varchar THEN ");
            builder.append("sparql.rdfbox_extract_lang_string_string" + "(" + column + ")");
            builder.append(" END");
        }
        else
        {
            builder.append("sparql.rdfbox_extract_lang_string_string" + "(" + column + ")");
        }

        return asList(new ExpressionColumn(builder.toString()));
    }


    @Override
    public Column toExpression(Node node)
    {
        return new ConstantColumn("'" + ((String) ((Literal) node).getValue()).replaceAll("'", "''") + "'::varchar");
    }


    @Override
    public Column toExpression(List<Column> columns, boolean rdfbox)
    {
        if(!rdfbox)
            return columns.get(0);

        return new ExpressionColumn(
                "sparql.cast_as_rdfbox_from_lang_string(" + columns.get(0) + ", '" + lang + "'::varchar)");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        String code = "CASE WHEN " + columns.get(0) + " IS NOT NULL THEN '" + lang + "'::varchar END";

        return asList(columns.get(0), new ExpressionColumn(code));
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


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        LangStringConstantTagClass other = (LangStringConstantTagClass) object;

        if(!lang.equals(other.lang))
            return false;

        return true;
    }
}
