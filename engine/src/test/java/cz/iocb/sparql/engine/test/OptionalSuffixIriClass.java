package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * Test IRI class with an optional column: {@code <prefix><id>} when the second column is NULL, and
 * {@code <prefix><id>/<sub>} otherwise. The first column determines the presence of the term, the second one is
 * optional; the mapping is injective, since the separator distinguishes a missing suffix from any present one.
 */
public class OptionalSuffixIriClass extends UserIriClass
{
    private final String prefix;

    private final String regexp;

    private final Pattern pattern;


    public OptionalSuffixIriClass(String name, String prefix)
    {
        super(name, List.of(INT4, INT4), Set.of(box, iri));

        this.prefix = prefix;
        this.regexp = "^" + Pattern.quote(prefix) + "(0|[1-9][0-9]*)(?:/(0|[1-9][0-9]*))?$";
        this.pattern = Pattern.compile(regexp);
    }


    @Override
    public boolean isOptionalColumn(int index)
    {
        return index == 1;
    }


    @Override
    public boolean match(Statement statement, Iri iri)
    {
        return pattern.matcher(iri.getValue()).matches();
    }


    @Override
    public List<Column> toColumns(Statement statement, Iri iri)
    {
        Matcher matcher = pattern.matcher(iri.getValue());

        if(!matcher.matches())
            throw new IllegalArgumentException();

        Column sub = matcher.group(2) == null ? new ConstantColumn(null, INT4) : constant(matcher.group(2), INT4);

        return List.of(constant(matcher.group(1), INT4), sub);
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column value = expression("(%s || %s::varchar || COALESCE('/' || %s::varchar, ''))::varchar", string(prefix),
                columns.get(0), columns.get(1));

        if(targetClass.equals(iri))
            return List.of(value);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_iri(%s)", value));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        if(!sourceClass.equals(iri) && !sourceClass.equals(box))
            throw new IllegalArgumentException();

        Column value = sourceClass.equals(box) ? expression("sparql.rdfbox_get_iri(%s)", columns.get(0)) :
                columns.get(0);

        // a value not matching the pattern yields NULL in both columns, so the check is never optional
        return List.of(expression("(regexp_match(%s, %s))[1]::int4", value, string(regexp)),
                expression("(regexp_match(%s, %s))[2]::int4", value, string(regexp)));
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return prefix;
    }


    @Override
    public int getCheckCost()
    {
        return 1;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        OptionalSuffixIriClass other = (OptionalSuffixIriClass) object;

        return Objects.equals(prefix, other.prefix);
    }
}
