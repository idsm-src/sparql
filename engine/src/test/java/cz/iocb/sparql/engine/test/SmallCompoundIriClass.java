package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;



/**
 * Test IRI class declared as a subclass of a given integer IRI class: the IRIs of the superclass whose id is below one
 * hundred, stored in the same int4 column. Converting from the superclass checks the bound unless the check may be
 * skipped.
 */
public class SmallCompoundIriClass extends UserIriClass
{
    private final IntegerUserIriClass compound;

    private final String prefix;

    private final Pattern pattern;


    public SmallCompoundIriClass(String name, IntegerUserIriClass compound, String prefix)
    {
        super(name, List.of(INT4), Set.of(compound, iri, box));

        this.compound = compound;
        this.prefix = prefix;
        this.pattern = Pattern.compile("^" + Pattern.quote(prefix) + "(0|[1-9][0-9]?)$");
    }


    @Override
    public boolean match(Request request, Iri iri)
    {
        return pattern.matcher(iri.getValue()).matches();
    }


    @Override
    public List<Column> toColumns(Request request, Iri iri)
    {
        Matcher matcher = pattern.matcher(iri.getValue());

        if(!matcher.matches())
            throw new IllegalArgumentException();

        return List.of(constant(matcher.group(1), INT4));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this) || targetClass.equals(compound))
            return columns;

        return compound.toGeneralClass(targetClass, columns, canBeNull);
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        List<Column> ids = sourceClass.equals(compound) ? columns :
                compound.fromGeneralClass(sourceClass, columns, checkOptional);

        if(checkOptional)
            return ids;

        return List.of(expression(INT4, "CASE WHEN %s < 100 THEN %s END", ids.get(0), ids.get(0)));
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return prefix;
    }


    @Override
    public int getCheckCost()
    {
        return 0;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        SmallCompoundIriClass other = (SmallCompoundIriClass) object;

        return Objects.equals(compound, other.compound) && Objects.equals(prefix, other.prefix);
    }
}
