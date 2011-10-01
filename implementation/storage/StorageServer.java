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
public class StorageServer implements Storage, Command
{
    File        root;

    Skeleton <Storage>  storageInvoker;
    Skeleton <Command>  commandInvoker;

    Logger      logger = Logger.getAnonymousLogger ();

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

        storageInvoker = new Skeleton <Storage> (Storage.class, this);
        commandInvoker = new Skeleton <Command> (Command.class, this);

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

        storageInvoker.start ();
        commandInvoker.start ();

        try {
            toDelete = naming_server.register (
                    Stub.create (Storage.class, storageInvoker),
                    Stub.create (Command.class, commandInvoker),
                    Path.list(root)
                    );

            for (Path file : toDelete) {
                if (!delete (file)) {
                    throw new FileNotFoundException (
                            "Can't find the file " + file
                            );
                }
            }

            prune (root);

            System.out.println (
                    "Now I have " +
                    Arrays.asList (Path.list (root))
                    );
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
        storageInvoker.stop ();
        commandInvoker.stop ();

        stopped (null);
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
            if (!purge (path.toFile (root))) {
                throw new RuntimeException (
                        "Couldn't purge the file " + path + "successfully"
                        );
            }
        } catch (SecurityException e) {
            System.out.println (e.getMessage ());
            return false;
        }

        return true;
    }

    /** Prunes the directory.

        <p>
        This removes all directories that have only empty directories or empty
        directories.

        <p>
        @param dir Is the directory that should be pruned.
      */
    private static void prune (File dir)
    {
        if (dir != null && dir.isDirectory ()) {
            for (File f : dir.listFiles ()) {
                if (hasNoFiles (f)) {
                    prune (f);
                    f.delete ();
                }
            }
        }
    }

    /** Purges a directory and all its contents.

        <p>
        @param toPurge Is the directory or file to purge.
        @throws SecurityException When we're denied the operation of deleting
                                  the file.
     */
    private static boolean purge (File toPurge) throws SecurityException
    {
        boolean pass = false;

        if (toPurge != null) {
            if (toPurge.isDirectory ()) {
                pass = true;
                for (File f : toPurge.listFiles ()) {
                    pass = pass && purge (f);
                    pass = pass && f.delete ();
                }
            } else {
                pass = toPurge.delete ();
            }
        }

        return pass;
    }

    /** Checks whether a directory has no files in it.

        <p>
        @param dir Is the directory to check.
        @return true If the directory contains only directories and no files.
     */
    private static boolean hasNoFiles (File dir)
    {
        if (dir != null && dir.isDirectory ()) {
            File [] subdirs = dir.listFiles ();

            for (File f : subdirs) {
                if (!f.isDirectory ()) {
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;
    }
}
