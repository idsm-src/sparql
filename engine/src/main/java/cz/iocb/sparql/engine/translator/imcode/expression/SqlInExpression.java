package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Multiset;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public final class SqlInExpression extends SqlExpressionIntercode
{
    static private final class OperandWrapper extends SqlExpressionIntercode
    {
        private SqlExpressionIntercode operand;

        protected OperandWrapper(SqlExpressionIntercode operand)
        {
            super(operand.getResourceClasses(), operand.canBeNull(), operand.isDeterministic());
            this.operand = operand;

            this.referencedVariables.addAll(operand.getReferencedVariables());
        }

        public static SqlExpressionIntercode create(SqlExpressionIntercode operand)
        {
            if(operand instanceof SqlNodeValue)
                return operand;

            return new OperandWrapper(operand);
        }

        @Override
        public Restrictions getRequirements(Set<ResourceClass> expected)
        {
            return operand.getRequirements(expected);
        }

        @Override
        public SqlExpressionIntercode optimize(Request request, UsedVariables variables, boolean evalServices)
        {
            return create(operand.optimize(request, variables, evalServices));
        }

        @Override
        public String translate(Request request)
        {
            return "\"expr\"";
        }

        @Override
        public void generateExplanation(StringBuilder builder, String indent, int priority)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object object)
        {
            if(this == object)
                return true;

            if(!(object instanceof OperandWrapper imcode))
                return false;

            if(!super.equals(imcode))
                return false;

            if(!Objects.equals(operand, imcode.operand))
                return false;

            return true;
        }

        @Override
        protected int getHashCode()
        {
            return Objects.hash(operand);
        }
    }


    private boolean negated;
    private SqlExpressionIntercode left;
    private List<SqlExpressionIntercode> rights;
    private SqlExpressionIntercode expression;


    protected SqlInExpression(boolean negated, SqlExpressionIntercode left, List<SqlExpressionIntercode> rights,
            SqlExpressionIntercode expression)
    {
        super(asSet(xsdBoolean), expression.canBeNull(),
                left.isDeterministic() && rights.stream().allMatch(r -> r.isDeterministic()));

        this.negated = negated;
        this.left = left;
        this.rights = rights;
        this.expression = expression;

        this.referencedVariables.addAll(left.getReferencedVariables());

        for(SqlExpressionIntercode right : rights)
            this.referencedVariables.addAll(right.getReferencedVariables());
    }


    public static SqlExpressionIntercode create(boolean negated, SqlExpressionIntercode left,
            List<SqlExpressionIntercode> rights)
    {
        if(rights.isEmpty() && negated)
            return trueValue;

        if(rights.isEmpty() && !negated)
            return falseValue;


        SqlExpressionIntercode expression = null;

        SqlExpressionIntercode wrappedLeft = OperandWrapper.create(left);
        Operator compareOperator = negated ? Operator.NotEquals : Operator.Equals;
        Operator logicalOperator = negated ? Operator.And : Operator.Or;

        for(SqlExpressionIntercode right : rights)
        {
            SqlExpressionIntercode compare = SqlBinaryComparison.create(compareOperator, wrappedLeft, right);
            expression = expression != null ? SqlBinaryLogical.create(logicalOperator, expression, compare) : compare;
        }

        if(expression instanceof SqlEffectiveBooleanValue || expression instanceof SqlNull)
            return expression;

        return new SqlInExpression(negated, left, rights, expression);
    }


    @Override
    public Restrictions getRequirements(Set<ResourceClass> expected)
    {
        return new Restrictions(expression.getRequirements(expected));
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, boolean evalServices)
    {
        SqlExpressionIntercode optLeft = left.optimize(request, variables, evalServices);

        List<SqlExpressionIntercode> optRights = new LinkedList<SqlExpressionIntercode>();

        for(SqlExpressionIntercode right : rights)
            optRights.add(right.optimize(request, variables, evalServices));


        if(optRights.equals(rights) && optLeft == left)
            return this;

        return create(negated, optLeft, optRights);
    }


    @Override
    public String translate(Request request)
    {
        if(left instanceof SqlNodeValue)
            return expression.translate(request);


        StringBuilder builder = new StringBuilder();
        builder.append("(SELECT ");
        builder.append(expression.translate(request));
        builder.append(" FROM (VALUES (");
        builder.append(left.translate(request));
        builder.append(")) AS \"tab\"(\"expr\"))");
        return builder.toString();
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
