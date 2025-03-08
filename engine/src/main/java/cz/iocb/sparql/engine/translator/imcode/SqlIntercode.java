package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedPairedVariable.PairedClass;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public abstract class SqlIntercode extends SqlBaseClass
{
    protected final UsedVariables variables;
    protected final boolean isDeterministic;


    protected SqlIntercode(UsedVariables variables, boolean isDeterministic)
    {
        this.variables = variables;
        this.isDeterministic = isDeterministic;
    }


    public abstract SqlIntercode optimize(Request request, Set<String> restrictions, boolean reduced);


    public abstract String translate(Request request);


    public final UsedVariables getVariables()
    {
        return variables;
    }


    public final UsedVariable getVariable(String variable)
    {
        return variables.get(variable);
    }


    public final Map<ResourceClass, List<Column>> getMappings(String variable)
    {
        return variables.get(variable).getMappings();
    }


    public final List<Column> getMapping(String variable, ResourceClass resClass)
    {
        return variables.get(variable).getMapping(resClass);
    }


    public final List<Column> getMapping(String variable)
    {
        if(variables.get(variable) == null)
            return null;

        return variables.get(variable).getMapping();
    }


    public final boolean hasConstantVariable(String variable)
    {
        UsedVariable var = variables.get(variable);

        return var == null || var.isConstant();
    }


    public final boolean hasConstantVariables(Set<String> variables)
    {
        return variables.stream().allMatch(v -> hasConstantVariable(v));
    }


    public boolean isDeterministic()
    {
        return isDeterministic;
    }


    protected static UsedVariables getJoinUsedVariables(Request request, List<UsedVariables> allVars,
            List<Table> tables, Set<String> restrictions, Map<Column, Column> map)
    {
        Map<Column, Column> columnMap = new HashMap<Column, Column>();

        UsedVariables variables = new UsedVariables();

        for(String name : allVars.stream().flatMap(v -> v.getNames().stream()).collect(toSet()))
        {
            List<UsedVariable> vars = allVars.stream().map(v -> v.get(name)).toList();
            List<UsedVariable> defs = vars.stream().filter(v -> v != null).toList();

            if(defs.stream().anyMatch(v -> !v.canBeNull()))
            {
                defs = defs.stream().filter(v -> !v.canBeNull()).toList();
                Set<ResourceClass> resClasses = selectSharedClasses(cleanGeneralClasses(collectClasses(defs)), defs);

                if(resClasses.isEmpty())
                    return null;

                UsedVariable var = createUsedVariable(request, name, resClasses, vars, tables, columnMap, false);

                if(var == null)
                    return null;

                variables.add(var);
            }
            else
            {
                Set<ResourceClass> resClasses = cleanSpecificClasses(collectClasses(defs));
                UsedVariable var = createUsedVariable(request, name, resClasses, vars, tables, columnMap, true);

                variables.add(var);
            }
        }

        columnMap.entrySet().forEach(e -> map.put(e.getValue(), e.getKey()));

        return variables.restrict(restrictions);
    }


    private static UsedVariable createUsedVariable(Request request, String name, Set<ResourceClass> resClasses,
            List<UsedVariable> vars, List<Table> tables, Map<Column, Column> columnMap, boolean canBeNull)
    {
        UsedVariable variable = new UsedVariable(name, canBeNull);

        if(!canBeNull)
        {
            for(ResourceClass resClass : resClasses)
            {
                List<List<Column>> mappings = new ArrayList<>();

                for(int i = 0; i < vars.size(); i++)
                {
                    UsedVariable var = vars.get(i);

                    if(var == null || var.canBeNull() || !var.containsClass(resClass))
                        continue;

                    mappings.add(toTableColumns(tables.get(i), var.getMapping(resClass)));
                }

                if(IntStream.range(0, resClass.getColumnCount()).anyMatch(i -> mappings.stream().map(m -> m.get(i))
                        .filter(c -> c instanceof ConstantColumn).distinct().count() > 1))
                    return null;

                List<Column> columns = selectColumns(resClass, mappings);

                variable.addMapping(resClass,
                        getMappedColuns(resClass.createColumns(request.getColumnMap(), name), columns, columnMap));
            }
        }
        else
        {
            for(ResourceClass resClass : resClasses)
            {
                List<List<Column>> variants = new ArrayList<>();

                for(int i = 0; i < vars.size(); i++)
                {
                    UsedVariable var = vars.get(i);
                    Table table = tables.get(i);

                    if(var == null)
                        continue;

                    for(ResourceClass specClass : var.getCompatibleClasses(resClass))
                    {
                        List<Column> columns = toTableColumns(table, var.getMapping(specClass));
                        variants.add(resClass == specClass ? columns : specClass.toGeneralClass(columns, true));
                    }
                }

                List<Column> columns = coalesceVariants(resClass.getColumnCount(), variants);

                variable.addMapping(resClass,
                        getMappedColuns(resClass.createColumns(request.getColumnMap(), name), columns, columnMap));
            }
        }

        return variable;
    }


    private static List<Column> selectColumns(ResourceClass resClass, List<List<Column>> mappings)
    {
        return IntStream
                .range(0, resClass.getColumnCount()).mapToObj(i -> mappings.stream().map(m -> m.get(i))
                        .filter(c -> c instanceof ConstantColumn).findFirst().orElse(mappings.getFirst().get(i)))
                .toList();
    }


    private static List<Column> getMappedColuns(List<Column> output, List<Column> input, Map<Column, Column> map)
    {
        List<Column> mapping = new ArrayList<>();

        for(int i = 0; i < output.size(); i++)
        {
            Column access = input.get(i);
            Column column = map.get(access);

            if(column == null)
            {
                column = output.get(i);
                map.put(access, column);
            }

            mapping.add(column);
        }

        return mapping;
    }


    private static List<Column> coalesceVariants(int cols, List<List<Column>> variants)
    {
        if(variants.size() == 1)
            return variants.getFirst();

        return IntStream
                .range(0, cols).mapToObj(i -> (Column) new ExpressionColumn(variants.stream()
                        .map(l -> l.get(i).toString()).distinct().sorted().collect(joining(", ", "coalesce(", ")"))))
                .toList();
    }


    private static Set<ResourceClass> selectSharedClasses(Set<ResourceClass> classes, List<UsedVariable> variables)
    {
        return classes.stream().filter(
                c -> variables.stream().allMatch(v -> v.containsClass(c) || v.containsClass(c.getGeneralClass())))
                .collect(toSet());
    }


    private static Set<ResourceClass> cleanSpecificClasses(Set<ResourceClass> classes)
    {
        return classes.stream().filter(r -> r == r.getGeneralClass() || !classes.contains(r.getGeneralClass()))
                .collect(toSet());
    }


    static Set<ResourceClass> cleanGeneralClasses(Set<ResourceClass> classes)
    {
        return classes.stream().filter(r -> classes.stream().noneMatch(x -> x != r && x.getGeneralClass() == r))
                .collect(toSet());
    }


    private static Set<ResourceClass> collectClasses(List<UsedVariable> variables)
    {
        return variables.stream().flatMap(v -> v.getClasses().stream()).collect(toSet());
    }


    static List<Column> toTableColumns(Table table, List<Column> columns)
    {
        return columns.stream().map(c -> c.fromTable(table)).toList();
    }


    protected static UsedVariables getJoinUsedVariables(Request request, UsedVariables left, UsedVariables right,
            Table leftTable, Table rightTable, Set<String> restrictions, Map<Column, Column> map)
    {
        return getJoinUsedVariables(request, Arrays.asList(left, right), Arrays.asList(leftTable, rightTable),
                restrictions, map);
    }


    public static boolean isJoinConditionAlwaysTrue(UsedVariables left, UsedVariables right)
    {
        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left, right))
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();

            if(leftVariable != null && rightVariable != null)
            {
                if(leftVariable.canBeNull())
                    return false;

                if(rightVariable.canBeNull())
                    return false;

                for(PairedClass pairedClass : pair.getClasses())
                {
                    if(pairedClass.getLeftClass() != pairedClass.getRightClass())
                        return false;

                    ResourceClass resClass = pairedClass.getLeftClass();
                    List<Column> leftCols = leftVariable.getMapping(resClass);
                    List<Column> rightCols = rightVariable.getMapping(resClass);

                    for(int i = 0; i < resClass.getColumnCount(); i++)
                    {
                        Column leftCol = leftCols.get(i);
                        Column rightCol = rightCols.get(i);

                        if(!(leftCol instanceof ConstantColumn && rightCol instanceof ConstantColumn)
                                || !leftCol.equals(rightCol))
                            return false;
                    }
                }
            }
        }

        return true;
    }


    public static String generateJoinCondition(List<UsedVariables> vars, List<Table> tables)
    {
        int size = vars.size();

        List<String> conditions = IntStream.range(0, size)
                .mapToObj(i -> IntStream.range(i + 1, size)
                        .mapToObj(j -> generateJoinCondition(vars.get(i), vars.get(j), tables.get(i), tables.get(j))))
                .flatMap(c -> c).filter(c -> c != null).toList();

        if(conditions.isEmpty())
            return null;

        return conditions.stream().collect(joining(" AND ", "(", ")"));
    }


    public static String generateJoinCondition(UsedVariables left, UsedVariables right, Table leftTable,
            Table rightTable)
    {
        List<String> join = new ArrayList<String>();

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left, right))
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();

            if(leftVariable != null && rightVariable != null)
            {
                List<String> condition = new ArrayList<String>();

                if(leftVariable.canBeNull())
                    condition.add(leftVariable.getNonConstantColumns().stream()
                            .map(c -> c.fromTable(leftTable) + " IS NULL").sorted().collect(joining(" AND ")));

                if(rightVariable.canBeNull())
                    condition.add(rightVariable.getNonConstantColumns().stream()
                            .map(c -> c.fromTable(rightTable) + " IS NULL").sorted().collect(joining(" AND ")));

                for(PairedClass pairedClass : pair.getClasses())
                {
                    if(pairedClass.getLeftClass() != null && pairedClass.getRightClass() != null)
                    {
                        List<Column> leftCols = leftVariable.getMapping(pairedClass.getLeftClass()).stream()
                                .map(c -> c.fromTable(leftTable)).collect(toList());
                        List<Column> rightCols = rightVariable.getMapping(pairedClass.getRightClass()).stream()
                                .map(c -> c.fromTable(rightTable)).collect(toList());

                        Set<String> compare = new HashSet<String>();

                        if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                        {
                            ResourceClass resClass = pairedClass.getLeftClass();

                            for(int i = 0; i < resClass.getColumnCount(); i++)
                            {
                                Column leftCol = leftCols.get(i);
                                Column rightCol = rightCols.get(i);

                                if(!(leftCol instanceof ConstantColumn && rightCol instanceof ConstantColumn))
                                    compare.add(leftCol + " = " + rightCol);
                                else if(!leftCol.equals(rightCol))
                                    compare.add("false");
                            }
                        }
                        else
                        {
                            ResourceClass leftClass = pairedClass.getLeftClass();
                            ResourceClass rightClass = pairedClass.getRightClass();

                            List<Column> genLeftCols = leftClass.toGeneralClass(leftCols, false);
                            List<Column> genRightCols = rightClass.toGeneralClass(rightCols, false);

                            for(int i = 0; i < leftClass.getGeneralClass().getColumnCount(); i++)
                                compare.add(genLeftCols.get(i) + " = " + genRightCols.get(i));
                        }

                        if(compare.isEmpty())
                            condition.add("true");

                        if(!compare.contains("false"))
                            condition.add(compare.stream().sorted().collect(joining(" AND ")));
                    }
                }

                assert !condition.isEmpty();

                if(!condition.contains("true"))
                    join.add(condition.stream().sorted().collect(joining(" OR ", "(", ")")));
            }
        }

        if(join.isEmpty())
            return null;

        return join.stream().sorted().collect(joining(" AND ", "(", ")"));
    }


    public static String generateJoinCondition(UsedVariable leftVariable, UsedVariable rightVariable, Table leftTable,
            Table rightTable)
    {
        List<String> condition = new ArrayList<String>();

        if(leftVariable.canBeNull())
            condition.add(leftVariable.getNonConstantColumns().stream().map(c -> c.fromTable(leftTable) + " IS NULL")
                    .sorted().collect(joining(" AND ")));

        if(rightVariable.canBeNull())
            condition.add(rightVariable.getNonConstantColumns().stream().map(c -> c.fromTable(rightTable) + " IS NULL")
                    .sorted().collect(joining(" AND ")));

        for(PairedClass pairedClass : (new UsedPairedVariable(leftVariable, rightVariable)).getClasses())
        {
            if(pairedClass.getLeftClass() != null && pairedClass.getRightClass() != null)
            {
                List<Column> leftCols = leftVariable.getMapping(pairedClass.getLeftClass()).stream()
                        .map(c -> c.fromTable(leftTable)).collect(toList());
                List<Column> rightCols = rightVariable.getMapping(pairedClass.getRightClass()).stream()
                        .map(c -> c.fromTable(rightTable)).collect(toList());

                Set<String> compare = new HashSet<String>();

                if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                {
                    ResourceClass resClass = pairedClass.getLeftClass();

                    for(int i = 0; i < resClass.getColumnCount(); i++)
                    {
                        Column leftCol = leftCols.get(i);
                        Column rightCol = rightCols.get(i);

                        if(!(leftCol instanceof ConstantColumn && rightCol instanceof ConstantColumn))
                            compare.add(leftCol + " = " + rightCol);
                        else if(!leftCol.equals(rightCol))
                            compare.add("false");
                    }
                }
                else
                {
                    ResourceClass leftClass = pairedClass.getLeftClass();
                    ResourceClass rightClass = pairedClass.getRightClass();

                    List<Column> genLeftCols = leftClass.toGeneralClass(leftCols, false);
                    List<Column> genRightCols = rightClass.toGeneralClass(rightCols, false);

                    for(int i = 0; i < leftClass.getGeneralClass().getColumnCount(); i++)
                        compare.add(genLeftCols.get(i) + " = " + genRightCols.get(i));
                }

                if(compare.isEmpty())
                    condition.add("true");

                if(!compare.contains("false"))
                    condition.add(compare.stream().sorted().collect(joining(" AND ")));
            }
        }

        assert !condition.isEmpty();

        if(condition.contains("true"))
            return null;

        return condition.stream().sorted().collect(joining(" OR ", "(", ")"));
    }


    public boolean isDistinct(Request request, Collection<String> selected)
    {
        return false;
    }
}
