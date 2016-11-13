package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type;

/**
 * Type constraint - a class identifier.
 *
 */
public class Constraint extends TypeElement implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Create constraint.
     *
     * @param value Value of Constraint - a class IRI.
     */
    public Constraint(String value)
    {
        this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
