package rmi;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.lang.reflect.*;
import java.io.*;
import java.util.logging.*;

import java.util.*;

/** RMI stub factory.

    <p>
    RMI stubs hide network communication with the remote server and provide a
    simple object-like interface to their users. This class provides methods for
    creating stub objects dynamically, when given pre-defined interfaces.

    <p>
    The network address of the remote server is set when a stub is created, and
    may not be modified afterwards. Two stubs are equal if they implement the
    same interface and carry the same remote server address - and would
    therefore connect to the same skeleton. Stubs are serializable.
 */
public abstract class Stub
{
    /** Creates a stub, given a skeleton with an assigned adress.

        <p>
        The stub is assigned the address of the skeleton. The skeleton must
        either have been created with a fixed address, or else it must have
        already been started.

        <p>
        This method should be used when the stub is created together with the
        skeleton. The stub may then be transmitted over the network to enable
        communication with the skeleton.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param skeleton The skeleton whose network address is to be used.
        @return The stub created.
        @throws IllegalStateException If the skeleton has not been assigned an
                                      address by the user and has not yet been
                                      started.
        @throws UnknownHostException When the skeleton address is a wildcard and
                                     a port is assigned, but no address can be
                                     found for the local host.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton)
        throws UnknownHostException
    {
        InetSocketAddress       skeletonAddress;
        StubHandler             handler;

        if (c == null || skeleton == null) {
            throw new NullPointerException ("Failed to create stub");
        } else if (skeleton.address == null || skeleton.skeletonServer == null) {
            throw new IllegalStateException (
                    "Server not started yet"
                    );
        } else if (!RMI.isRemoteInterface (c)) {
            throw new Error (
                    c.getName () + " doesn't describe a remote interface"
                    );
        }

        // Create the proxy here.
        skeletonAddress = skeleton.address;
        handler = new StubHandler <T> (skeletonAddress, c);

        T stub = (T)Proxy.newProxyInstance (
                c.getClassLoader (),
                new Class [] {c},
                handler
                );

        return stub;
    }

    /** Creates a stub, given a skeleton with an assigned address and a hostname
        which overrides the skeleton's hostname.

        <p>
        The stub is assigned the port of the skeleton and the given hostname.
        The skeleton must either have been started with a fixed port, or else
        it must have been started to receive a system-assigned port, for this
        method to succeed.

        <p>
        This method should be used when the stub is created together with the
        skeleton, but firewalls or private networks prevent the system from
        automatically assigning a valid externally-routable address to the
        skeleton. In this case, the creator of the stub has the option of
        obtaining an externally-routable address by other means, and specifying
        this hostname to this method.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param skeleton The skeleton whose port is to be used.
        @param hostname The hostname with which the stub will be created.
        @return The stub created.
        @throws IllegalStateException If the skeleton has not been assigned a
                                      port.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton,
                               String hostname)
    {
        InetSocketAddress       skeletonAddress;
        StubHandler             handler;

        if (c == null || skeleton == null || hostname == null) {
            throw new NullPointerException ("Failed to create stub");
        } if (skeleton.address == null || skeleton.skeletonServer == null) {
            throw new IllegalStateException (
                    "Server not started yet"
                    );
        } else if (!RMI.isRemoteInterface (c)) {
            throw new Error (
                    c.getName () + " doesn't describe a remote interface"
                    );
        }

        // Create the proxy here.
        try {
            skeletonAddress = new InetSocketAddress (
                    InetAddress.getByName (hostname),
                    skeleton.address.getPort ()
                    );
        } catch (UnknownHostException e) {
            throw new Error (e); //TODO Check if that's the proper way
        }

        handler = new StubHandler (skeletonAddress, c);

        T stub = (T)Proxy.newProxyInstance (
                c.getClassLoader (),
                new Class [] {c},
                handler
                );

        return stub;
    }

    /** Creates a stub, given the address of a remote server.

        <p>
        This method should be used primarily when bootstrapping RMI. In this
        case, the server is already running on a remote host but there is
        not necessarily a direct way to obtain an associated stub.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param address The network address of the remote skeleton.
        @return The stub created.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, InetSocketAddress address)
    {
        InetSocketAddress       skeletonAddress;
        StubHandler             handler;

        if (c == null || address == null) {
            throw new NullPointerException ("Failed to create stub");
        } else if (!RMI.isRemoteInterface (c)) {
            throw new Error (
                    c.getName () + " doesn't describe a remote interface"
                    );
        }

        // Create the proxy here.
        skeletonAddress = address;
        handler = new StubHandler (skeletonAddress, c);

        T stub = (T)Proxy.newProxyInstance (
                c.getClassLoader (),
                new Class [] {c},
                handler
                );

        return stub;
    }

    /** The proxy for all methods invoked on a stub.

        <p>
        This is responsible for connecting to the <code>Skeleton</code> and
        forwarding all method invocations on the stub to it.
     */
    private static class StubHandler<T> implements InvocationHandler
    {
        InetSocketAddress   address;
        Class <T>           remoteInterface;

        /** Configures the proxy to forward method invocations to a specific
            address.

            @param skeletonAddress Is the address to marshal methods and the
                                   arguments to.
          */
        public StubHandler (InetSocketAddress skeletonAddress, Class <T> c)
        {
            address = skeletonAddress;
            this.remoteInterface = remoteInterface;
        }

        /** Marshals the arguments and the methods name off to the
            <code>Skeleton</code>.

            <p>
            @throws RMIException If a connection with the <code>Skeleton</code>
                                 could not be established or if it was thrown
                                 by the method.
            @return The result of the RMI.
            @param stub Is the instance implementing an interface that this
                        <code>StubHandler</code> is a proxy for.
            @param call Is the method being called on the stub.
            @param args Are the arguments to the method being called.
         */
        public Object invoke (Object stub, Method call, Object [] args)
            throws RMIException, Exception
        {
            Socket      connection = new Socket ();
            Object []   toInvoke =
                new Object [] {new RMI.SerializedMethod (call), args};

            Object      result = null;

            ObjectOutputStream  toServer;
            ObjectInputStream   fromServer;

            RMI.logger.info ("Calling method " + call.getName ());

            if (call.getName ().equals ("equals") && args.length == 1) {
                if (args [0] == null) {
                    result = false;
                } else {
                    result = Proxy.getInvocationHandler (stub).equals (
                            Proxy.getInvocationHandler (args[0])
                            );
                }
            } else if (call.getName ().equals ("hashCode") && args == null) {
                result = Proxy.getInvocationHandler (stub).hashCode ();
            } else if (call.getName ().equals ("toString") && args == null) {
                result = Proxy.getInvocationHandler (stub).toString ();
            } else {
                try {
                    connection.connect (address);

                    toServer = new ObjectOutputStream (
                            connection.getOutputStream ()
                            );
                    fromServer = new ObjectInputStream (
                            connection.getInputStream ()
                            );

                    toServer.writeObject (toInvoke);

                    RMI.logger.info ("Sent invocation to Skeleton");

                    result = fromServer.readObject ();

                    connection.close ();
                    toServer.close ();
                    fromServer.close ();
                } catch (IOException e) {
                    RMI.logger.severe (
                            "IOException on connection with " + address +
                            " : " + e.getMessage ()
                            );

                    throw new RMIException (e.getMessage ());
                } catch (ClassNotFoundException e) {
                    RMI.logger.severe (
                            "ClassNotFoundException: " + e.getMessage ()
                            );

                    throw new RMIException (e.getMessage ());
                } 
                if (result instanceof InvocationTargetException) {
                    throw (Exception)(((InvocationTargetException)result).getCause ());
                }
            }

            return result;
        }

        /** Two <code>StubHandler</code>'s are equal if they refer to the same
            <code>Skeleton</code> address and they implement the same remote
            interface.

            <p>
            @param other Is the other <code>StubHandler</code> to test for
                         equality with.
            @return false If <code>other</code> is <code>null</code>
                          or if <code>other</code> is not an instance of
                          <code>StubHandler</code> or if they refer to
                          different <code>Skeleton</code> addresses.
         */
        public boolean equals (Object other)
        {
            if (other != null && other instanceof StubHandler) {
                Set <Method> myMethods = new HashSet <Method> ();
                myMethods.addAll (
                        Arrays.asList (this.getClass ().getMethods ())
                        );

                for (Method m : other.getClass ().getMethods ()) {
                    if (!myMethods.contains (m)) {
                        return false;
                    }
                }

                return ((StubHandler)other).address.equals (address);
            }

            return false;
        }

        /** A hash for <code>StubHandler</code>s.

            @return A hash that has the same distribution as
                    <code>InetSocketAddress</code> hashCode
         */
        public int hashCode ()
        {
            return address.hashCode ();
        }

        /** Returns a string representation for the <code>StubHandler</code>.
         */
        public String toString ()
        {
            return address.toString ();
        }
    }
}
