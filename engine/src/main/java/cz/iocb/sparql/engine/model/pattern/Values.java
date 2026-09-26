package cz.iocb.sparql.engine.model.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.expression.Expression;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Pattern that assigns constant values ({@link #getValuesLists}) to variables ( {@link #getVariables}).
 *
 * <p>
 * Corresponds to the rules [30] ValuesClause and [65] InlineData in the SPARQL grammar.
 */
public class Values extends PatternElement implements Pattern
{
    /**
     * Single assignment of constant values ({@link #getValues}) to the variables from {@link Values}.
     *
     * <p>
     * Note: {@code null} value in the list represents {@code UNDEF}.
     */
    public static class ValuesList extends BaseElement
    {
        /**
         * Values in the order of the variables; null for UNDEF.
         */
        private final List<Expression> values;

        /**
         * Creates the row.
         *
         * @param values the values, null for UNDEF
         */
        public ValuesList(Collection<Expression> values)
        {
            this.values = Collections.unmodifiableList(new ArrayList<>(values));
        }


        /**
         * Values in the order of the variables; null for UNDEF.
         *
         * @return values in the order of the variables; null for UNDEF
         */
        public List<Expression> getValues()
        {
            return values;
        }

        @Override
        public <T> T accept(ElementVisitor<T> visitor)
        {
            return visitor.visit(this);
        }
    }


    /**
     * Variables being assigned.
     */
    private final List<VariableNode> variables;

    /**
     * Rows of values.
     */
    private final List<ValuesList> valuesLists;


    /**
     * Creates the pattern; the variables become in scope.
     *
     * @param variables the variables
     * @param valuesLists the rows of values
     */
    public Values(Collection<VariableNode> variables, Collection<ValuesList> valuesLists)
    {
        this.variables = Collections.unmodifiableList(new ArrayList<>(variables));
        this.valuesLists = Collections.unmodifiableList(new ArrayList<>(valuesLists));

        variablesInScope.addAll(variables);
    }


    /**
     * Variables being assigned.
     *
     * @return variables being assigned
     */
    public List<VariableNode> getVariables()
    {
        return variables;
    }


    /**
     * Rows of values.
     *
     * @return rows of values
     */
    public List<ValuesList> getValuesLists()
    {
        return valuesLists;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
