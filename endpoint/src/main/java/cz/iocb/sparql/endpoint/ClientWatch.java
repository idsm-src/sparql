package cz.iocb.sparql.endpoint;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.iocb.sparql.engine.request.Request;



/**
 * Watch over the connection of the client of a request, which cancels the request (see {@link Request#cancel}) once the
 * client has closed the connection, so that a query nobody waits for any more, whose SQL statement or SERVICE call may
 * run long before the first row of its result, does not hold the database till its time limit. The Servlet API cannot
 * tell whether the client is still there without writing to it, so the watch reads the socket of the connection through
 * the internal classes of Tomcat: every so often (see {@link ServletSettings#clientWatchInterval()}) a read that does
 * not block, which finds the end of the stream, or an error, once the client has closed the connection (a client that
 * has closed only its sending side is taken for gone, as nginx takes it), while bytes found there, a request a client
 * sends before it has the response to the previous one, are given back to Tomcat and end the watch, as the client is
 * there.
 * <p>
 * Watched are the requests that the HTTP/1.1 and the AJP processors of Tomcat serve over the NIO connector without TLS;
 * for any other one, like a stream of HTTP/2, a connection with TLS or a request of another container, the watch does
 * nothing. Behind a proxy, the connection is that of the proxy, which tells that its client has left only if it closes
 * its connection then: nginx closes it (unless {@code proxy_ignore_client_abort} is on), whereas Apache httpd closes it
 * only once it has failed to write the response to its client, which is too late for a request that has sent nothing
 * yet. The watch reads by a virtual thread of its own, and it ends before the request does, as Tomcat reads the next
 * request of the connection then.
 */
final class ClientWatch implements AutoCloseable
{
    /**
     * Size of the buffer of a read; Tomcat reads into a buffer of this size without copying the bytes.
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * Names of the classes of the processors of Tomcat whose connections are watched: HTTP/1.1 and AJP.
     */
    private static final Set<String> processors = Set.of("org.apache.coyote.http11.Http11Processor",
            "org.apache.coyote.ajp.AjpProcessor");

    /**
     * Logger of the watch.
     */
    private static final Logger logger = LoggerFactory.getLogger(ClientWatch.class);

    /**
     * Whether it has been logged that a request of a Tomcat-like container cannot be watched.
     */
    private static final AtomicBoolean reported = new AtomicBoolean();

    /**
     * The request to cancel.
     */
    private final Request request;

    /**
     * The socket wrapper of Tomcat for the connection, null if the request is not watched.
     */
    private final Object wrapper;

    /**
     * The method reading the socket wrapper, {@code read(boolean block, ByteBuffer to)}.
     */
    private final Method read;

    /**
     * The method giving bytes back to the socket wrapper, {@code unRead(ByteBuffer returned)}.
     */
    private final Method unRead;

    /**
     * Time between two reads of the socket in milliseconds.
     */
    private final long interval;

    /**
     * Buffer of the reads.
     */
    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    /**
     * Whether the watch has ended, guarded by the monitor of the watch.
     */
    private boolean stopped = false;

    /**
     * Whether the client has been found to have closed the connection.
     */
    private volatile boolean closed = false;


    /**
     * Creates the watch of the connection.
     *
     * @param request the request to cancel
     * @param wrapper the socket wrapper of Tomcat for the connection, null if the request is not watched
     * @param interval time between two reads of the socket in milliseconds
     * @throws ReflectiveOperationException if the socket wrapper has not the methods the watch reads by
     */
    private ClientWatch(Request request, Object wrapper, long interval) throws ReflectiveOperationException
    {
        this.request = request;
        this.wrapper = wrapper;
        this.read = wrapper != null ? wrapper.getClass().getMethod("read", boolean.class, ByteBuffer.class) : null;
        this.unRead = wrapper != null ? wrapper.getClass().getMethod("unRead", ByteBuffer.class) : null;
        this.interval = interval;
    }


    /**
     * Starts the watch of the connection of the client of a request, which does nothing if the request is not one that
     * can be watched.
     *
     * @param req the HTTP request
     * @param request the request to cancel once the client has closed the connection
     * @param interval time between two reads of the socket, at least a millisecond
     * @return the watch of the connection
     */
    static ClientWatch start(ServletRequest req, Request request, Duration interval)
    {
        try
        {
            ClientWatch watch = new ClientWatch(request, getSocketWrapper(req), interval.toMillis());

            if(watch.wrapper != null)
                Thread.ofVirtual().name("client watch").start(watch::watch);

            return watch;
        }
        catch(ReflectiveOperationException | RuntimeException e)
        {
            // the classes of Tomcat are not those the watch knows, or the access to them is denied
            if(!reported.getAndSet(true))
                logger.warn("the connections of the clients cannot be watched: " + e);

            try
            {
                return new ClientWatch(request, null, interval.toMillis());
            }
            catch(ReflectiveOperationException x)
            {
                throw new IllegalStateException(x);
            }
        }
    }


    /**
     * The socket wrapper of Tomcat for the connection of the request, found through the internal classes of Tomcat: the
     * request of the connector that the facade given to a servlet holds, its request of the protocol, whose hook is the
     * processor serving it, and the socket wrapper of the processor.
     *
     * @param req the HTTP request
     * @return the socket wrapper for the connection, or null if the request is not one that can be watched
     * @throws ReflectiveOperationException if the classes are not those the watch knows
     */
    private static Object getSocketWrapper(ServletRequest req) throws ReflectiveOperationException
    {
        while(req instanceof ServletRequestWrapper wrapper)
            req = wrapper.getRequest();

        if(!req.getClass().getName().equals("org.apache.catalina.connector.RequestFacade"))
            return null;

        Object connectorRequest = getField(req, "request");
        Object coyoteRequest = connectorRequest.getClass().getMethod("getCoyoteRequest").invoke(connectorRequest);
        Object processor = getField(coyoteRequest, "hook");

        if(processor == null || !processors.contains(processor.getClass().getName()))
            return null;

        Method getSocketWrapper = getMethod(processor.getClass(), "getSocketWrapper");
        getSocketWrapper.setAccessible(true);
        Object wrapper = getSocketWrapper.invoke(processor);

        if(wrapper == null
                || !wrapper.getClass().getName().equals("org.apache.tomcat.util.net.NioEndpoint$NioSocketWrapper"))
            return null;

        // a channel with TLS, which may have to write while it reads, and the channel of a closed socket are not read
        Object channel = wrapper.getClass().getMethod("getSocket").invoke(wrapper);

        if(channel == null || !channel.getClass().getName().equals("org.apache.tomcat.util.net.NioChannel"))
            return null;

        return wrapper;
    }


    /**
     * Value of a field that the class of an object, or one of its superclasses, declares.
     *
     * @param object the object
     * @param name the name of the field
     * @return value of the field
     * @throws ReflectiveOperationException if there is no such field
     */
    private static Object getField(Object object, String name) throws ReflectiveOperationException
    {
        for(Class<?> type = object.getClass(); type != null; type = type.getSuperclass())
        {
            try
            {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            }
            catch(NoSuchFieldException e)
            {
                // declared by a superclass
            }
        }

        throw new NoSuchFieldException(name);
    }


    /**
     * Method without parameters that a class, or one of its superclasses, declares.
     *
     * @param type the class
     * @param name the name of the method
     * @return the method
     * @throws NoSuchMethodException if there is no such method
     */
    private static Method getMethod(Class<?> type, String name) throws NoSuchMethodException
    {
        for(Class<?> declaring = type; declaring != null; declaring = declaring.getSuperclass())
        {
            try
            {
                return declaring.getDeclaredMethod(name);
            }
            catch(NoSuchMethodException e)
            {
                // declared by a superclass
            }
        }

        throw new NoSuchMethodException(name);
    }


    /**
     * Reads the socket every {@link #interval} milliseconds until the watch is closed, the client has closed the
     * connection, which cancels the request, or bytes arrive; run by the thread of the watch.
     */
    private void watch()
    {
        synchronized(this)
        {
            try
            {
                while(!stopped)
                {
                    wait(interval);

                    if(stopped)
                        break;

                    int state = probe();

                    if(state != 0)
                        stopped = true;

                    if(state < 0)
                        closed = true;
                }
            }
            catch(InterruptedException e)
            {
                return;
            }
        }

        if(closed)
        {
            try
            {
                request.cancel();
            }
            catch(SQLException e)
            {
                logger.warn("the request cannot be cancelled: " + e.getMessage());
            }
        }
    }


    /**
     * Reads the socket without blocking, giving the bytes read back to Tomcat.
     *
     * @return a negative number if the client has closed the connection, zero if nothing has arrived, and a positive
     *         number if bytes have arrived or the socket cannot be read
     */
    private int probe()
    {
        try
        {
            buffer.clear();

            int count = (Integer) read.invoke(wrapper, false, buffer);

            if(count > 0)
            {
                buffer.flip();
                unRead.invoke(wrapper, buffer);
            }

            return count;
        }
        catch(InvocationTargetException e)
        {
            // the socket fails once the client has closed the connection (or Tomcat has closed the socket)
            if(e.getCause() instanceof IOException)
                return -1;

            logger.warn("the connection of the client cannot be read: " + e.getCause());
            return 1;
        }
        catch(ReflectiveOperationException | RuntimeException e)
        {
            logger.warn("the connection of the client cannot be read: " + e);
            return 1;
        }
    }


    /**
     * True if the client has been found to have closed the connection, which has cancelled the request.
     *
     * @return true if the client has been found to have closed the connection, false otherwise
     */
    boolean isClosed()
    {
        return closed;
    }


    /**
     * Ends the watch, waiting for a read of the socket in progress; the cancellation of the request of a client found
     * to have closed the connection may be still in progress.
     */
    @Override
    public synchronized void close()
    {
        stopped = true;
        notifyAll();
    }
}
