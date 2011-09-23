package rmi;

import java.net.*;
import java.util.logging.*;

class SkeletonConnection <T> implements Runnable
{
    Socket clientSocket;

    public SkeletonConnection (Socket connection)
    {
        clientSocket = connection;
        RMI.logger.publish (new LogRecord (
                    Level.INFO,
                    "Accepting a new connection on" + connection
                    ));
    }

    @Override
    public void run ()
    {
        System.out.println ("I'm a server connection, I do nothing.");
        // TODO
        // Should unmarshal arguments and execute the respective function on T
        // Then forward the result back to the client
    }
}
