package cz.iocb.sparql.engine;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;



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
