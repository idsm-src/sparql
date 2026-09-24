package cz.iocb.sparql.engine.model;

import java.util.List;
import cz.iocb.sparql.engine.model.pattern.Pattern;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * The full CONSTRUCT query
 */
public class ConstructQuery extends Query
{
    /**
     * Triple templates of the CONSTRUCT clause.
     */
    private final List<Pattern> templates;


    /**
     * Creates the query from its prologue, templates and select part.
     *
     * @param prologue the prologue of the query
     * @param templates the triple templates
     * @param select the query body
     */
    public ConstructQuery(Prologue prologue, List<Pattern> templates, Select select)
    {
        super(prologue, select);
        this.templates = templates;
    }


    /**
     * Triple templates of the CONSTRUCT clause.
     *
     * @return triple templates of the CONSTRUCT clause
     */
    public List<Pattern> getTemplates()
    {
        return templates;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
