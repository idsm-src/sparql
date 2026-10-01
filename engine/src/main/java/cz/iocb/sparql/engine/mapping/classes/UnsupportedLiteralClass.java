package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.request.Request;



/**
 * Literals of datatypes unknown to the configuration, or with an invalid lexical form, stored as the lexical value and
 * the datatype IRI text. A literal matches it when the classification by the request ({@link Request#getLiteralClass})
 * yields this class.
 */
public final class UnsupportedLiteralClass extends BaseLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected UnsupportedLiteralClass()
    {
        super("literal", null, List.of(VARCHAR, VARCHAR), Set.of(box));
    }


    @Override
    public ResourceClass getResultResourceClass()
    {
        return this;
    }


    @Override
    public boolean match(Request request, Literal literal)
    {
        return equals(request.getLiteralClass(literal));
    }


    @Override
    public List<Column> toColumns(Request request, Literal literal)
    {
        return List.of(constant(literal.getValue(), VARCHAR), constant(literal.getType().getValue(), VARCHAR));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column value = columns.get(0);
        Column type = columns.get(1);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_typedliteral(%s, %s)", value, type));

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
            return List.of(expression("sparql.rdfbox_get_typedliteral_value(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_typedliteral_type(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }
}
