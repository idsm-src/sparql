package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type;

import java.util.StringJoiner;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;



/**
 * Visitor that builds infix representation of type tree.
 *
 */
public class PrintInfix implements Visitor<String>
{
    private final Prologue prologue;

    public PrintInfix(Prologue prologue)
    {
        this.prologue = prologue;
    }

    public String visitTree(TypeElement tree)
    {
        return tree.accept(this);
    }

    /**
     * Return constraint value string.
     *
     * @param constraint constraint.
     * @return Class IRI String - constraint
     */
    @Override
    public String visit(Constraint constraint)
    {
        if(prologue != null)
            return new IRI(constraint.value).toString(prologue, false, false);
        else
            return constraint.value;
    }

    /**
     * Join child types using And value String.
     *
     * @param and And operator
     * @return infix representation of And operator
     */
    @Override
    public String visit(AndOperator and)
    {
        StringJoiner sj = new StringJoiner(" & ", "(", ")");
        and.sons.stream().forEach((t) -> {
            sj.add(t.accept(this));
        });
        return sj.toString();
    }

    /**
     * Join child types using Or value String.
     *
     * @param or Or operator
     * @return infix representation of Or operator
     */
    @Override
    public String visit(OrOperator or)
    {
        StringJoiner sj = new StringJoiner(" | ", "(", ")");
        or.sons.stream().forEach((t) -> {
            sj.add(t.accept(this));
        });
        return sj.toString();
    }

    @Override
    public String visit(NegOperator neg)
    {
        StringBuffer buf = new StringBuffer();
        buf.append("!");
        buf.append(neg.son.accept(this));
        return buf.toString();
    }
}
