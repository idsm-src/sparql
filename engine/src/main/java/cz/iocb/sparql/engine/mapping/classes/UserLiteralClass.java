package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUserType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.userType;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.UserType;
import cz.iocb.sparql.engine.mapping.datatypes.UserDatatype;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Canonical literals of one user datatype stored in its PostgreSQL user type.
 */
public final class UserLiteralClass extends CanonicalLiteralClass
{
    /**
     * PostgreSQL user type of the values.
     */
    protected UserType sqlType;

    /**
     * Base class keeping the lexical form.
     */
    protected LiteralClass base;


    /**
     * Creates the class over the user type with the given base class as a superclass.
     *
     * @param name the name
     * @param sqlType the PostgreSQL user type
     * @param datatype the user datatype
     * @param base the base class keeping the lexical form
     */
    public UserLiteralClass(String name, UserType sqlType, UserDatatype datatype, LiteralClass base)
    {
        super(name, datatype, List.of(sqlType), Set.of(box, genUserType, userType, base));

        this.sqlType = sqlType;
        this.base = base;
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(userType);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(datatype.getCanonicalLexicalForm(literal.getValue()), sqlTypes.get(0)));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column value = columns.get(0);
        Column lexical = constant("", VARCHAR);
        Column type = constant(datatype.getTypeIri().getValue(), VARCHAR);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_userliteral(%s, %s)", value, type));

        if(targetClass.equals(genUserType))
            return List.of(expression("sparql.ubox_create(%s)", value),
                    !canBeNull ? type : expression("CASE WHEN %s IS NOT NULL THEN %s END", value, type),
                    !canBeNull ? lexical : expression("CASE WHEN %s IS NOT NULL THEN %s END", value, lexical));

        if(targetClass.equals(userType))
            return List.of(expression("sparql.ubox_create(%s)", value),
                    !canBeNull ? type : expression("CASE WHEN %s IS NOT NULL THEN %s END", value, type));

        if(targetClass.equals(base))
            return List.of(value,
                    !canBeNull ? lexical : expression("CASE WHEN %s IS NOT NULL THEN %s END", value, lexical));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        Column type = constant(datatype.getTypeIri().getValue(), VARCHAR);

        if(sourceClass.equals(box))
            return List.of(expression("sparql.rdfbox_get_userliteral_typedvalue_of_type(%s, %s, NULL::%s, false)",
                    columns.get(0), type, sqlType));

        if(sourceClass.equals(genUserType))
            return List.of(
                    expression("(CASE WHEN %s = ''::varchar AND %s = %s THEN sparql.ubox_get_value(%s, NULL::%s) END)",
                            columns.get(2), columns.get(1), type, columns.get(0), sqlType));

        if(sourceClass.equals(userType))
            return List.of(expression("(CASE %s WHEN %s THEN sparql.ubox_get_value(%s, NULL::%s) END)", columns.get(1),
                    type, columns.get(0), sqlType));

        if(sourceClass.equals(base))
            return List.of(expression("(CASE %s WHEN ''::varchar THEN %s END)", columns.get(1), columns.get(0)));

        throw new IllegalArgumentException();
    }
}
