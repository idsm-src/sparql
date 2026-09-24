package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUserType;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.UserType;
import cz.iocb.sparql.engine.mapping.datatypes.UserDatatype;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Any valid literal of one user datatype stored in its PostgreSQL user type plus its lexical form.
 */
public final class UserLiteralBaseClass extends BaseLiteralClass
{
    /**
     * PostgreSQL user type of the values.
     */
    protected UserType sqlType;


    /**
     * Creates the class over the user type.
     *
     * @param name the name
     * @param sqlType the PostgreSQL user type
     * @param datatype the user datatype
     */
    public UserLiteralBaseClass(String name, UserType sqlType, UserDatatype datatype)
    {
        super(name, datatype, List.of(sqlType, VARCHAR), Set.of(box, genUserType));

        this.sqlType = sqlType;
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(genUserType);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(datatype.getCanonicalLexicalForm(literal.getValue()), sqlTypes.get(0)),
                getLexicalColumn(literal));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column value = columns.get(0);
        Column lexical = columns.get(1);
        Column type = constant(datatype.getTypeIri().getValue(), VARCHAR);

        if(targetClass.equals(box))
            return List.of(
                    expression("sparql.rdfbox_create_from_userliteral_with_lexical(%s, %s, %s)", value, type, lexical));

        if(targetClass.equals(genUserType))
            return List.of(expression("sparql.ubox_create(%s)", value),
                    !canBeNull ? type : expression("CASE WHEN %s IS NOT NULL THEN %s END", value, type), lexical);

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
            return List.of(
                    expression("sparql.rdfbox_get_userliteral_typedvalue_of_type(%s, %s, NULL::%s)", columns.get(0),
                            type, sqlType),
                    expression("sparql.rdfbox_get_userliteral_lexical_of_type(%s, %s)", columns.get(0), type));

        if(sourceClass.equals(genUserType))
            return List.of(
                    expression("(CASE %s WHEN %s THEN sparql.ubox_get_value(%s, NULL::%s) END)", columns.get(1), type,
                            columns.get(0), sqlType),
                    expression("(CASE %s WHEN %s THEN %s END)", columns.get(1), type, columns.get(2)));

        throw new IllegalArgumentException();
    }
}
