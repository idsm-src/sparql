package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type;

import java.util.ArrayList;



/**
 * Intersection or And operation between 'children' element types.
 *
 */
public class AndOperator extends TypeElement implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public AndOperator(String value)
    {
        this.value = value;
    }

    /** List of 'children' nodes. */
    public ArrayList<TypeElement> sons = new ArrayList<>();

    @Override
    public <T> T accept(Visitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
