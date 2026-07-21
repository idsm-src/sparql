package cz.iocb.sparql.engine.mapping.classes;



public sealed interface ResultResourceClass
        permits SimpleLiteralClass, CommonIriClass, CommonIntBlankNodeCompositeClass, CommonStrBlankNodeCompositeClass,
        DateTimeCompositeClass, DateCompositeClass, LangStringClass, UnsupportedLiteralClass
{
}
