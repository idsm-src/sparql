package cz.iocb.sparql.engine.model;

import java.util.List;
import cz.iocb.sparql.engine.model.pattern.Pattern;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * The full CONSTRUCT query
 */
public class ConstructQuery extends Query
{
    private final List<Pattern> templates;


    public ConstructQuery(Prologue prologue, List<Pattern> templates, Select select)
    {
        super(prologue, select);
        this.templates = templates;
    }


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
