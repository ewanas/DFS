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
    T                   implementation;
    SkeletonServer<T>   server;

    public SkeletonConnection (
            Socket connection,
            T implementation,
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
        ObjectInputStream       stubCall;
        ObjectOutputStream      result;

        Method                  method = null;
        Object[]                args;

        RMI.SerializedMethod    methodName = null;

        try {
            // Setup the object streams which, by default, flushes the streams
            stubCall = new ObjectInputStream (clientSocket.getInputStream ());
            result = new ObjectOutputStream (clientSocket.getOutputStream ());

            Object invocation = stubCall.readObject ();

            methodName = (RMI.SerializedMethod)((Object [])invocation)[0];
            RMI.logger.publish (new LogRecord (
                        Level.INFO,
                        "Trying to execute " + methodName
                        ));

            method = RMI.SerializedMethod.findMethod (
                    implementation.getClass (),
                    methodName
                    );

            if (method == null) {
                RMI.logger.publish (new LogRecord (
                            Level.SEVERE,
                            "Failed to find " + methodName
                            ));
                throw new NoSuchMethodException (methodName.toString ());
            }

            args = ((Object [])((Object [])invocation)[1]);

            RMI.logger.publish (new LogRecord (
                        Level.INFO,
                        "Executing " + method.getName () +
                        " with arguments " + Arrays.asList (args)
                        ));

            result.writeObject (method.invoke (
                        implementation,
                        args
                        ));

            clientSocket.close ();
        } catch (Exception e) {
            RMI.logger.publish (new LogRecord (
                        Level.SEVERE,
                        "Failed to execute method, on the remote side: " +
                        e.getMessage ()
                        ));
            e.printStackTrace ();
            server.listen_error (e);
        }
    }
}
