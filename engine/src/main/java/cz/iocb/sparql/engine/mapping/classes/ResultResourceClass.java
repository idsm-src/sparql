package cz.iocb.sparql.engine.mapping.classes;



public sealed interface ResultResourceClass permits IriScalarClass, IntBlankNodeCompositeClass,
        StrBlankNodeCompositeClass, SimpleLiteralClass, SimpleLiteralBaseClass, DateTimeCompositeClass,
        DateTimeCompositeBaseClass, DateCompositeClass, DateCompositeBaseClass, LangStringClass,
        UserLiteralCompositeClass, UserLiteralCompositeBaseClass, UnsupportedLiteralClass
{
}
