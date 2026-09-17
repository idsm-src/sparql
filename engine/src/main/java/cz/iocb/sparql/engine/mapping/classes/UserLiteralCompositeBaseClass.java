package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Literal;



public final class UserLiteralCompositeBaseClass extends BaseLiteralClass implements ResultResourceClass
{
    protected UserLiteralCompositeBaseClass()
    {
        super("base-user", null, List.of("sparql.ubox", "varchar", "varchar"), Set.of(box));
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
        Column lexical = columns.get(2);

        if(targetClass.equals(box))
            return List.of(
                    expression("sparql.rdfbox_create_from_userliteral_with_lexical(%s, %s, %s)", value, type, lexical));

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
            return List.of(expression("sparql.rdfbox_get_userliteral_value(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_usertype_type(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_usertype_lexical(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }
}
