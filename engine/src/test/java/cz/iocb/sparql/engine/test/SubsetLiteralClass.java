package cz.iocb.sparql.engine.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.CanonicalLiteralClass;
import cz.iocb.sparql.engine.mapping.classes.PrimitiveResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResultResourceClass;
import cz.iocb.sparql.engine.mapping.classes.SimpleLiteralClass;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Literal class standing in for a deployment-defined subclass of a built-in literal class: it stores the same values in
 * the same columns as the original class, but is a distinct, more specific class. {@link SparqlTest} maps the literals
 * of the test data to these classes to exercise the conversions between a class and its superclasses.
 */
public class SubsetLiteralClass extends CanonicalLiteralClass
{
    /**
     * The built-in class this class is a subclass of.
     */
    private final SimpleLiteralClass original;


    /**
     * Creates the subclass of {@code org}, named {@code <org>_sub}.
     */
    public SubsetLiteralClass(SimpleLiteralClass org)
    {
        Set<PrimitiveResourceClass> superClasses = new HashSet<>();
        superClasses.addAll(org.getSuperClasses());
        superClasses.add(org);

        super(org.getResourceName() + "_sub", org.getDatatype(), org.getSqlTypes(), superClasses);

        original = org;
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return original.getResultResourceClasses();
    }


    @Override
    public PrimitiveResourceClass getEffectiveClass()
    {
        return original.getEffectiveClass();
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return original.toColumns(literal);
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        ResourceClass sourceClass = superClass.getEffectiveClass();

        if(sourceClass.equals(this))
            return columns;

        return original.toGeneralClass(superClass.getEffectiveClass(), columns, canBeNull);
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
    {
        ResourceClass sourceClass = superClass.getEffectiveClass();

        if(sourceClass.equals(this))
            return columns;

        return original.fromGeneralClass(superClass, columns, checkOptional);
    }
}
