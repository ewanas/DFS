package naming;

import java.io.*;
import java.net.*;
import java.util.logging.*;
import java.util.*;
import java.util.concurrent.*;

import rmi.*;
import common.*;
import storage.*;

/** Naming server.

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration
{
    Skeleton <Service>      serviceInvoker;
    Skeleton <Registration> registrationInvoker;

    static Logger           logger = Logger.getAnonymousLogger ();
    static Level            loggingLevel = Level.ALL;

    boolean     canStart = true;

    Map <StorageCommandPair, List <Path>>     files;

    /** Represents an immutable pair of Storage and Command implementations. */
    private class StorageCommandPair
    {
        private Storage storage;
        private Command command;

        public StorageCommandPair (Storage storage, Command command)
        {
            this.storage = storage;
            this.command = command;
        }

        public Storage getStorage ()
        {
            return storage;
        }

        public Command getCommand ()
        {
            return command;
        }

        @Override
        public boolean equals (Object other)
        {
            StorageCommandPair  pair;

            if (other instanceof StorageCommandPair) {
                pair = (StorageCommandPair)other;

                return  pair.storage.equals (storage) &&
                        pair.command.equals (command);
            }

            return false;
        }

        @Override
        public int hashCode ()
        {
            return storage.hashCode () + command.hashCode ();
        }
    }

    /** Creates the naming server object.

      <p>
      The naming server is not started.
     */
    public NamingServer()
    {
        InetSocketAddress   regAddress = null;
        InetSocketAddress   serviceAddress = null;

        regAddress = new InetSocketAddress (
                "127.0.0.1",
                NamingStubs.REGISTRATION_PORT
                );
        serviceAddress = new InetSocketAddress (
                "127.0.0.1",
                NamingStubs.SERVICE_PORT
                );

        registrationInvoker = new Skeleton <Registration> (
                Registration.class,
                this,
                regAddress
                );

        serviceInvoker = new Skeleton<Service> (
                Service.class,
                this,
                serviceAddress
                );

        files = new ConcurrentHashMap <StorageCommandPair, List <Path>> ();

        logger.info ("Created a new naming server");
        logger.info ("Registration requests on " + regAddress);
        logger.info ("Service requests on " + serviceAddress);
    }

    /** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        if (canStart) {
            try {
                registrationInvoker.start ();
                serviceInvoker.start ();
            } catch (RMIException e) {
                logger.info ("Failed to start invokers " + e.getMessage ());
                stopped (e);
                throw e;
            }

            canStart = false;

            logger.info ("Started the Naming server");
        } else {
            logger.severe ("Can't restart the naming server");
        }
    }

    /** Stops the naming server.

        <p>
        This method waits for both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        logger.info ("Stopping the naming server");

        registrationInvoker.stop ();
        serviceInvoker.stop ();

        stopped (null);
    }

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
        if (cause != null) {
            logger.severe ("Naming server stopped abnormally " + cause);
        }

        canStart = false;
    }

    // The following methods are documented in Service.java.
    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        logger.info ("Received directory check on " + path);

        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
        logger.info ("Received list request for directory " + directory);

        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        logger.info ("Received request for creation of file " + file);

        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException
    {
        logger.info ("Received request for creation of directory " + directory);

        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException
    {
        logger.info ("Received request for deletion of " + path);

        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {
        logger.info ("Received request for storage server for file " + file);

        throw new UnsupportedOperationException("not implemented");
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {
        List <Path> toDelete = new ArrayList <Path> ();
        Set <Path>  currentFiles = new HashSet <Path> ();

        Set <StorageCommandPair>    servers = this.files.keySet ();
        StorageCommandPair          pair =
            new StorageCommandPair (client_stub, command_stub);

        for (List <Path> filesOnServer : this.files.values ()) {
            for (Path f : filesOnServer) {
                currentFiles.add (f);
            }
        }

        logger.info ("Registering a new storage server");

        if (client_stub == null || command_stub == null || files == null) {
            throw new NullPointerException (
                    "Can't register with null arguments"
                    );
        }

        if (servers.contains (pair)) {
            throw new IllegalStateException (
                    "Storage server already regsitered"
                    );
        } else {
            this.files.put (pair, new LinkedList <Path> ());
        }

        for (Path remoteFile : files) {
            if (currentFiles.contains (remoteFile)) {
                toDelete.add (remoteFile);
            } else {
                this.files.get (pair).add (remoteFile);
            }
        }

        return toDelete.toArray (new Path [0]);
    }
}
