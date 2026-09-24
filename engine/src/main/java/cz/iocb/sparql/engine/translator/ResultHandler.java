package cz.iocb.sparql.engine.translator;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedIri;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.IriCache;
import cz.iocb.sparql.engine.request.Request;



/**
 * Collects the solutions received from a federated SERVICE endpoint and turns them into intermediate code. It
 * classifies the received terms, trying for each variable the IRI classes admitted by the restrictions and moving the
 * class that matched to the front.
 */
public abstract class ResultHandler implements AutoCloseable
{
    /**
     * Current request.
     */
    protected final Request request;

    /**
     * Variables and classes the parent needs.
     */
    protected final Restrictions restrictions;

    /**
     * Classes and columns of the IRIs received so far.
     */
    private final IriCache iriCache = new IriCache(10000);

    /**
     * Per variable, the IRI classes to try, most recently matched first.
     */
    private final Map<Variable, List<UserIriClass>> typeIriClassesMap = new HashMap<>();


    /**
     * Creates the handler; IRI classes to try are taken from the restrictions of each variable.
     *
     * @param request the current request
     * @param restrictions what the parent needs of the variables
     */
    protected ResultHandler(Request request, Restrictions restrictions)
    {
        this.request = request;
        this.restrictions = restrictions;

        for(Variable var : restrictions.getNames())
        {
            Set<ResourceClass> restriction = restrictions.get(var);

            if(restriction == null || restriction.contains(unsupportedIri))
                typeIriClassesMap.put(var, new LinkedList<>(request.getConfiguration().getIriClasses()));
            else
                typeIriClassesMap.put(var, new LinkedList<>(restriction.stream().filter(c -> c instanceof UserIriClass)
                        .map(c -> (UserIriClass) c).toList()));
        }
    }


    /**
     * Class of the received term bound to the variable.
     *
     * @param request the current request
     * @param value the received term
     * @param variable the variable
     * @return class of the received term bound to the variable
     */
    protected final ResourceClass getResourceClass(Request request, RdfTerm value, Variable variable)
    {
        return switch(value)
        {
            case Literal lit -> getLiteralClass(request, lit);
            case Iri iri -> getIriClass(request, iri, variable);
            case BlankNode bn -> getBlankNodeClass(request, bn);
            default -> null;
        };
    }


    /**
     * Class of a received IRI: the first admitted user class that matches, moved to the front for the next lookup.
     *
     * @param request the current request
     * @param iri the IRI
     * @param variable the variable
     * @return class of a received IRI: the first admitted user class that matches, moved to the front for the next
     *         lookup
     */
    private ResourceClass getIriClass(Request request, Iri iri, Variable variable)
    {
        ResourceClass iriClass = iriCache.getIriClass(iri);

        if(iriClass != null)
            return iriClass;

        List<UserIriClass> iriClasses = typeIriClassesMap.get(variable);

        Iterator<UserIriClass> it = iriClasses.iterator();

        while(it.hasNext())
        {
            UserIriClass resClass = it.next();

            if(resClass.match(request.getStatement(), iri))
            {
                if(!resClass.equals(iriClasses.getFirst()))
                {
                    it.remove();
                    iriClasses.addFirst(resClass);
                }

                return resClass;
            }
        }

        return unsupportedIri;
    }


    /**
     * Class of a received literal.
     *
     * @param request the current request
     * @param literal the literal
     * @return the resulting class
     */
    private ResourceClass getLiteralClass(Request request, Literal literal)
    {
        return request.getLiteralClass(literal);
    }


    /**
     * Class of a received blank node.
     *
     * @param request the current request
     * @param bnode the blank node
     * @return the resulting class
     */
    private ResourceClass getBlankNodeClass(Request request, BlankNode bnode)
    {
        return request.getBlankNodeClass(bnode);
    }


    /**
     * Constant columns representing the term in the class, cached for IRIs.
     *
     * @param request the current request
     * @param resClass the resource class
     * @param term the RDF term
     * @return constant columns representing the term in the class, cached for IRIs
     */
    public List<Column> getColumns(Request request, ResourceClass resClass, RdfTerm term)
    {
        if(resClass instanceof IriClass iriClass && term instanceof Iri iri)
            return getColumns(request, iriClass, iri);

        return resClass.toColumns(request.getStatement(), term);
    }


    /**
     * Constant columns representing the IRI in the class, cached.
     *
     * @param request the current request
     * @param iriClass the IRI class
     * @param iri the IRI
     * @return constant columns representing the IRI in the class, cached
     */
    public List<Column> getColumns(Request request, IriClass iriClass, Iri iri)
    {
        List<Column> columns = iriCache.getIriColumns(iri);

        if(columns != null)
            return columns;

        iriCache.storeToCache(iri, iriClass, columns);

        return iriClass.toColumns(request.getStatement(), iri);
    }


    /**
     * Adds one received solution.
     *
     * @param row the received solution
     * @throws SQLException on database errors
     */
    public abstract void add(Map<Variable, RdfTerm> row) throws SQLException;


    /**
     * Intermediate code producing the collected solutions.
     *
     * @return intermediate code producing the collected solutions
     * @throws SQLException on database errors
     */
    public abstract SqlIntercode get() throws SQLException;


    /**
     * Number of collected solutions.
     *
     * @return number of collected solutions
     */
    public abstract int size();


    @Override
    public abstract void close();
}
