package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfLangStringType;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.request.Request;



/**
 * Language-tagged strings stored as value and tag columns; the result class of all language-tagged strings.
 */
public final class LangStringClass extends CanonicalLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected LangStringClass()
    {
        super("lang", rdfLangStringType, List.of(VARCHAR, VARCHAR), Set.of(box));
    }


    @Override
    public ResourceClass getResultResourceClass()
    {
        return this;
    }


    @Override
    public boolean match(Request request, Literal literal)
    {
        if(!super.match(request, literal))
            return false;

        return literal instanceof LangStringLiteral;
    }


    @Override
    public List<Column> toColumns(Request request, Literal literal)
    {
        LangStringLiteral langLiteral = (LangStringLiteral) literal;

        return List.of(constant(langLiteral.getValue(), VARCHAR), constant(langLiteral.getTag(), VARCHAR));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column string = columns.get(0);
        Column lang = columns.get(1);

        if(targetClass.equals(box))
            return List.of(expression(RDFBOX, "sparql.rdfbox_create_from_langstring(%s, %s)", string, lang));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        if(sourceClass.equals(box))
            return List.of(expression(VARCHAR, "sparql.rdfbox_get_langstring_value(%s)", columns.get(0)),
                    expression(VARCHAR, "sparql.rdfbox_get_langstring_lang(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }
}
