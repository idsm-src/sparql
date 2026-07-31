package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.areComparable;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.EQUAL;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.NOT_EQUAL;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical.LogicalOperator.AND;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical.LogicalOperator.OR;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonType;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical.LogicalOperator;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Multiset;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlInExpression extends SqlExpressionIntercode
{
    static private final class OperandWrapper extends SqlExpressionIntercode
    {
        private LinkedHashMap<Column, Column> columnMap;

        protected OperandWrapper(Map<ResourceClass, List<Column>> mappings, boolean canBeNull, boolean isDeterministic,
                LinkedHashMap<Column, Column> columnMap)
        {
            super(mappings, canBeNull, isDeterministic);

            this.columnMap = columnMap;
        }

        public static SqlExpressionIntercode create(SqlExpressionIntercode operand)
        {
            if(!operand.getBinding().hasExpressionColumn())
                return operand;

            LinkedHashMap<Column, Column> columnMap = new LinkedHashMap<>();

            for(Column c : operand.getBinding().getExpressionColumns())
                columnMap.put(c, new TableColumn("@col" + columnMap.size()));

            Map<ResourceClass, List<Column>> mapping = operand.getBinding().getMappings().entrySet().stream()
                    .collect(toMap(e -> e.getKey(), e -> e.getValue() == null ? null :
                            e.getValue().stream().map(c -> columnMap.getOrDefault(c, c)).collect(toList())));

            return new OperandWrapper(mapping, operand.canBeNull(), operand.isDeterministic(), columnMap);
        }

        @Override
        public Restrictions getRequirements()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
                boolean evalServices)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void generateExplanation(StringBuilder builder, String indent, int priority)
        {
            throw new UnsupportedOperationException();
        }

        public Map<Column, Column> getColumnMap()
        {
            return columnMap;
        }

        @Override
        protected int getHashCode()
        {
            return System.identityHashCode(this);
        }

        @Override
        public boolean equals(Object object)
        {
            return this == object;
        }
    }


    private boolean negated;
    private SqlExpressionIntercode left;
    private List<SqlExpressionIntercode> rights;


    protected SqlInExpression(boolean negated, SqlExpressionIntercode left, List<SqlExpressionIntercode> rights,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(mappings, canBeNull, left.isDeterministic() && rights.stream().allMatch(r -> r.isDeterministic()));

        this.negated = negated;
        this.left = left;
        this.rights = rights;

        this.referencedVariables.addAll(left.getReferencedVariables());

        for(SqlExpressionIntercode right : rights)
            this.referencedVariables.addAll(right.getReferencedVariables());
    }


    public static SqlExpressionIntercode create(boolean negated, SqlExpressionIntercode left,
            List<SqlExpressionIntercode> rights)
    {
        return create(negated, left, rights, Restriction.ALL);
    }


    public static SqlExpressionIntercode create(boolean negated, SqlExpressionIntercode left,
            List<SqlExpressionIntercode> rights, Restriction restriction)
    {
        if(rights.isEmpty() && negated)
            return trueValue;

        if(rights.isEmpty() && !negated)
            return falseValue;


        SqlExpressionIntercode wrappedLeft = OperandWrapper.create(left);
        ComparisonOperator compareOperator = negated ? NOT_EQUAL : EQUAL;
        LogicalOperator logicalOperator = negated ? AND : OR;

        SqlExpressionIntercode expression = negated ? trueValue : falseValue;

        for(SqlExpressionIntercode right : rights)
            expression = SqlBinaryLogical.create(logicalOperator, expression,
                    SqlBinaryComparison.create(compareOperator, wrappedLeft, right));

        if(expression instanceof SqlLiteral || expression instanceof SqlNull)
            return expression;

        if(!restriction.contains(xsdBoolean))
        {
            return new SqlInExpression(negated, left, rights, singletonMap(xsdBoolean, null), expression.canBeNull());
        }
        else if(wrappedLeft instanceof OperandWrapper wrapped)
        {
            StringBuilder builder = new StringBuilder();

            Map<Column, Column> columnMap = wrapped.getColumnMap();

            builder.append("(SELECT ");
            builder.append(expression.get(xsdBoolean).get(0));
            builder.append(" FROM (VALUES (");
            builder.append(columnMap.keySet().stream().map(c -> c.toString()).collect(joining(", ")));
            builder.append(")) AS \"tab\"(");
            builder.append(columnMap.keySet().stream().map(c -> c.toString()).collect(joining(", ")));
            builder.append("))");

            List<Column> result = List.of(new ExpressionColumn(builder.toString()));

            return new SqlInExpression(negated, left, rights, singletonMap(xsdBoolean, result), expression.canBeNull());
        }
        else
        {
            return new SqlInExpression(negated, left, rights, expression.getMappings(), expression.canBeNull());
        }
    }


    @Override
    public Restrictions getRequirements()
    {
        Restrictions restrictions = new Restrictions();

        restrictions.add(left.getRequirements());

        for(SqlExpressionIntercode argument : rights)
            restrictions.add(argument.getRequirements());

        return restrictions;
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        ComparisonOperator operator = negated ? NOT_EQUAL : EQUAL;

        Restriction leftSet = new Restriction();
        List<SqlExpressionIntercode> optRights = new ArrayList<>(rights.size());

        for(ResourceClass leftClass : left.getMappings().keySet())
        {
            for(SqlExpressionIntercode right : rights)
            {
                Restriction rightSet = new Restriction();
                boolean isComparable = false;

                for(ResourceClass rightClass : right.getMappings().keySet())
                {
                    if(areComparable(operator, leftClass, rightClass) != ComparisonType.NULL)
                    {
                        leftSet.add(leftClass);
                        rightSet.add(rightClass);
                        isComparable = true;
                    }
                }

                if(isComparable)
                    optRights.add(right.optimize(request, bindings, rightSet, evalServices));
            }
        }

        SqlExpressionIntercode optLeft = left.optimize(request, bindings, leftSet, evalServices);

        if(optRights.equals(rights) && optLeft == left)
            return this;

        return create(negated, optLeft, optRights, restriction);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        left.generateExplanation(builder, indent, 7);

        if(negated)
            builder.append(" not");

        builder.append(" in (");

        for(int i = 0; i < rights.size(); i++)
        {
            if(i > 0)
                builder.append(", ");

            rights.get(i).generateExplanation(builder, indent, 10);
        }

        builder.append(")");
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlInExpression imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(negated, imcode.negated))
            return false;

        if(!Objects.equals(left, imcode.left))
            return false;

        if(!Objects.equals(new Multiset<>(rights), new Multiset<>(imcode.rights)))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(negated, left, new Multiset<>(rights));
    }
}
