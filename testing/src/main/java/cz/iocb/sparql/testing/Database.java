package cz.iocb.sparql.testing;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.postgresql.Driver;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;



/**
 * Test database shared by the tests of the engine and of the endpoint: a PostgreSQL 16 container built from the
 * {@code docker} directory of the resources of this module, which clones and compiles the pgsparql extension from the
 * public {@code next} branch on GitHub and loads the NeXtProt test schemas. Local, unpushed changes of pgsparql are
 * therefore not visible to the tests. The container is started once per JUnit launcher session by
 * {@link DatabaseLauncherSessionListener}, which this module registers in {@code META-INF/services}, so that every
 * module depending on this one gets the database.
 */
public class Database
{
    /**
     * Pool connected to the running container; null until {@link #start} has been called.
     */
    private static DataSource connectionPool;


    /**
     * Not instantiable: the database is shared through the static methods.
     */
    private Database()
    {
    }


    /**
     * Builds the image (without cache, so the latest pgsparql is fetched) and starts the container on first call; later
     * calls are no-ops.
     */
    public static synchronized void start()
    {
        if(connectionPool == null)
        {
            String imageName = new ImageFromDockerfile("sparql-test", false).withFileFromClasspath(".", "docker")
                    .withBuildImageCmdModifier(cmd -> cmd.withNoCache(true)).get();
            DockerImageName image = DockerImageName.parse(imageName).asCompatibleSubstituteFor("postgres");

            @SuppressWarnings("resource")
            PostgreSQLContainer<?> container = new PostgreSQLContainer<>(image);
            container.setShmSize(1L << 30);
            container.start();

            PoolProperties p = new PoolProperties();
            p.setUrl(container.getJdbcUrl());
            p.setUsername(container.getUsername());
            p.setPassword(container.getPassword());

            connectionPool = new DataSource();
            connectionPool.setPoolProperties(p);
            connectionPool.setTestOnBorrow(true);
            connectionPool.setDriverClassName(Driver.class.getCanonicalName());
        }
    }


    /**
     * Nothing to do: Testcontainers stops the container with the JVM.
     */
    public static synchronized void stop()
    {
    }


    /**
     * Pool connected to the test database, valid after {@link #start}.
     *
     * @return pool connected to the test database
     */
    public static DataSource getPool()
    {
        return connectionPool;
    }
}
