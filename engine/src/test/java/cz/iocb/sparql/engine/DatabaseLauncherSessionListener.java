package cz.iocb.sparql.engine;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;



/**
 * Starts the test database when the JUnit launcher session opens, so that all test classes of the run share one
 * container. Registered in {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener}.
 */
public class DatabaseLauncherSessionListener implements LauncherSessionListener
{
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
