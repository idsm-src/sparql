package cz.iocb.sparql.engine.model.visitor;

import cz.iocb.sparql.engine.model.base.ComplexElement;
import cz.iocb.sparql.engine.model.base.Element;
import cz.iocb.sparql.engine.model.triple.BlankNodePropertyList;
import cz.iocb.sparql.engine.model.triple.ComplexTriple;
import cz.iocb.sparql.engine.model.triple.Property;
import cz.iocb.sparql.engine.model.triple.RdfCollection;



/**
 * Abstract visitor for {@link ComplexElement}s.
 */
public abstract class ComplexElementVisitor<T> extends ElementVisitor<T>
{
    @Override
    public T visitElement(Element element)
    {
        if(element instanceof ComplexElement complexElement)
        {
            return visitElement(complexElement);
        }

        return super.visitElement(element);
    }


    public T visitElement(ComplexElement element)
    {
        if(element == null)
            return null;

        return element.accept(this);
    }


    public T visit(ComplexTriple complexTriple)
    {
        return defaultResult();
    }


    public T visit(RdfCollection rdfCollection)
    {
        return defaultResult();
    }


    public T visit(BlankNodePropertyList blankNodePropertyList)
    {
        return defaultResult();
    }


    public T visit(Property property)
    {
        return defaultResult();
    }
}
