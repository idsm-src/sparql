package cz.iocb.sparql.engine.mapping.classes;



public sealed interface ResultResourceClass permits SimpleLiteralClass, SimpleLiteralBaseClass, DateCompositeBaseClass,
        DateTimeCompositeBaseClass, IriScalarClass, IntBlankNodeCompositeClass, StrBlankNodeCompositeClass,
        DateTimeCompositeClass, DateCompositeClass, LangStringClass, UserLiteralCompositeBaseClass,
        UserLiteralCompositeClass, UnsupportedLiteralClass
{
}
