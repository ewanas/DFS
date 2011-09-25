package rmi;

import java.net.*;
import java.util.logging.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Represents an RMI conversation. */
class SkeletonConnection <T> implements Runnable
{
    Socket                  clientSocket;
    T                       implementation;
    SkeletonServer<T>       server;
    ObjectOutputStream      result;

    Method                  method;
    Object[]                args;

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

    /** This attempts to read the method and its arguments from the client.

        <p>
        @throws IOException If the streams couldn't be referenced, or the
                            invocation couldn't be read.
        @throws ClassNotFoundException When the <code>Object</code> read from
                                       the client isn't recognized.
     */
    private void unpackInvocation () throws IOException, ClassNotFoundException
    {
        Object                  invocation;
        RMI.SerializedMethod    methodName;
        ObjectInputStream       stubCall;

        // Both streams have to be opened, for the readObject() to work
        // readObject () blocks on the absence of of an OutputStream
        // TODO check the validity of this
        stubCall = new ObjectInputStream (clientSocket.getInputStream ());
        result = new ObjectOutputStream (clientSocket.getOutputStream ());

        invocation = stubCall.readObject ();

        methodName = (RMI.SerializedMethod)((Object [])invocation)[0];
        method = RMI.SerializedMethod.findMethod (
                implementation.getClass (),
                methodName
                );
        args = ((Object [])((Object [])invocation)[1]);
        RMI.logger.publish (new LogRecord (
                    Level.INFO,
                    "Executing method with argument : " + args
                    ));

        RMI.logger.publish (new LogRecord (
                    Level.INFO,
                    "Unpacked a method with serialized form: " +
                    methodName.toString ()
                    ));
    }

    /** Invokes the method and sends the result back to the client.

        <p>
        @throws IOException When the streams couldn't be referenced, or the
                            result couldn't be sent back to the client.

        @throws NoSuchMethodException When the method being invoked isn't a
                                      member of the Skeleton's implementation.

        @throws IllegalAccessException When the method being invoked has a
                                       restriction that prevents its invocation
                                       from the <code>SkeletonConnection</code>.

        @throws InvocationTargetException When the method being invoked throws
                                          an exception.
     */
    private void invoke () throws IOException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException
    {
        if (method == null) {
            throw new NoSuchMethodException ("Method not found");
        }

        RMI.logger.publish (new LogRecord (
                    Level.INFO,
                    "Executing " + new RMI.SerializedMethod (method) + " on " +
                    implementation.toString () +
                    " with arguments " + Arrays.asList (args)
                    ));

        method.setAccessible (true);

        Object invocationResult;

        try {
            invocationResult = method.invoke (implementation, args);
        } catch (InvocationTargetException e) {
            invocationResult = e;
            e.printStackTrace ();
            RMI.logger.publish (new LogRecord (
                        Level.SEVERE,
                        "Exception in call:\n" + e.getMessage ()
                        ));
        }

        result.writeObject (invocationResult);
        RMI.logger.publish (new LogRecord (
                    Level.INFO,
                    "Done and returned " + invocationResult
                    ));
    }

    @Override
    public void run ()
    {
        try {
            unpackInvocation ();
            invoke ();
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
