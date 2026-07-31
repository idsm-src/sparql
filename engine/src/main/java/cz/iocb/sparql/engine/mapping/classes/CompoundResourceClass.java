package cz.iocb.sparql.engine.mapping.classes;

import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.RdfTerm;



public abstract class CompoundResourceClass extends ResourceClass
{
    private final PrimitiveResourceClass effectiveClass;


    protected CompoundResourceClass(String name, PrimitiveResourceClass effectiveClass)
    {
        super(name);

        this.effectiveClass = effectiveClass;
    }


    @Override
    public List<Column> toColumns(Statement statement, RdfTerm term)
    {
        return effectiveClass.toColumns(statement, term);
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        return effectiveClass.toGeneralClass(superClass.getEffectiveClass(), columns, canBeNull);
    }



    @Override
    public PrimitiveResourceClass getEffectiveClass()
    {
        return effectiveClass;
    }


    @Override
    public ResourceClass getBuiltinClass()
    {
        return effectiveClass.getBuiltinClass();
    }



    @Override
    public List<String> getSqlTypes()
    {
        return effectiveClass.getSqlTypes();
    }


    @Override
    public int getColumnCount()
    {
        return effectiveClass.getColumnCount();
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        CompoundResourceClass other = (CompoundResourceClass) object;

        return Objects.equals(effectiveClass, other.effectiveClass);
    }
}
