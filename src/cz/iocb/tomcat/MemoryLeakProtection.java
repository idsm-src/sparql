package cz.iocb.tomcat;

import javax.imageio.spi.IIORegistry;



public class MemoryLeakProtection
{
    static
    {
        System.setProperty("java.awt.headless", "true");
        IIORegistry.getDefaultInstance();
    }
}
