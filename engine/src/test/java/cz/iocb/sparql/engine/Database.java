package cz.iocb.sparql.engine;

import java.nio.file.Paths;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.postgresql.Driver;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;



/**
 * Test database shared by all engine tests: a PostgreSQL 16 container built from {@code src/test/resources/docker},
 * which clones and compiles the pgsparql extension from the public {@code next} branch on GitHub and loads the NeXtProt
 * test schemas. Local, unpushed changes of pgsparql are therefore not visible to the tests. The container is started
 * once per JUnit launcher session by {@link DatabaseLauncherSessionListener}.
 */
public class Database
{
    /**
     * Pool connected to the running container; null until {@link #start} has been called.
     */
    private static DataSource connectionPool;


    /**
     * Builds the image (without cache, so the latest pgsparql is fetched) and starts the container on first call; later
     * calls are no-ops.
     */
    public static synchronized void start()
    {
        if(connectionPool == null)
        {
            String dockerPath = "src/test/resources/docker";
            String imageName = new ImageFromDockerfile("sparql-test", false)
                    .withFileFromPath(".", Paths.get(dockerPath))
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
     */
    public static DataSource getPool()
    {
        return connectionPool;
    }
}
