package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.intBlankNode;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.strBlankNode;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedIri;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDayTimeDuration;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue.falseValue;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue.trueValue;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.mapping.classes.BlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntBlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.StrBlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIntBlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserStrBlankNodeClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.chemweb.server.sparql.translator.Pair;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlBinaryComparison extends SqlBinary
{
    private final Operator operator;
    private final boolean isAlwaysDifferentIfNotNull;
    private final List<Pair<ResourceClass, ResourceClass>> comparable;
    private final List<Pair<ResourceClass, ResourceClass>> different;


    public SqlBinaryComparison(Operator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
            boolean canBeNull, boolean isAlwaysDifferentIfNotNull, List<Pair<ResourceClass, ResourceClass>> comparable,
            List<Pair<ResourceClass, ResourceClass>> different)
    {
        super(left, right, asSet(xsdBoolean), canBeNull);
        this.operator = operator;
        this.isAlwaysDifferentIfNotNull = isAlwaysDifferentIfNotNull;
        this.comparable = comparable;
        this.different = different;
    }


    public static SqlExpressionIntercode create(Operator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        List<Pair<ResourceClass, ResourceClass>> comparable = new LinkedList<Pair<ResourceClass, ResourceClass>>();
        List<Pair<ResourceClass, ResourceClass>> different = new LinkedList<Pair<ResourceClass, ResourceClass>>();

        boolean isAlwaysNull = true;
        boolean isAlwaysDifferentIfNotNull = !left.canBeNull() && !right.canBeNull();
        boolean canBeNull = left.canBeNull() || right.canBeNull();


        for(ResourceClass leftClass : left.getResourceClasses())
        {
            for(ResourceClass rightClass : right.getResourceClasses())
            {
                if(leftClass == xsdBoolean && rightClass == xsdBoolean
                        || leftClass == xsdString && rightClass == xsdString
                        || isNumeric(leftClass) && isNumeric(rightClass)
                        || isDateTime(leftClass) && isDateTime(rightClass) || isDate(leftClass) && isDate(rightClass))
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(operator != Operator.Equals && operator != Operator.NotEquals)
                {
                    // is always null
                    canBeNull = true;
                }
                else if(leftClass == xsdDayTimeDuration && rightClass == xsdDayTimeDuration)
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(isLangString(leftClass) && isLangString(rightClass))
                {
                    if(leftClass instanceof LangStringConstantTagClass
                            && rightClass instanceof LangStringConstantTagClass
                            && !((LangStringConstantTagClass) leftClass).getTag()
                                    .equals(((LangStringConstantTagClass) rightClass).getTag()))
                    {
                        // is always different
                        isAlwaysNull = false;
                        different.add(new Pair<>(leftClass, rightClass));
                    }
                    else
                    {
                        isAlwaysNull = false;
                        isAlwaysDifferentIfNotNull = false;
                        comparable.add(new Pair<>(leftClass, rightClass));
                    }
                }
                else if(leftClass == unsupportedLiteral && rightClass == unsupportedLiteral)
                {
                    // is null, if types are different
                    canBeNull = true;
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(leftClass instanceof LiteralClass && rightClass instanceof LiteralClass)
                {
                    // is always null
                    canBeNull = true;
                }
                else if(leftClass instanceof BlankNodeClass && rightClass instanceof BlankNodeClass)
                {
                    if(leftClass == rightClass)
                    {
                        isAlwaysNull = false;
                        isAlwaysDifferentIfNotNull = false;
                        comparable.add(new Pair<>(leftClass, rightClass));
                    }
                    else
                    {
                        // is always different
                        isAlwaysNull = false;
                        different.add(new Pair<>(leftClass, rightClass));
                    }
                }
                else if(leftClass instanceof IriClass && rightClass instanceof IriClass)
                {
                    if(leftClass instanceof UserIriClass && rightClass instanceof UserIriClass
                            && leftClass != rightClass)
                    {
                        // is always different
                        isAlwaysNull = false;
                        different.add(new Pair<>(leftClass, rightClass));
                    }
                    else if((leftClass == unsupportedIri || rightClass == unsupportedIri)
                            && (leftClass instanceof UserIriClass || rightClass instanceof UserIriClass))
                    {
                        // is always different
                        isAlwaysNull = false;
                        different.add(new Pair<>(leftClass, rightClass));
                    }
                    else
                    {
                        isAlwaysNull = false;
                        isAlwaysDifferentIfNotNull = false;
                        comparable.add(new Pair<>(leftClass, rightClass));
                    }
                }
                else
                {
                    // is always different
                    isAlwaysNull = false;
                    different.add(new Pair<>(leftClass, rightClass));
                }
            }
        }


        if(isAlwaysNull)
            return SqlNull.get();

        if(operator == Operator.Equals && !canBeNull && isAlwaysDifferentIfNotNull)
            return falseValue;

        if(operator == Operator.NotEquals && !canBeNull && isAlwaysDifferentIfNotNull)
            return trueValue;

        return new SqlBinaryComparison(operator, left, right, canBeNull, isAlwaysDifferentIfNotNull, comparable,
                different);
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        SqlExpressionIntercode left = getLeft().optimize(variableAccessor);
        SqlExpressionIntercode right = getRight().optimize(variableAccessor);
        return create(operator, left, right);
    }


    @Override
    public String translate()
    {
        if(getLeft() instanceof SqlNodeValue && getRight() instanceof SqlNodeValue)
            return translateAsNodeComparison();


        StringBuilder builder = new StringBuilder();

        if(different.size() == getLeft().getResourceClasses().size() * getRight().getResourceClasses().size())
        {
            assert comparable.size() == 0;
            assert operator == Operator.Equals || operator == Operator.NotEquals;

            builder.append("NULLIF(");

            if(getLeft().canBeNull())
                builder.append(translateAsNullCheck(getLeft(), operator == Operator.NotEquals));

            if(getLeft().canBeNull() && getRight().canBeNull())
                builder.append(operator == Operator.Equals ? " OR " : " AND ");

            if(getRight().canBeNull())
                builder.append(translateAsNullCheck(getRight(), operator == Operator.NotEquals));

            builder.append(operator == Operator.Equals ? ", true)" : ", false)");

            return builder.toString();
        }
        else if(different.size() == 0 && (!getLeft().isBoxed() || !getRight().isBoxed()))
        {
            Set<ResourceClass> cmpClasses = comparable.stream()
                    .map(r -> determineComparisonClass(r.getKey(), r.getValue())).collect(Collectors.toSet());

            if(cmpClasses.size() == 1)
            {
                ResourceClass cmpClass = cmpClasses.iterator().next();

                //TODO: add other optimized variants

                if(cmpClass != rdfLangString && cmpClass != unsupportedLiteral)
                {
                    builder.append("sparql.");
                    builder.append(operator.getName());
                    builder.append("_");
                    builder.append(cmpClass.getName());
                    builder.append("(");
                    builder.append(translateAsUnboxedOperand(getLeft(), cmpClass));
                    builder.append(", ");
                    builder.append(translateAsUnboxedOperand(getRight(), cmpClass));
                    builder.append(")");

                    return builder.toString();
                }
            }
        }


        Set<ResourceClass> leftSet = new HashSet<ResourceClass>();
        leftSet.addAll(comparable.stream().map(r -> r.getKey()).collect(Collectors.toSet()));
        leftSet.addAll(different.stream().map(r -> r.getKey()).collect(Collectors.toSet()));

        Set<ResourceClass> rightSet = new HashSet<ResourceClass>();
        rightSet.addAll(comparable.stream().map(r -> r.getValue()).collect(Collectors.toSet()));
        rightSet.addAll(different.stream().map(r -> r.getValue()).collect(Collectors.toSet()));

        builder.append("sparql.");
        builder.append(operator.getName());
        builder.append("_rdfbox");
        builder.append("(");
        builder.append(translateAsBoxedOperand(getLeft(), leftSet));
        builder.append(", ");
        builder.append(translateAsBoxedOperand(getRight(), rightSet));
        builder.append(")");

        return builder.toString();
    }


    public String translateAsNodeComparison()
    {
        SqlNodeValue leftNode = (SqlNodeValue) getLeft();
        SqlNodeValue rightNode = (SqlNodeValue) getRight();

        StringBuilder builder = new StringBuilder();
        boolean hasAlternative = false;

        if(comparable.size() + different.size() > 1)
            builder.append("COALESCE(");


        for(Pair<ResourceClass, ResourceClass> pair : comparable)
        {
            appendComma(builder, hasAlternative);
            hasAlternative = true;

            ResourceClass leftClass = pair.getKey();
            ResourceClass rightClass = pair.getValue();


            if(leftClass instanceof BlankNodeClass)
            {
                String left = leftNode.getNodeAccess(leftClass, 0);
                String right = rightNode.getNodeAccess(rightClass, 0);

                if(leftClass != rightClass)
                {
                    if(leftClass instanceof UserIntBlankNodeClass)
                        left = "sparql.int_blanknode_create('" + ((UserIntBlankNodeClass) leftClass).getSegment()
                                + "'::int4, " + left + ")";
                    else if(rightClass instanceof UserIntBlankNodeClass)
                        right = "sparql.int_blanknode_create('" + ((UserIntBlankNodeClass) rightClass).getSegment()
                                + "'::int4, " + right + ")";
                    else if(leftClass instanceof UserStrBlankNodeClass)
                        left = "sparql.str_blanknode_create('" + ((UserStrBlankNodeClass) leftClass).getSegment()
                                + "'::int4, " + left + ")";
                    else if(rightClass instanceof UserStrBlankNodeClass)
                        right = "sparql.str_blanknode_create('" + ((UserStrBlankNodeClass) rightClass).getSegment()
                                + "'::int4, " + right + ")";

                    ResourceClass cmpClass = determineComparisonClass(leftClass, rightClass);
                    builder.append("sparql." + operator.getName() + "_" + cmpClass.getName());
                    builder.append("(" + left + ", " + right + ")");
                }
                else if(operator == Operator.Equals)
                {
                    builder.append("(" + left + " = " + right + ")");
                }
                else if(operator == Operator.NotEquals)
                {
                    builder.append("(" + left + " != " + right + ")");
                }
            }
            else if(leftClass == xsdBoolean || leftClass == xsdString || leftClass == xsdDayTimeDuration
                    || isNumeric(leftClass))
            {
                String left = leftNode.getNodeAccess(leftClass, 0);
                String right = rightNode.getNodeAccess(rightClass, 0);

                ResourceClass cmpClass = determineComparisonClass(leftClass, rightClass);

                if(leftClass != cmpClass)
                    left = "sparql.cast_as_" + cmpClass.getName() + "_from_" + leftClass.getName() + "(" + left + ")";

                if(rightClass != cmpClass)
                    right = "sparql.cast_as_" + cmpClass.getName() + "_from_" + rightClass.getName() + "(" + right
                            + ")";

                builder.append("sparql." + operator.getName() + "_" + cmpClass.getName());
                builder.append("(" + left + ", " + right + ")");
            }
            else if(isDateTime(leftClass))
            {
                builder.append("sparql." + operator.getName() + "_datetime(");
                builder.append(leftNode.getNodeAccess(leftClass, 0));
                builder.append(", ");

                if(leftClass instanceof DateTimeClass)
                    builder.append(leftNode.getNodeAccess(leftClass, 1));
                else
                    builder.append("'" + ((DateTimeConstantZoneClass) leftClass).getZone() + "'::int4");

                builder.append(", ");
                builder.append(rightNode.getNodeAccess(rightClass, 0));
                builder.append(", ");

                if(rightClass instanceof DateTimeClass)
                    builder.append(rightNode.getNodeAccess(rightClass, 1));
                else
                    builder.append("'" + ((DateTimeConstantZoneClass) rightClass).getZone() + "'::int4");

                builder.append(")");
            }
            else if(isDate(leftClass))
            {
                builder.append("sparql." + operator.getName() + "_date(");
                builder.append(leftNode.getNodeAccess(leftClass, 0));
                builder.append(", ");

                if(leftClass instanceof DateClass)
                    builder.append(leftNode.getNodeAccess(leftClass, 1));
                else
                    builder.append("'" + ((DateConstantZoneClass) leftClass).getZone() + "'::int4");

                builder.append(", ");
                builder.append(rightNode.getNodeAccess(rightClass, 0));
                builder.append(", ");

                if(rightClass instanceof DateClass)
                    builder.append(rightNode.getNodeAccess(rightClass, 1));
                else
                    builder.append("'" + ((DateConstantZoneClass) rightClass).getZone() + "'::int4");

                builder.append(")");
            }
            else if(isLangString(leftClass))
            {
                builder.append("sparql." + operator.getName() + "_lang_string(");
                builder.append(leftNode.getNodeAccess(leftClass, 0));
                builder.append(", ");

                if(leftClass instanceof LangStringClass)
                    builder.append(leftNode.getNodeAccess(leftClass, 1));
                else
                    builder.append("'" + ((LangStringConstantTagClass) leftClass).getTag() + "'::varchar");

                builder.append(", ");
                builder.append(rightNode.getNodeAccess(rightClass, 0));
                builder.append(", ");

                if(rightClass instanceof LangStringClass)
                    builder.append(rightNode.getNodeAccess(rightClass, 1));
                else
                    builder.append("'" + ((LangStringConstantTagClass) rightClass).getTag() + "'::varchar");

                builder.append(")");
            }
            else if(leftClass instanceof IriClass)
            {
                if(leftClass == rightClass)
                {
                    builder.append("(");

                    for(int i = 0; i < leftClass.getPatternPartsCount(); i++)
                    {
                        if(operator == Operator.Equals)
                        {
                            appendAnd(builder, i > 0);
                            builder.append(leftNode.getNodeAccess(leftClass, i));
                            builder.append(" = ");
                            builder.append(rightNode.getNodeAccess(rightClass, i));
                        }
                        else if(operator == Operator.NotEquals)
                        {
                            appendOr(builder, i > 0);
                            builder.append(leftNode.getNodeAccess(leftClass, i));
                            builder.append(" != ");
                            builder.append(rightNode.getNodeAccess(rightClass, i));
                        }
                        else
                        {
                            assert false;
                        }
                    }

                    builder.append(")");
                }
                else
                {
                    String left = getLeft() instanceof SqlIri ?
                            leftClass.getPatternCode(((SqlIri) getLeft()).getIri(), 0) :
                            leftClass.getExpressionCode(((SqlVariable) getLeft()).getName(),
                                    ((SqlVariable) getLeft()).getVariableAccessor(), false);

                    String right = getRight() instanceof SqlIri ?
                            rightClass.getPatternCode(((SqlIri) getRight()).getIri(), 0) :
                            rightClass.getExpressionCode(((SqlVariable) getRight()).getName(),
                                    ((SqlVariable) getRight()).getVariableAccessor(), false);

                    builder.append("sparql." + operator.getName() + "_iri");
                    builder.append("(" + left + ", " + right + ")");
                }
            }
            else if(leftClass == unsupportedLiteral)
            {
                builder.append("sparql." + operator.getName() + "_typed_literal(");
                builder.append(leftNode.getNodeAccess(leftClass, 0));
                builder.append(", ");
                builder.append(leftNode.getNodeAccess(leftClass, 1));
                builder.append(", ");
                builder.append(rightNode.getNodeAccess(rightClass, 0));
                builder.append(", ");
                builder.append(rightNode.getNodeAccess(rightClass, 1));
                builder.append(")");
            }
            else
            {
                assert false;
            }
        }


        for(Pair<ResourceClass, ResourceClass> pair : different)
        {
            appendComma(builder, hasAlternative);
            hasAlternative = true;

            ResourceClass leftClass = pair.getKey();
            ResourceClass rightClass = pair.getValue();

            assert operator == Operator.Equals || operator == Operator.NotEquals;

            boolean hasVariants = false;

            builder.append("NULLIF(");

            if(getLeft().canBeNull() || leftNode.getResourceClasses().size() > 1)
            {
                for(int i = 0; i < leftClass.getPatternPartsCount(); i++)
                {
                    if(operator == Operator.Equals)
                        appendOr(builder, hasVariants);
                    else
                        appendAnd(builder, hasVariants);

                    hasVariants = true;
                    builder.append(leftNode.getNodeAccess(leftClass, i));
                    builder.append(operator == Operator.Equals ? " IS NULL" : " IS NOT NULL");
                }
            }

            if(getRight().canBeNull() || rightNode.getResourceClasses().size() > 1)
            {
                for(int i = 0; i < rightClass.getPatternPartsCount(); i++)
                {
                    if(operator == Operator.Equals)
                        appendOr(builder, hasVariants);
                    else
                        appendAnd(builder, hasVariants);

                    hasVariants = true;
                    builder.append(rightNode.getNodeAccess(rightClass, i));
                    builder.append(operator == Operator.Equals ? " IS NULL" : " IS NOT NULL");
                }
            }

            builder.append(operator == Operator.Equals ? ", true)" : ", false)");
        }


        if(comparable.size() + different.size() > 1)
            builder.append(")");

        return builder.toString();
    }


    private static ResourceClass determineComparisonClass(ResourceClass leftClass, ResourceClass rightClass)
    {
        if(isDateTime(leftClass) && isDateTime(rightClass))
            return xsdDateTime;

        if(isDate(leftClass) && isDate(rightClass))
            return xsdDate;

        if(isLangString(leftClass) || isLangString(rightClass))
            return rdfLangString;

        if(leftClass instanceof IriClass && rightClass instanceof IriClass)
            return iri;

        if(leftClass instanceof IntBlankNodeClass && rightClass instanceof IntBlankNodeClass)
            return intBlankNode;

        if(leftClass instanceof StrBlankNodeClass && rightClass instanceof StrBlankNodeClass)
            return strBlankNode;

        if(leftClass == rightClass)
            return leftClass;

        if(isNumeric(leftClass) && isNumeric(rightClass))
        {
            if(leftClass == xsdDouble || rightClass == xsdDouble)
                return xsdDouble;
            else if(leftClass == xsdFloat || rightClass == xsdFloat)
                return xsdFloat;
            else if(leftClass == xsdDecimal || rightClass == xsdDecimal)
                return xsdDecimal;
            else if(leftClass == xsdInteger || rightClass == xsdInteger)
                return xsdInteger;
            else if(leftClass == xsdLong || rightClass == xsdLong)
                return xsdLong;
            else if(leftClass == xsdInt || rightClass == xsdInt)
                return xsdInt;
            else if(leftClass == xsdShort || rightClass == xsdShort)
                return xsdShort;
        }

        throw new IllegalArgumentException();
    }


    public boolean isAlwaysFalseOrNull()
    {
        return operator == Operator.Equals && isAlwaysDifferentIfNotNull;
    }


    public boolean isAlwaysTrueOrNull()
    {
        return operator == Operator.NotEquals && isAlwaysDifferentIfNotNull;
    }
}
