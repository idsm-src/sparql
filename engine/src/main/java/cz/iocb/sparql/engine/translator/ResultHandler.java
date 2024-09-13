package cz.iocb.sparql.engine.translator;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.BlankNodeLiteral;
import cz.iocb.sparql.engine.mapping.classes.DataType;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.request.IriCache;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode;



public abstract class ResultHandler implements AutoCloseable
{
    final private IriCache iriCache = new IriCache(10000);
    final private HashMap<String, List<UserIriClass>> typeIriClassesMap = new HashMap<String, List<UserIriClass>>();


    protected final ResourceClass getResourceClass(Request request, Node value, String variable)
    {
        if(value instanceof Literal)
        {
            Literal literal = (Literal) value;
            DataType datatype = request.getConfiguration().getDataType(literal.getTypeIri());
            LiteralClass resourceClass = datatype == null ? unsupportedLiteral : datatype.getResourceClass(literal);

            return resourceClass;
        }
        else if(value instanceof IRI iri)
        {
            return getIriClass(request, iri, variable);
        }
        else if(value instanceof BlankNodeLiteral)
        {
            return ((BlankNodeLiteral) value).getResourceClass();
        }
        else
        {
            return null;
        }
    }


    private ResourceClass getIriClass(Request request, IRI iri, String variable)
    {
        IriClass iriClass = iriCache.getIriClass(iri);

        if(iriClass != null)
            return iriClass;

        List<UserIriClass> iriClasses = typeIriClassesMap.computeIfAbsent(variable,
                k -> new LinkedList<UserIriClass>(request.getConfiguration().getIriClasses()));

        Iterator<UserIriClass> it = iriClasses.iterator();

        while(it.hasNext())
        {
            UserIriClass resClass = it.next();

            if(resClass.match(request.getStatement(), iri))
            {
                if(resClass != iriClasses.getFirst())
                {
                    it.remove();
                    iriClasses.addFirst(resClass);
                }

                return resClass;
            }
        }

        return unsupportedIri;
    }


    public List<Column> getColumns(Request request, ResourceClass resClass, Node node)
    {
        if(resClass instanceof IriClass iriClass && node instanceof IRI iri)
            return getColumns(request, iriClass, iri);

        return resClass.toColumns(request.getStatement(), node);
    }


    public List<Column> getColumns(Request request, IriClass iriClass, IRI iri)
    {
        List<Column> columns = iriCache.getIriColumns(iri);

        if(columns != null)
            return columns;

        iriCache.storeToCache(iri, iriClass, columns);

        return iriClass.toColumns(request.getStatement(), iri);
    }


    public abstract void add(Map<String, Node> row) throws SQLException;


    public abstract SqlIntercode get() throws SQLException;


    public abstract int size();


    @Override
    public abstract void close();
}
