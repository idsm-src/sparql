package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LANG;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LANGSTRING;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.rdfLangStringIri;
import static java.util.Arrays.asList;
import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class LangStringClass extends LiteralClass
{
    LangStringClass()
    {
        super("lang", asList("varchar", "varchar"), asList(LANGSTRING, LANG), rdfLangStringIri);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return rdfLangString;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        Literal literal = (Literal) node;

        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ConstantColumn("'" + ((String) literal.getValue()).replaceAll("'", "''") + "'::varchar"));
        result.add(new ConstantColumn("'" + literal.getLanguageTag() + "'::varchar"));

        return result;
    }


    @Override
    public List<Column> fromExpression(Column column, boolean isBoxed, boolean check)
    {
        if(isBoxed == false)
            throw new IllegalArgumentException();

        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ExpressionColumn("sparql.rdfbox_extract_lang_string_string(" + column + ")"));
        result.add(new ExpressionColumn("sparql.rdfbox_extract_lang_string_lang(" + column + ")"));

        return result;
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        return columns;
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        return columns;
    }


    @Override
    public Column toExpression(Node node)
    {
        Literal literal = (Literal) node;

        return new ExpressionColumn(
                "sparql.cast_as_rdfbox_from_lang_string('" + ((String) literal.getValue()).replaceAll("'", "''")
                        + "'::varchar, '" + literal.getLanguageTag() + "'::varchar)");
    }


    @Override
    public Column toExpression(List<Column> columns, boolean rdfbox)
    {
        return new ExpressionColumn(
                "sparql.cast_as_rdfbox_from_lang_string(" + columns.get(0) + ", " + columns.get(1) + ")");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return columns;
    }
}
