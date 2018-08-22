package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedIri;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
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
import java.util.LinkedList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.mapping.classes.DatePatternClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DatePatternClassWithConstantZone;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimePatternClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimePatternClassWithConstantZone;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringPatternClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringPatternClassWithConstantTag;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.BlankNodeBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.DateClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.DateTimeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.LangStringClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.chemweb.server.sparql.translator.Pair;



public class SqlNodeBinaryComparison extends SqlBinaryComparison
{
    private final List<Pair<PatternResourceClass, PatternResourceClass>> comparable;
    private final List<Pair<PatternResourceClass, PatternResourceClass>> different;


    public SqlNodeBinaryComparison(Operator operator, SqlNodeValue left, SqlNodeValue right, boolean canBeNull,
            boolean isAlwaysDifferentIfNotNull, List<Pair<PatternResourceClass, PatternResourceClass>> comparable,
            List<Pair<PatternResourceClass, PatternResourceClass>> different)
    {
        super(operator, left, right, canBeNull, isAlwaysDifferentIfNotNull);
        this.comparable = comparable;
        this.different = different;
    }


    public static SqlExpressionIntercode create(Operator operator, SqlNodeValue left, SqlNodeValue right)
    {
        List<Pair<PatternResourceClass, PatternResourceClass>> comparable = new LinkedList<Pair<PatternResourceClass, PatternResourceClass>>();
        List<Pair<PatternResourceClass, PatternResourceClass>> different = new LinkedList<Pair<PatternResourceClass, PatternResourceClass>>();

        boolean isAlwaysNull = true;
        boolean isAlwaysDifferentIfNotNull = !left.canBeNull() && !right.canBeNull();
        boolean canBeNull = left.canBeNull() || right.canBeNull();

        for(PatternResourceClass leftClass : left.getPatternResourceClasses())
        {
            for(PatternResourceClass rightClass : right.getPatternResourceClasses())
            {
                if(leftClass == rightClass && (leftClass == xsdBoolean || leftClass == xsdString))
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(isNumeric(leftClass) && isNumeric(rightClass))
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(leftClass instanceof DateTimeClass && rightClass instanceof DateTimeClass)
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(leftClass instanceof DateClass && rightClass instanceof DateClass)
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
                else if(leftClass instanceof BlankNodeBaseClass && rightClass instanceof BlankNodeBaseClass)
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
                if(leftClass == xsdDayTimeDuration && rightClass == xsdDayTimeDuration)
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(leftClass instanceof LangStringClass && rightClass instanceof LangStringClass)
                {
                    if(leftClass instanceof LangStringPatternClassWithConstantTag
                            && rightClass instanceof LangStringPatternClassWithConstantTag
                            && !((LangStringPatternClassWithConstantTag) leftClass).getTag()
                                    .equals(((LangStringPatternClassWithConstantTag) rightClass).getTag()))
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
                else if(leftClass instanceof IriClass && rightClass instanceof IriClass)
                {
                    if(leftClass instanceof UserIriClass && rightClass instanceof UserIriClass
                            && leftClass != rightClass)
                    {
                        // is always different
                        isAlwaysNull = false;
                        different.add(new Pair<>(leftClass, rightClass));
                    }
                    else if((leftClass == unsupportedIri || rightClass == unsupportedIri) && leftClass != rightClass)
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

        return new SqlNodeBinaryComparison(operator, left, right, canBeNull, isAlwaysDifferentIfNotNull, comparable,
                different);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();
        boolean hasAlternative = false;

        if(comparable.size() + different.size() > 1)
            builder.append("COALESCE(");


        for(Pair<PatternResourceClass, PatternResourceClass> pair : comparable)
        {
            appendComma(builder, hasAlternative);
            hasAlternative = true;

            PatternResourceClass leftClass = pair.getKey();
            PatternResourceClass rightClass = pair.getValue();

            if(leftClass == xsdBoolean || leftClass == xsdString || leftClass == xsdDayTimeDuration
                    || isNumeric(leftClass) || leftClass instanceof BlankNodeBaseClass)
            {
                String left = getLeftNode().nodeAccess(leftClass, 0);
                String right = getRightNode().nodeAccess(rightClass, 0);

                PatternResourceClass cmpClass = determineComparisonClass(leftClass, rightClass);

                if(leftClass != cmpClass)
                    left = "sparql.cast_as_" + cmpClass.getName() + "_from_" + leftClass.getName() + "(" + left + ")";

                if(rightClass != cmpClass)
                    right = "sparql.cast_as_" + cmpClass.getName() + "_from_" + rightClass.getName() + "(" + right
                            + ")";

                builder.append("sparql." + operator.getName() + "_" + cmpClass.getName());
                builder.append("(" + left + ", " + right + ")");
            }
            else if(leftClass instanceof DateTimeClass)
            {
                builder.append("sparql." + operator.getName() + "_datetime(");
                builder.append(getLeftNode().nodeAccess(leftClass, 0));
                builder.append(", ");

                if(leftClass instanceof DateTimePatternClass)
                    builder.append(getLeftNode().nodeAccess(leftClass, 1));
                else
                    builder.append("'" + ((DateTimePatternClassWithConstantZone) leftClass).getZone() + "'::int4");

                builder.append(", ");
                builder.append(getRightNode().nodeAccess(leftClass, 0));
                builder.append(", ");

                if(rightClass instanceof DateTimePatternClass)
                    builder.append(getRightNode().nodeAccess(rightClass, 1));
                else
                    builder.append("'" + ((DateTimePatternClassWithConstantZone) rightClass).getZone() + "'::int4");

                builder.append(")");
            }
            else if(leftClass instanceof DateClass)
            {
                builder.append("sparql." + operator.getName() + "_date(");
                builder.append(getLeftNode().nodeAccess(leftClass, 0));
                builder.append(", ");

                if(leftClass instanceof DatePatternClass)
                    builder.append(getLeftNode().nodeAccess(leftClass, 1));
                else
                    builder.append("'" + ((DatePatternClassWithConstantZone) leftClass).getZone() + "'::int4");

                builder.append(", ");
                builder.append(getRightNode().nodeAccess(leftClass, 0));
                builder.append(", ");

                if(rightClass instanceof DatePatternClass)
                    builder.append(getRightNode().nodeAccess(rightClass, 1));
                else
                    builder.append("'" + ((DatePatternClassWithConstantZone) rightClass).getZone() + "'::int4");

                builder.append(")");
            }
            else if(leftClass instanceof LangStringClass)
            {
                builder.append("sparql." + operator.getName() + "_lang_string(");
                builder.append(getLeftNode().nodeAccess(leftClass, 0));
                builder.append(", ");

                if(leftClass instanceof LangStringPatternClass)
                    builder.append(getLeftNode().nodeAccess(leftClass, 1));
                else
                    builder.append("'" + ((LangStringPatternClassWithConstantTag) leftClass).getTag() + "'::varchar");

                builder.append(", ");
                builder.append(getRightNode().nodeAccess(leftClass, 0));
                builder.append(", ");

                if(rightClass instanceof LangStringPatternClass)
                    builder.append(getRightNode().nodeAccess(rightClass, 1));
                else
                    builder.append("'" + ((LangStringPatternClassWithConstantTag) rightClass).getTag() + "'::varchar");

                builder.append(")");
            }
            else if(leftClass instanceof IriClass)
            {
                if(leftClass == rightClass)
                {
                    builder.append("(");

                    for(int i = 0; i < leftClass.getPartsCount(); i++)
                    {
                        if(operator == Operator.Equals)
                        {
                            appendAnd(builder, i > 0);
                            builder.append(getLeftNode().nodeAccess(leftClass, i));
                            builder.append(" = ");
                            builder.append(getRightNode().nodeAccess(rightClass, i));
                        }
                        else if(operator == Operator.NotEquals)
                        {
                            appendOr(builder, i > 0);
                            builder.append(getLeftNode().nodeAccess(leftClass, i));
                            builder.append(" != ");
                            builder.append(getRightNode().nodeAccess(rightClass, i));
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
                    String left = getLeft() instanceof SqlIri ? leftClass.getSqlValue(((SqlIri) getLeft()).getIri(), 0)
                            : leftClass.getSqlExpressionValue(((SqlVariable) getLeft()).getName(),
                                    ((SqlVariable) getLeft()).getVariableAccessor(), false);

                    String right = getRight() instanceof SqlIri
                            ? rightClass.getSqlValue(((SqlIri) getRight()).getIri(), 0)
                            : rightClass.getSqlExpressionValue(((SqlVariable) getRight()).getName(),
                                    ((SqlVariable) getRight()).getVariableAccessor(), false);

                    builder.append("sparql." + operator.getName() + "_iri");
                    builder.append("(" + left + ", " + right + ")");
                }
            }
            else if(leftClass == unsupportedLiteral)
            {
                builder.append("sparql." + operator.getName() + "_typed_literal(");
                builder.append(getLeftNode().nodeAccess(leftClass, 0));
                builder.append(", ");
                builder.append(getLeftNode().nodeAccess(leftClass, 1));
                builder.append(", ");
                builder.append(getRightNode().nodeAccess(leftClass, 0));
                builder.append(", ");
                builder.append(getRightNode().nodeAccess(leftClass, 1));
                builder.append(")");
            }
            else
            {
                assert false;
            }
        }


        for(Pair<PatternResourceClass, PatternResourceClass> pair : different)
        {
            appendComma(builder, hasAlternative);
            hasAlternative = true;

            PatternResourceClass leftClass = pair.getKey();
            PatternResourceClass rightClass = pair.getValue();

            assert operator == Operator.Equals || operator == Operator.NotEquals;

            boolean hasVariants = false;

            builder.append("NULLIF(");

            if(getLeft().canBeNull() || getLeftNode().getPatternResourceClasses().size() > 1)
            {
                for(int i = 0; i < leftClass.getPartsCount(); i++)
                {
                    if(operator == Operator.Equals)
                        appendOr(builder, hasVariants);
                    else
                        appendAnd(builder, hasVariants);

                    hasVariants = true;
                    builder.append(getLeftNode().nodeAccess(leftClass, i));
                    builder.append(operator == Operator.Equals ? " IS NULL" : " IS NOT NULL");
                }
            }

            if(getRight().canBeNull() || getRightNode().getPatternResourceClasses().size() > 1)
            {
                for(int i = 0; i < rightClass.getPartsCount(); i++)
                {
                    if(operator == Operator.Equals)
                        appendOr(builder, hasVariants);
                    else
                        appendAnd(builder, hasVariants);

                    hasVariants = true;
                    builder.append(getRightNode().nodeAccess(rightClass, i));
                    builder.append(operator == Operator.Equals ? " IS NULL" : " IS NOT NULL");
                }
            }

            builder.append(operator == Operator.Equals ? ", true)" : ", false)");
        }


        if(comparable.size() + different.size() > 1)
            builder.append(")");

        return builder.toString();
    }


    private SqlNodeValue getLeftNode()
    {
        return (SqlNodeValue) getLeft();
    }


    private SqlNodeValue getRightNode()
    {
        return (SqlNodeValue) getRight();
    }


    private static PatternResourceClass determineComparisonClass(PatternResourceClass leftClass,
            PatternResourceClass rightClass)
    {
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
}
