package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.getUnionClass;
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
import cz.iocb.sparql.engine.common.UnionFind;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.mapping.classes.PrimitiveResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlBaseClass;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public abstract class SqlExpressionIntercode extends SqlBaseClass
{
    public static class Restriction
    {
        public static final Restriction ALL = new Restriction(box);

        public static final Restriction NONE = new Restriction(box);


        Set<ResourceClass> set = new HashSet<ResourceClass>();

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

        public boolean isOptimized(UsedVariable variable)
        {
            return restrict(variable.getMappings()).equals(variable.getMappings());
        }

        public Map<ResourceClass, List<Column>> restrict(Map<ResourceClass, List<Column>> mappings)
        {
            Map<ResourceClass, List<Column>> result = new HashMap<ResourceClass, List<Column>>();

            for(Entry<ResourceClass, List<Column>> e : mappings.entrySet())
                result.put(e.getKey(), contains(e.getKey()) ? e.getValue() : null);

            return result;
        }
    }


    protected final boolean isDeterministic;
    protected final Set<String> referencedVariables = new HashSet<String>();
    protected final UsedVariable variable;


    protected SqlExpressionIntercode(Map<ResourceClass, List<Column>> mappings, boolean canBeNull,
            boolean isDeterministic)
    {
        this.isDeterministic = isDeterministic;
        this.variable = new UsedVariable(mappings, canBeNull);
    }


    public abstract Restrictions getRequirements();


    public abstract SqlExpressionIntercode optimize(Request request, UsedVariables variables, Restriction restriction,
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
        for(Entry<ResourceClass, List<Column>> e : variable.getMappings().entrySet())
            if(!areDisjunct(resClass, e.getKey()) && e.getValue().stream().anyMatch(c -> c instanceof ExpressionColumn))
                return true;

        return false;
    }


    public boolean canSafelyGeneralize(ResourceClass sourceClass, ResourceClass targetClass)
    {
        return sourceClass.getEffectiveClass().equals(targetClass)
                || sourceClass.isSubclassOf(targetClass) && !hasExpressionColumn(sourceClass);
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
        if(leftClass.equals(xsdDouble) || rightClass.equals(xsdDouble))
            return xsdDouble;
        else if(leftClass.equals(xsdFloat) || rightClass.equals(xsdFloat))
            return xsdFloat;
        else if(leftClass.equals(xsdDecimal) || rightClass.equals(xsdDecimal))
            return xsdDecimal;
        else
            return xsdInteger;
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

        Set<Input> inputs = new HashSet<Input>();

        for(Entry<ResourceClass, Set<List<ResourceClass>>> e : map.entrySet())
            for(List<ResourceClass> c : e.getValue())
                inputs.add(new Input(e.getKey(), c));

        Collection<Set<Input>> groupParameters = UnionFind.getDisjunctEntries(inputs,
                (l, r) -> Input.areInConflict(arguments, restriction, l, r));


        record Midle(Set<ResourceClass> result, List<Set<ResourceClass>> params)
        {
        }

        Set<Midle> middles = new HashSet<Midle>();

        for(Set<Input> s : groupParameters)
        {
            Set<ResourceClass> result = new HashSet<ResourceClass>();
            List<Set<ResourceClass>> params = new ArrayList<>();

            for(int i = 0; i < arguments.size(); i++)
                params.add(new HashSet<ResourceClass>());

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
            Set<ResourceClass> result = new HashSet<ResourceClass>();
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
                output.put(getUnionClass(result, unionClass), params);
            else
                output.put(getUnionClass(result), params);
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
        return variable.getIsNull();
    }


    public String getIsNotNull()
    {
        return variable.getIsNotNull();
    }


    public String getIsNull(ResourceClass resClass)
    {
        return variable.getIsNull(resClass);
    }


    public String getIsNotNull(ResourceClass resClass)
    {
        return variable.getIsNotNull(resClass);
    }


    protected Column getStringLiteral()
    {
        return variable.getStringLiteral();
    }


    protected Column getStringLiteral(Set<ResourceClass> resClasses)
    {
        return variable.getStringLiteral(resClasses);
    }


    protected Column promoteNumericAs(ResourceClass source, ResourceClass target)
    {
        return variable.promoteNumericAs(source, target);
    }


    public Column promoteNumericAs(Set<ResourceClass> set, ResourceClass target)
    {
        return variable.promoteNumericAs(set, target);
    }


    public List<Column> get(ResourceClass resClass)
    {
        return variable.deriveMapping(resClass);
    }


    public Map<ResourceClass, List<Column>> getMappings()
    {
        return variable.getMappings();
    }


    public List<Column> getMapping(ResourceClass resClass)
    {
        return variable.getMapping(resClass);
    }


    public Set<ResourceClass> getResourceClasses()
    {
        return variable.getClasses();
    }


    public boolean canBeNull()
    {
        return variable.canBeNull();
    }


    public UsedVariable getUsedVariable()
    {
        return variable;
    }


    public boolean isDeterministic()
    {
        return isDeterministic;
    }


    public Set<String> getReferencedVariables()
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

        if(!Objects.equals(variable, imcode.variable))
            return false;

        return true;
    }
}
