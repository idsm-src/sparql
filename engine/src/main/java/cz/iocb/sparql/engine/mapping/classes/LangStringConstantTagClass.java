package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.rdfLangStringIri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.expression.Literal;



public final class LangStringConstantTagClass extends LiteralClass
{
    private static final ConcurrentMap<String, LangStringConstantTagClass> instances = new ConcurrentHashMap<>();

    private final String tag;


    private LangStringConstantTagClass(String tag)
    {
        super("lang-" + tag, rdfLangStringIri, List.of("varchar"), Set.of(box, rdfLangString));
        this.tag = tag;
    }


    public static LangStringConstantTagClass get(String tag)
    {
        return instances.computeIfAbsent(tag.toLowerCase(), LangStringConstantTagClass::new);
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(rdfLangString);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(literal.getValue(), "varchar"));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column string = columns.get(0);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_langstring(%s, '%s'::varchar)", string, tag));

        if(targetClass.equals(rdfLangString))
            return List.of(string, !canBeNull ? constant(tag, "varchar") :
                    expression("CASE WHEN %s IS NOT NULL THEN '%s'::varchar END", string, tag));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        if(sourceClass.equals(box))
            return List.of(
                    expression("sparql.rdfbox_get_langstring_value_of_lang(%s, '%s'::varchar)", columns.get(0), tag));

        if(sourceClass.equals(rdfLangString))
            return List.of(expression("CASE WHEN %s = '%s'::varchar THEN %s END", columns.get(1), tag, columns.get(0)));

        throw new IllegalArgumentException();
    }


    @Override
    public boolean match(Statement statement, Literal literal)
    {
        return super.match(statement, literal) && Objects.equals(literal.getLanguageTag(), tag);
    }


    public String getTag()
    {
        return tag;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        LangStringConstantTagClass other = (LangStringConstantTagClass) object;

        return Objects.equals(tag, other.tag);
    }
}
