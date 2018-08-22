package cz.iocb.chemweb.server.sparql.mapping.classes.interfaces;

import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public interface ExpressionLiteralClass extends ExpressionResourceClass, LiteralClass
{
    /**
     * Get the SQL value for the given SPARQL query literal
     *
     * @param literal SPARQL query node
     * @return SQL value
     */
    public String getSqlLiteralValue(Literal literal);
}
