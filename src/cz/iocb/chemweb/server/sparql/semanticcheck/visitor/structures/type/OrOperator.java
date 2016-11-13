package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type;

import java.util.ArrayList;



/**
 * Union or Or operation between 'children' element types.
 *
 */
public class OrOperator extends TypeElement implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public OrOperator(String value)
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
