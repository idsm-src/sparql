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
        /**
         * IRI naming the parameter.
         */
        private final IriNode name;

        /**
         * Value: a variable, IRI, literal or blank node.
         */
        private final Node value;

        /**
         * Creates the parameter.
         *
         * @param name IRI naming the parameter
         * @param value the value node
         */
        public Parameter(IriNode name, Node value)
        {
            this.name = name;
            this.value = value;
        }


        /**
         * IRI naming the parameter.
         *
         * @return IRI naming the parameter
         */
        public IriNode getName()
        {
            return name;
        }


        /**
         * Value: a variable, IRI, literal or blank node.
         *
         * @return value: a variable, IRI, literal or blank node
         */
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


    /**
     * IRI of the procedure.
     */
    private final IriNode procedure;

    /**
     * Parameters of the call.
     */
    private final List<Parameter> parameters;


    /**
     * Creates the call.
     *
     * @param procedure IRI of the procedure
     * @param parameters the parameters
     */
    protected ProcedureCallBase(IriNode procedure, Collection<Parameter> parameters)
    {
        this.procedure = procedure;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
    }


    /**
     * IRI of the procedure.
     *
     * @return IRI of the procedure
     */
    public IriNode getProcedure()
    {
        return procedure;
    }


    /**
     * Parameters of the call.
     *
     * @return parameters of the call
     */
    public List<Parameter> getParameters()
    {
        return parameters;
    }
}
