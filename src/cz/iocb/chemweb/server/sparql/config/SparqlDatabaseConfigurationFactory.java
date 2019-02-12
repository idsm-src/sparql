package cz.iocb.chemweb.server.sparql.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;



public class SparqlDatabaseConfigurationFactory implements ObjectFactory
{
    @Override
    public Object getObjectInstance(Object object, Name name, Context context, Hashtable<?, ?> environment)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, NamingException
    {
        if(!(object instanceof Reference))
            return null;

        Reference ref = (Reference) object;
        RefAddr datasource = ref.get("datasource");

        if(datasource == null)
            return null;

        Context contextX = (Context) (new InitialContext()).lookup("java:comp/env");
        DataSource connectionPool = (DataSource) contextX.lookup((String) datasource.getContent());

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String className = ref.getClassName();
        Class<?> objectClass = classLoader != null ? classLoader.loadClass(className) : Class.forName(className);

        Constructor<?> constructor = objectClass.getConstructor(DataSource.class);
        return constructor.newInstance(connectionPool);
    }
}
