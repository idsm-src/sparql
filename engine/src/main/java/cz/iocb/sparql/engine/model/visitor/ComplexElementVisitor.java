package cz.iocb.sparql.engine.model.visitor;

import cz.iocb.sparql.engine.model.base.ComplexElement;
import cz.iocb.sparql.engine.model.base.Element;
import cz.iocb.sparql.engine.model.triple.BlankNodePropertyList;
import cz.iocb.sparql.engine.model.triple.ComplexTriple;
import cz.iocb.sparql.engine.model.triple.Property;
import cz.iocb.sparql.engine.model.triple.RdfCollection;



/**
 * Abstract visitor for {@link ComplexElement}s.
 *
 * @param <T> the result type of the visit
 */
public abstract class ComplexElementVisitor<T> extends ElementVisitor<T>
{
    /**
     * Creates the visitor.
     */
    protected ComplexElementVisitor()
    {
    }

    @Override
    public T visitElement(Element element)
    {
        if(element instanceof ComplexElement complexElement)
        {
            return visitElement(complexElement);
        }

        return super.visitElement(element);
    }


    /**
     * Visits a complex element; returns null for a null element.
     *
     * @param element the element to visit
     * @return the result of the visit, or null for a null element
     */
    public T visitElement(ComplexElement element)
    {
        if(element == null)
            return null;

        return element.accept(this);
    }


    /**
     * Visits a complex triple; returns the default result unless overridden.
     *
     * @param complexTriple the visited element
     * @return the result of the visit
     */
    public T visit(ComplexTriple complexTriple)
    {
        return defaultResult();
    }


    /**
     * Visits an RDF collection; returns the default result unless overridden.
     *
     * @param rdfCollection the visited element
     * @return the result of the visit
     */
    public T visit(RdfCollection rdfCollection)
    {
        return defaultResult();
    }


    /**
     * Visits a blank node property list; returns the default result unless overridden.
     *
     * @param blankNodePropertyList the visited element
     * @return the result of the visit
     */
    public T visit(BlankNodePropertyList blankNodePropertyList)
    {
        return defaultResult();
    }


    /**
     * Visits a property; returns the default result unless overridden.
     *
     * @param property the visited element
     * @return the result of the visit
     */
    public T visit(Property property)
    {
        return defaultResult();
    }
}
