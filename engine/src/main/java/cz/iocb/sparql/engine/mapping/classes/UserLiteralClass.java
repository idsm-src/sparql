package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.sparql.engine.parser.model.expression.Literal;



public class UserLiteralClass extends LiteralClass
{
    private final String equalOperator;
    private final String notEqualOperator;


    private UserLiteralClass(String name, String sqlType, String equalOp, String notEqualOp, IRI type)
    {
        super(name, type, List.of(sqlType), Set.of(box));
        this.equalOperator = equalOp;
        this.notEqualOperator = notEqualOp;
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(unsupportedLiteral);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(literal.getValue(), sqlTypes.get(0)));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column value = columns.get(0);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_typedliteral((%s)::varchar, %s::varchar)", value,
                    string(typeIri)));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        if(sourceClass.equals(box))
            return List.of(expression("sparql.rdfbox_get_typedliteral_value_of_type(%s, %s::varchar)::%s",
                    columns.get(0), string(typeIri), sqlTypes.get(0)));

        throw new IllegalArgumentException();
    }


    public String getOperatorCode(Operator operator)
    {
        return switch(operator)
        {
            case Equals -> equalOperator;
            case NotEquals -> notEqualOperator;
            default -> throw new IllegalArgumentException();
        };
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        UserLiteralClass other = (UserLiteralClass) object;

        return Objects.equals(equalOperator, other.equalOperator)
                && Objects.equals(notEqualOperator, other.notEqualOperator);
    }
}
