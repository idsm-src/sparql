package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.rdfLangStringIri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.expression.Literal;



public final class LangStringClass extends LiteralClass implements ResultResourceClass
{
    protected LangStringClass()
    {
        super("lang", rdfLangStringIri, List.of("varchar", "varchar"), Set.of(box));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(this);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(literal.getValue(), "varchar"), constant(literal.getLanguageTag(), "varchar"));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column string = columns.get(0);
        Column lang = columns.get(1);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_langstring(%s, %s)", string, lang));

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
            return List.of(expression("sparql.rdfbox_get_langstring_value(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_langstring_lang(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }
}
