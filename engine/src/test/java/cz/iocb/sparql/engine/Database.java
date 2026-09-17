package cz.iocb.sparql.engine;

import java.nio.file.Paths;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.postgresql.Driver;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;



public class Database
{
    private static DataSource connectionPool;


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


    public static synchronized void stop()
    {
    }


    public static DataSource getPool()
    {
        return connectionPool;
    }
}
