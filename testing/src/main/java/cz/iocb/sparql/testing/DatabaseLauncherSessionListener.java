package cz.iocb.sparql.testing;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;



/**
 * Starts the test database when the JUnit launcher session opens, so that all test classes of the run share one
 * container. Registered in {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener} of this
 * module.
 */
public class DatabaseLauncherSessionListener implements LauncherSessionListener
{
    /**
     * Creates the listener; the JUnit platform instantiates it through the service loader.
     */
    public DatabaseLauncherSessionListener()
    {
    }


    @Override
    public void launcherSessionOpened(LauncherSession session)
    {
        Database.start();
    }


    @Override
    public void launcherSessionClosed(LauncherSession session)
    {
        Database.stop();
    }
}
