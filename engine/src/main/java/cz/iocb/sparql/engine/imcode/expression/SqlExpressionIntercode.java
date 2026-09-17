package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static java.util.stream.Collectors.toCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.common.UnionFind;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.imcode.SqlBaseClass;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.BooleanBaseClass;
import cz.iocb.sparql.engine.mapping.classes.BooleanClass;
import cz.iocb.sparql.engine.mapping.classes.DateCompositeClass;
import cz.iocb.sparql.engine.mapping.classes.DateScalarClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeCompositeClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeScalarClass;
import cz.iocb.sparql.engine.mapping.classes.DayTimeDurationBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DayTimeDurationClass;
import cz.iocb.sparql.engine.mapping.classes.DecimalBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DecimalClass;
import cz.iocb.sparql.engine.mapping.classes.DoubleBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DoubleClass;
import cz.iocb.sparql.engine.mapping.classes.FloatBaseClass;
import cz.iocb.sparql.engine.mapping.classes.FloatClass;
import cz.iocb.sparql.engine.mapping.classes.IntBaseClass;
import cz.iocb.sparql.engine.mapping.classes.IntClass;
import cz.iocb.sparql.engine.mapping.classes.IntegerBaseClass;
import cz.iocb.sparql.engine.mapping.classes.IntegerClass;
import cz.iocb.sparql.engine.mapping.classes.LongBaseClass;
import cz.iocb.sparql.engine.mapping.classes.LongClass;
import cz.iocb.sparql.engine.mapping.classes.PrimitiveResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ShortBaseClass;
import cz.iocb.sparql.engine.mapping.classes.ShortClass;
import cz.iocb.sparql.engine.mapping.classes.StringClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



public abstract class SqlExpressionIntercode extends SqlBaseClass
{
    public static class Restriction
    {
        public static final Restriction ALL = new Restriction(box);

        public static final Restriction NONE = new Restriction(box);


        Set<ResourceClass> set = new HashSet<>();

        public Restriction(Set<ResourceClass> classes)
        {
            add(classes);
        }

        public Restriction(ResourceClass... classes)
        {
            add(Arrays.asList(classes));
        }

        public void add(Collection<ResourceClass> classes)
        {
            if(classes != null)
                set.addAll(classes);
        }

        public void add(ResourceClass... classes)
        {
            add(Arrays.asList(classes));
        }

        public boolean contains(ResourceClass resClass)
        {
            return set.stream().anyMatch(r -> !areDisjunct(r, resClass));
        }

        public boolean contains(Set<ResourceClass> resClasses)
        {
            return resClasses.stream().anyMatch(r -> contains(r));
        }

        public boolean isOptimized(VariableBinding binding)
        {
            return restrict(binding.getMappings()).equals(binding.getMappings());
        }

        public Map<ResourceClass, List<Column>> restrict(Map<ResourceClass, List<Column>> mappings)
        {
            Map<ResourceClass, List<Column>> result = new HashMap<>();

            for(Entry<ResourceClass, List<Column>> e : mappings.entrySet())
                result.put(e.getKey(), contains(e.getKey()) ? e.getValue() : null);

            return result;
        }
    }


    protected final boolean isDeterministic;
    protected final Set<Variable> referencedVariables = new HashSet<>();
    protected final VariableBinding variableBinding;


    protected SqlExpressionIntercode(Map<ResourceClass, List<Column>> mappings, boolean canBeNull,
            boolean isDeterministic)
    {
        this.isDeterministic = isDeterministic;
        this.variableBinding = new VariableBinding(mappings, canBeNull);
    }


    public abstract Restrictions getRequirements();


    public abstract SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices);


    protected abstract void generateExplanation(StringBuilder builder, String indent, int priority);


    public void generateExplanation(StringBuilder builder, String indent)
    {
        generateExplanation(builder, indent, 10);
    }


    public boolean hasExpressionColumn(Set<ResourceClass> resClasses)
    {
        return resClasses.stream().anyMatch(r -> hasExpressionColumn(r));
    }


    public boolean hasExpressionColumn(ResourceClass resClass)
    {
        for(Entry<ResourceClass, List<Column>> e : variableBinding.getMappings().entrySet())
            if(!areDisjunct(resClass, e.getKey()) && e.getValue().stream().anyMatch(c -> c instanceof ExpressionColumn))
                return true;

        return false;
    }


    public boolean canSafelyGeneralize(ResourceClass sourceClass, ResourceClass targetClass)
    {
        return sourceClass.getEffectiveClass().equals(targetClass) || sourceClass.isSubclassOf(targetClass)
                && (!hasExpressionColumn(sourceClass) || targetClass.getColumnCount() == 1);
    }


    public static ResourceClass determineResultClass(ResourceClass operandClass)
    {
        if(isDouble(operandClass))
            return xsdDouble;
        else if(isFloat(operandClass))
            return xsdFloat;
        else if(isDecimal(operandClass))
            return xsdDecimal;
        else
            return xsdInteger;
    }


    public static ResourceClass determineResultClass(ResourceClass leftClass, ResourceClass rightClass)
    {
        if(isDouble(leftClass) || isDouble(rightClass))
            return xsdDouble;
        else if(isFloat(leftClass) || isFloat(rightClass))
            return xsdFloat;
        else if(isDecimal(leftClass) || isDecimal(rightClass))
            return xsdDecimal;
        else
            return xsdInteger;
    }


    public static Set<ResourceClass> getNumericClasses(ResourceClass resClass)
    {
        return Stream.of(genShort, genInt, genLong, genInteger, genDecimal, genFloat, genDouble)
                .filter(r -> !areDisjunct(r, resClass)).collect(toCollection(HashSet::new));
    }


    public static String getLiteralClassName(ResourceClass resClass)
    {
        return switch(resClass.getEffectiveClass())
        {
            case BooleanBaseClass _ -> "boolean";
            case BooleanClass _ -> "boolean";
            case ShortBaseClass _ -> "short";
            case ShortClass _ -> "short";
            case IntBaseClass _ -> "int";
            case IntClass _ -> "int";
            case LongBaseClass _ -> "long";
            case LongClass _ -> "long";
            case IntegerBaseClass _ -> "integer";
            case IntegerClass _ -> "integer";
            case DecimalBaseClass _ -> "decimal";
            case DecimalClass _ -> "decimal";
            case FloatBaseClass _ -> "float";
            case FloatClass _ -> "float";
            case DoubleBaseClass _ -> "double";
            case DoubleClass _ -> "double";
            case StringClass _ -> "string";
            case DayTimeDurationBaseClass _ -> "daytimeduration";
            case DayTimeDurationClass _ -> "daytimeduration";
            case DateTimeScalarClass _ -> "datetime";
            case DateTimeCompositeClass _ -> "datetime";
            case DateScalarClass _ -> "date";
            case DateCompositeClass _ -> "date";

            default -> throw new IllegalArgumentException();
        };
    }


    protected static Map<ResourceClass, Set<List<Set<ResourceClass>>>> processResultMap(
            List<SqlExpressionIntercode> arguments, Map<ResourceClass, Set<List<ResourceClass>>> map,
            Restriction restriction, PrimitiveResourceClass unionClass)
    {
        record Input(ResourceClass result, List<ResourceClass> params)
        {
            public static boolean areInConflict(List<SqlExpressionIntercode> arguments, Restriction restriction,
                    Input l, Input r)
            {
                if(!restriction.contains(l.result) || !restriction.contains(r.result))
                    return false;

                for(int i = 0; i < arguments.size(); i++)
                {
                    SqlExpressionIntercode arg = arguments.get(i);
                    ResourceClass lc = l.params.get(i);
                    ResourceClass rc = l.params.get(i);

                    if(!areDisjunct(lc, rc) && (arg.hasExpressionColumn(lc) || arg.hasExpressionColumn(lc)))
                        return true;
                }

                return false;
            }
        }

        Set<Input> inputs = new HashSet<>();

        for(Entry<ResourceClass, Set<List<ResourceClass>>> e : map.entrySet())
            for(List<ResourceClass> c : e.getValue())
                inputs.add(new Input(e.getKey(), c));

        Collection<Set<Input>> groupParameters = UnionFind.getDisjunctEntries(inputs,
                (l, r) -> Input.areInConflict(arguments, restriction, l, r));


        record Midle(Set<ResourceClass> result, List<Set<ResourceClass>> params)
        {
        }

        Set<Midle> middles = new HashSet<>();

        for(Set<Input> s : groupParameters)
        {
            Set<ResourceClass> result = new HashSet<>();
            List<Set<ResourceClass>> params = new ArrayList<>();

            for(int i = 0; i < arguments.size(); i++)
                params.add(new HashSet<>());

            for(Input e : s)
            {
                result.add(e.result);

                for(int i = 0; i < arguments.size(); i++)
                    params.get(i).add(e.params.get(i));
            }

            middles.add(new Midle(result, params));
        }


        Collection<Set<Midle>> groupResults = UnionFind.getDisjunctEntries(middles,
                (l, r) -> !areDisjunct(l.result, r.result) && restriction.contains(l.result)
                        && restriction.contains(r.result));

        Map<ResourceClass, Set<List<Set<ResourceClass>>>> output = new HashMap<>();

        for(Set<Midle> s : groupResults)
        {
            Set<ResourceClass> result = new HashSet<>();
            Set<List<Set<ResourceClass>>> params = new HashSet<>();

            for(Midle e : s)
            {
                result.addAll(e.result);
                params.add(e.params);
            }

            if(!restriction.contains(result))
                params = null;

            if(result.size() == 1)
                output.put(result.iterator().next(), params);
            else if(unionClass != null)
                output.put(unionize(result, unionClass), params);
            else
                output.put(unionize(result), params);
        }

        return output;
    }


    protected static Map<ResourceClass, Set<List<Set<ResourceClass>>>> processResultMap(
            List<SqlExpressionIntercode> arguments, Map<ResourceClass, Set<List<ResourceClass>>> map,
            Restriction restriction)
    {
        return processResultMap(arguments, map, restriction, null);
    }


    public String getIsNull()
    {
        return variableBinding.getIsNull();
    }


    public String getIsNotNull()
    {
        return variableBinding.getIsNotNull();
    }


    public String getIsNull(ResourceClass resClass)
    {
        return variableBinding.getIsNull(resClass);
    }


    public String getIsNotNull(ResourceClass resClass)
    {
        return variableBinding.getIsNotNull(resClass);
    }


    protected Column getStringLiteral()
    {
        return variableBinding.getStringLiteral();
    }


    protected Column getStringLiteral(Set<ResourceClass> resClasses)
    {
        return variableBinding.getStringLiteral(resClasses);
    }


    protected Column promoteNumericAs(ResourceClass source, ResourceClass target)
    {
        return variableBinding.promoteNumericAs(source, target);
    }


    public Column promoteNumericAs(Set<ResourceClass> set, ResourceClass target)
    {
        return variableBinding.promoteNumericAs(set, target);
    }


    public List<Column> get(ResourceClass resClass)
    {
        return variableBinding.deriveMapping(resClass);
    }


    public Map<ResourceClass, List<Column>> getMappings()
    {
        return variableBinding.getMappings();
    }


    public List<Column> getMapping(ResourceClass resClass)
    {
        return variableBinding.getMapping(resClass);
    }


    public Set<ResourceClass> getResourceClasses()
    {
        return variableBinding.getClasses();
    }


    public boolean canBeNull()
    {
        return variableBinding.canBeNull();
    }


    public VariableBinding getBinding()
    {
        return variableBinding;
    }


    public boolean isDeterministic()
    {
        return isDeterministic;
    }


    public Set<Variable> getReferencedVariables()
    {
        return referencedVariables;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlExpressionIntercode imcode))
            return false;

        if(hashCode() != imcode.hashCode())
            return false;

        if(!Objects.equals(isDeterministic, imcode.isDeterministic))
            return false;

        if(!Objects.equals(variableBinding, imcode.variableBinding))
            return false;

        return true;
    }
}
