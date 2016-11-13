package cz.iocb.chemweb.server.sparql.parser.model.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



/**
 * Common base class for procedure calls.
 */
public abstract class ProcedureCallBase extends BaseElement implements BasicPattern
{
    private IRI procedure;
    private List<Parameter> parameters;

    protected ProcedureCallBase()
    {
        parameters = new ArrayList<>();
    }

    protected ProcedureCallBase(IRI procedure, Collection<Parameter> parameters)
    {
        this.procedure = procedure;
        this.parameters = new ArrayList<>(parameters);
    }

    public IRI getProcedure()
    {
        return procedure;
    }

    public void setProcedure(IRI procedure)
    {
        this.procedure = procedure;
    }

    public List<Parameter> getParameters()
    {
        return parameters;
    }

    /**
     * Parameter of a procedure call, which has a name ({@link #getName}) and a value ({@link #getValue}).
     */
    public static class Parameter extends BaseElement
    {
        private IRI name;
        private Node value;

        public Parameter()
        {
        }

        public Parameter(IRI name, Node value)
        {
            this.name = name;
            this.value = value;
        }

        public IRI getName()
        {
            return name;
        }

        public void setName(IRI name)
        {
            this.name = name;
        }

        public Node getValue()
        {
            return value;
        }

        public void setValue(Node value)
        {
            this.value = value;
        }

        @Override
        public <T> T accept(ElementVisitor<T> visitor)
        {
            return visitor.visit(this);
        }
    }
}
