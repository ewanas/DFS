package storage;

import java.io.*;
import java.net.*;
import java.util.*;

import java.util.logging.*;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command, Serializable
{
    File        root;

    transient Skeleton <Storage>    invoker;
    transient Logger                logger = Logger.getAnonymousLogger ();

    /** Creates a storage server, given a directory on the local filesystem.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root) throws NullPointerException
    {
        if (root == null) {
            throw new NullPointerException ("Null root directory provided");
        }

        invoker = new Skeleton <Storage> (Storage.class, this);

        logger.info ("Creating a new storage server");

        this.root = root;
    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
        String  error;
        Path [] toDelete;

        logger.info ("Staring the storage server");
        invoker.start ();

        // Registration process
        try {
            toDelete = naming_server.register (
                    this, this,
                    Path.list(root)
                    );

            for (Path file : toDelete) {
                if (!delete (file)) {
                    throw new FileNotFoundException (
                            "Can't find the file " + file
                            );
                }
            }
        } catch (Exception e) {
            error = "Failed to register the storage server " + e.getMessage ();
            logger.severe (error);
            throw e;
        }
    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        invoker.stop ();
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code>
                     if the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
        if (cause != null) {
            // TODO log it
            // Throw it, maybe.
        }
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized boolean delete(Path path)
    {
        List <File> toDelete;

        try {
            if (path.toFile (root).isDirectory ()) {
                toDelete = new LinkedList <File> ();

                for (Path p : path.list (path.toFile (root))) {
                    toDelete.add (p.toFile (root));
                }

                Collections.sort (toDelete, new ParentLast <File> ());
            } else {
                toDelete = new ArrayList <File> ();
                toDelete.add (path.toFile (root));
            }

            for (File f : toDelete) {
                f.delete ();
            }
        } catch (SecurityException e) {
            System.out.println (e.getMessage ());
        } catch (FileNotFoundException e) {
            System.out.println (e.getMessage ());
        }

        return true; // TODO REMOVE THIS
    }

    /** Sorts a list of <code>File</code>s where subfiles go before parents. */
    public static class ParentLast <T extends File> implements Comparator <T>
    {
        /** Compares two files using a parent last approach.

            <p>
            If both files are directories or if they both aren't directories, 
            normal lexiographic ordering is applied.

            If either file is not a directory, the file gets ranked higher.
          */
        public int compare (T first, T second)
        {
            if (first.isDirectory () && !second.isDirectory ()) {
                return -3;
            } else if (!first.isDirectory () && second.isDirectory ()) {
                return 3;
            } else {
                return first.compareTo (second);
            }
        }
    }
}
