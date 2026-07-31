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



public abstract class ResultHandler implements AutoCloseable
{
    protected final Request request;
    protected final Restrictions restrictions;

    private final IriCache iriCache = new IriCache(10000);
    private final Map<Variable, List<UserIriClass>> typeIriClassesMap = new HashMap<>();


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


    private ResourceClass getIriClass(Request request, Iri iri, Variable variable)
    {
        IriClass iriClass = iriCache.getIriClass(iri);

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


    private ResourceClass getLiteralClass(Request request, Literal literal)
    {
        return request.getLiteralClass(literal);
    }


    private ResourceClass getBlankNodeClass(Request request, BlankNode bnode)
    {
        return request.getBlankNodeClass(bnode);
    }


    public List<Column> getColumns(Request request, ResourceClass resClass, RdfTerm term)
    {
        if(resClass instanceof IriClass iriClass && term instanceof Iri iri)
            return getColumns(request, iriClass, iri);

        return resClass.toColumns(request.getStatement(), term);
    }


    public List<Column> getColumns(Request request, IriClass iriClass, Iri iri)
    {
        List<Column> columns = iriCache.getIriColumns(iri);

        if(columns != null)
            return columns;

        iriCache.storeToCache(iri, iriClass, columns);

        return iriClass.toColumns(request.getStatement(), iri);
    }


    public abstract void add(Map<Variable, RdfTerm> row) throws SQLException;


    public abstract SqlIntercode get() throws SQLException;


    public abstract int size();


    @Override
    public abstract void close();
}
