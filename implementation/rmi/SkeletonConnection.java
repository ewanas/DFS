package rmi;

import java.net.*;

class SkeletonConnection <T> implements Runnable
{
    Socket clientSocket;

    public SkeletonConnection (Socket connection)
    {
        clientSocket = connection;
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
