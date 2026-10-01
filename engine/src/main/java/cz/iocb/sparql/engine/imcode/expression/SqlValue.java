package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.database.Column.coalesce;
import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isTripleTerm;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.common.UnionFind;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.TripleTermClass;
import cz.iocb.sparql.engine.mapping.classes.TripleTermClass.Component;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Value given directly by its columns per class: a part of another expression (a component of its triple terms) that is
 * compared or converted like an expression of its own. It exists only while the SQL of its parent is generated, so it
 * is neither optimised nor explained.
 */
public final class SqlValue extends SqlExpressionIntercode
{
    /**
     * Creates the value.
     *
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     * @param isDeterministic whether the value is deterministic
     */
    private SqlValue(Map<ResourceClass, List<Column>> mappings, boolean canBeNull, boolean isDeterministic)
    {
        super(mappings, canBeNull, isDeterministic);
    }


    /**
     * The component of the triple terms of the expression taken in the given classes (classes of the expression that
     * may hold triple terms). A triple term class delivers the component in its component class, a class of boxed terms
     * through {@code sparql.rdfbox_get_tripleterm_*} in the box (the predicate in the IRI class), which is NULL when
     * the box holds another kind of term; overlapping component classes are delivered together in their union. The
     * value is null when the triple term is missing or the expression holds another kind of term.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param expression the expression
     * @param classes classes of the expression that may hold triple terms
     * @param component the component
     * @return the component of the triple terms of the expression taken in the given classes
     */
    public static SqlValue tripleTermComponent(ClassRelations relations, SqlExpressionIntercode expression,
            Set<ResourceClass> classes, Component component)
    {
        // the classes of the component, each with the classes of the expression delivering it
        Map<ResourceClass, Set<ResourceClass>> components = new HashMap<>();

        for(ResourceClass resClass : classes)
            components.computeIfAbsent(getComponentClass(resClass, component), _ -> new HashSet<>()).add(resClass);

        Map<ResourceClass, List<Column>> mappings = new HashMap<>();

        for(Set<ResourceClass> group : UnionFind.getDisjunctEntries(components.keySet(),
                (l, r) -> !areDisjunct(relations, l, r)))
        {
            ResourceClass resultClass = group.size() == 1 ? group.iterator().next() : unionize(group);

            List<Set<Column>> variants = new ArrayList<>(resultClass.getColumnCount());

            for(int i = 0; i < resultClass.getColumnCount(); i++)
                variants.add(new HashSet<>());

            for(ResourceClass componentClass : group)
            {
                for(ResourceClass resClass : components.get(componentClass))
                {
                    List<Column> columns = getComponentColumns(resClass, component,
                            expression.get(relations, resClass));

                    if(!componentClass.equals(resultClass))
                        columns = componentClass.toGeneralClass(resultClass, columns, true);

                    for(int i = 0; i < columns.size(); i++)
                        variants.get(i).add(columns.get(i));
                }
            }

            mappings.put(resultClass, variants.stream().map(v -> coalesce(v)).toList());
        }

        boolean canBeNull = expression.canBeNull() || !expression.getResourceClasses().equals(classes)
                || classes.stream().anyMatch(r -> !isTripleTerm(r));

        return new SqlValue(mappings, canBeNull, expression.isDeterministic());
    }


    /**
     * Class of the component of the triple terms of the class: the component class of a triple term class, otherwise
     * (the triple terms are boxed) the box for a subject or an object and the IRI class for a predicate.
     *
     * @param resClass class of the triple terms
     * @param component the component
     * @return class of the component of the triple terms of the class
     */
    private static ResourceClass getComponentClass(ResourceClass resClass, Component component)
    {
        if(resClass.getEffectiveClass() instanceof TripleTermClass tripleClass)
            return tripleClass.getComponentClass(component);

        return component == Component.PREDICATE ? iri : box;
    }


    /**
     * Columns of the component of the triple terms represented by the given columns of the class, in the class given by
     * {@link #getComponentClass}: a part of the columns of a triple term class, otherwise the component extracted from
     * the box (NULL when the box holds another kind of term).
     *
     * @param resClass class of the triple terms
     * @param component the component
     * @param columns the columns representing the triple terms
     * @return columns of the component of the triple terms represented by the given columns of the class
     */
    private static List<Column> getComponentColumns(ResourceClass resClass, Component component, List<Column> columns)
    {
        if(resClass.getEffectiveClass() instanceof TripleTermClass tripleClass)
            return tripleClass.getComponentColumns(component, columns);

        String function = switch(component)
        {
            case SUBJECT -> "sparql.rdfbox_get_tripleterm_subject";
            case PREDICATE -> "sparql.rdfbox_get_tripleterm_predicate";
            case OBJECT -> "sparql.rdfbox_get_tripleterm_object";
        };

        return List.of(new ExpressionColumn(function + "(" + columns.get(0) + ")",
                component == Component.PREDICATE ? VARCHAR : RDFBOX));
    }


    @Override
    public Restrictions getRequirements(ClassRelations relations)
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


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlValue value))
            return false;

        return super.equals(value);
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(getMappings());
    }
}
