package cz.iocb.chemweb.server.sparql.translator.visitor.expressions;

/**
 * Flags for translation of an expression (based on the current context).
 */
public enum ExpressionTranslateFlag
{
    /**
     * Temporarily suppresses literal conversions/casting (e.g. input parameters of functions).
     */
    SUPPRESS_LITERAL_CONVERSION,

    /**
     * Special translation of operators (e.g. inside the IF function, or generaly booling in a query projection).
     */
    SPECIAL_RELATION_OPERATORS,

    /**
     * Use DB.DBA.RDF_MAKE_LONG_OF_TYPEDSQLVAL_STRINGS instead of DB.DBA.RDF_MAKE_OBJ_OF_TYPEDSQLVAL.
     */
    LONG_CAST,
}
