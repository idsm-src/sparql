package cz.iocb.sparql.engine.model.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.triple.Node;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Common base class for procedure calls.
 */
public abstract class ProcedureCallBase extends PatternElement implements BasicPattern
{
    /**
     * Parameter of a procedure call, which has a name ({@link #getName}) and a value ({@link #getValue}).
     */
    public static class Parameter extends BaseElement
    {
        private final IriNode name;
        private final Node value;

        public Parameter(IriNode name, Node value)
        {
            this.name = name;
            this.value = value;
        }

        public IriNode getName()
        {
            return name;
        }

        public Node getValue()
        {
            return value;
        }

        @Override
        public <T> T accept(ElementVisitor<T> visitor)
        {
            return visitor.visit(this);
        }
    }


    private final IriNode procedure;
    private final List<Parameter> parameters;


    protected ProcedureCallBase(IriNode procedure, Collection<Parameter> parameters)
    {
        this.procedure = procedure;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
    }


    public IriNode getProcedure()
    {
        return procedure;
    }


    public List<Parameter> getParameters()
    {
        return parameters;
    }
}
