package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type;

/**
 * Type element visitor interface.
 * 
 * @param <T> Return type
 */
public interface Visitor<T>
{
    T visit(Constraint aThis);

    T visit(AndOperator aThis);

    T visit(OrOperator aThis);

    T visit(NegOperator aThis);
}
