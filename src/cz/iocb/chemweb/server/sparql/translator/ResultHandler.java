package cz.iocb.chemweb.server.sparql.translator;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedIri;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.BlankNodeLiteral;
import cz.iocb.chemweb.server.sparql.mapping.classes.DataType;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlIntercode;



public abstract class ResultHandler implements AutoCloseable
{
    final private HashMap<String, List<UserIriClass>> typeIriClassesMap = new HashMap<String, List<UserIriClass>>();


    protected final ResourceClass getType(Node value, String variable)
    {
        if(value instanceof Literal)
        {
            Literal literal = (Literal) value;
            DataType datatype = Request.currentRequest().getConfiguration().getDataType(literal.getTypeIri());
            LiteralClass resourceClass = datatype == null ? unsupportedLiteral : datatype.getResourceClass(literal);

            return resourceClass;
        }
        else if(value instanceof IRI)
        {
            List<UserIriClass> iriClasses = typeIriClassesMap.computeIfAbsent(variable,
                    k -> new LinkedList<UserIriClass>(Request.currentRequest().getConfiguration().getIriClasses()));

            Iterator<UserIriClass> it = iriClasses.iterator();

            while(it.hasNext())
            {
                UserIriClass resClass = it.next();

                if(resClass.match(value))
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
        else if(value instanceof BlankNodeLiteral)
        {
            return ((BlankNodeLiteral) value).getResourceClass();
        }
        else
        {
            return null;
        }
    }


    public abstract void add(Map<String, Node> row) throws SQLException;


    public abstract SqlIntercode get() throws SQLException;


    public abstract int size();


    @Override
    public abstract void close();
}
