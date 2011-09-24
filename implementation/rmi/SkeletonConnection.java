package rmi;

import java.net.*;
import java.util.logging.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Represents an RMI conversation. */
class SkeletonConnection <T> implements Runnable
{
    Socket              clientSocket;
    Class<T>            implementation;
    SkeletonServer<T>   server;

    public SkeletonConnection (
            Socket connection,
            Class<T> implementation,
            SkeletonServer<T> server
            )
    {
        clientSocket = connection;
        this.implementation = implementation;
        this.server = server;

        RMI.logger.publish (new LogRecord (
                    Level.INFO,
                    "Accepting a new connection on" + connection
                    ));
    }

    @Override
    public void run ()
    {
        ObjectInputStream   stubCall;
        ObjectOutputStream  result;

        Method              method;
        Object[]            args;

        try {
            stubCall = new ObjectInputStream (clientSocket.getInputStream ());
            result = new ObjectOutputStream (clientSocket.getOutputStream ());

            Object invocation = stubCall.readObject ();

            method  = ((Method)((Object [])invocation)[0]);
            args    = ((Object [])((Object [])invocation)[1]);

            RMI.logger.publish (new LogRecord (
                        Level.INFO,
                        "Executing " + method.getName () +
                        " with arguments " + Arrays.asList (args)
                        ));

            result.writeObject (method.invoke (implementation, args));
            clientSocket.close ();
        } catch (Exception e) {
            server.listen_error (e);
        }

        // TODO
        // Should unmarshal arguments and execute the respective function on T
        // Then forward the result back to the client
    }
}
