package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.UBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUserType;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Canonical literals of any user datatype: a {@code sparql.ubox} value and the datatype IRI; the result class of
 * canonical user literals. Not usable for matching constants.
 */
public final class UserLiteralCompositeClass extends BaseLiteralClass implements ResultResourceClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected UserLiteralCompositeClass()
    {
        super("user", null, List.of(UBOX, VARCHAR), Set.of(box, genUserType));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(this);
    }


    @Override
    public boolean match(Statement statement, Literal literal)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        throw new IllegalArgumentException();
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
            return List.of(expression("sparql.rdfbox_create_from_userliteral(%s, %s)", value, type));

        if(targetClass.equals(genUserType))
            return List.of(value, type, !canBeNull ? constant("", VARCHAR) :
                    expression("CASE WHEN %s IS NOT NULL THEN ''::varchar END", value));

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
            return List.of(expression("sparql.rdfbox_get_userliteral_value(%s, false)", columns.get(0)),
                    expression("sparql.rdfbox_get_userliteral_type(%s, false)", columns.get(0)));

        if(sourceClass.equals(genUserType))
            return List.of(expression("(CASE %s WHEN '' THEN %s END)", columns.get(2), columns.get(0)),
                    expression("(CASE %s WHEN '' THEN %s END)", columns.get(2), columns.get(1)));

        throw new IllegalArgumentException();
    }
}
