package cz.iocb.chemweb.server.sparql.translator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;



public class SparqlDatabaseConfigurationFactory implements ObjectFactory
{
    @Override
    public Object getObjectInstance(Object object, Name name, Context context, Hashtable<?, ?> environment)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        if(!(object instanceof Reference))
            return null;

        Reference ref = (Reference) object;


        Properties properties = new Properties();
        Enumeration<RefAddr> elements = ref.getAll();

        while(elements.hasMoreElements())
        {
            RefAddr element = elements.nextElement();
            properties.put(element.getType(), element.getContent());
        }


        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String className = ref.getClassName();
        Class<?> objectClass = classLoader != null ? classLoader.loadClass(className) : Class.forName(className);

        Constructor<?> constructor = objectClass.getConstructor(properties.getClass());
        return constructor.newInstance(properties);
    }
}
