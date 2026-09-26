package cz.iocb.sparql.engine.mapping.classes;



/**
 * Marker of the classes in which values are delivered to the query result. Every class maps to one or more of them
 * ({@link ResourceClass#getResultResourceClasses}), and the generated SELECT list holds one column group per result
 * class of each projected variable. The box is one of them: a value whose class cannot be narrowed is delivered as the
 * text form of its {@code sparql.rdfbox} column and decoded by {@link cz.iocb.sparql.engine.request.RdfBoxParser}.
 */
public sealed interface ResultResourceClass permits RdfBoxClass, IriScalarClass, IntBlankNodeCompositeClass,
        StrBlankNodeCompositeClass, SimpleLiteralClass, SimpleLiteralBaseClass, DateTimeCompositeClass,
        DateTimeCompositeBaseClass, DateCompositeClass, DateCompositeBaseClass, LangStringClass,
        UserLiteralCompositeClass, UserLiteralCompositeBaseClass, UnsupportedLiteralClass
{
}
