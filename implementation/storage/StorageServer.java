package storage;

import java.io.*;
import java.net.*;
import java.util.*;

import java.nio.*;
import java.nio.channels.*;

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
    File    root;

    Skeleton <Storage>  storageInvoker;
    Skeleton <Command>  commandInvoker;

    static Logger   logger = Logger.getAnonymousLogger ();
    static Level    loggingLevel = Level.OFF;

    boolean     canStart = true;

    /** Creates a storage server, given a directory on the local filesystem.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root) throws NullPointerException
    {
        logger.setLevel (loggingLevel);

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

        if (!canStart) {
            logger.severe ("Can't restart the storage server");
            return;
        }

        storageInvoker.start ();
        commandInvoker.start ();

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

        canStart = false;

        logger.info ("Started storage server and registered it");
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
            logger.severe ("Storage server stopped abnormally " + cause);
        } else {
            logger.info ("Stopping the storage server");
        }

        canStart = false;
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        File toQuery = file.toFile (root);

        if (toQuery.exists () && toQuery.isFile ()) {
            return toQuery.length ();
        } else {
            throw new FileNotFoundException ("Can't get size for " + file);
        }
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        FileInputStream fileData;
        byte []         data;
        File            toRead = file.toFile (root);

        if (toRead.isFile () && offset >= 0 &&
                offset + length <= size (file) && length >= 0)
        {
            fileData = new FileInputStream (toRead);

            fileData.skip (offset);

            data = new byte [length];
            fileData.read (data);

            fileData.close ();
        } else if (toRead.isFile ()) {
            throw new IndexOutOfBoundsException (
                    "Can't read at a negative offset or a negative amount"
                    );
        } else {
            throw new FileNotFoundException (
                    toRead + " refers to a directory"
                    );
        }

        return data;
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        FileChannel fileData;
        File        destination = file.toFile (root);
        long        written;

        if (destination.isFile () && offset >= 0) {
            fileData = new FileOutputStream (destination).getChannel ();

            fileData.position (offset);

            written = fileData.write (ByteBuffer.wrap (data));

            if (data.length != written) {
                throw new IOException (
                        "Failed to write to the file correctly, wrote " +
                        written + " instead of " + data.length
                        );
            }
        } else if (destination.isFile ()) {
            throw new IndexOutOfBoundsException (
                    "Can't write at a negative offset"
                    );
        } else {
            throw new FileNotFoundException (
                    destination + " refers to a directory"
                    );
        }
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        File    thisFile;
        File    parent;

        logger.info ("Creating " + file);

        if (file == null) {
            logger.severe ("Can't create null directory");

            throw new NullPointerException ("Can't create file");
        } else if (file.equals (new Path ())) {
            // TODO isRoot () should work here
            logger.severe ("Can't create root");
            return false;
        } else {
            try {
                thisFile = file.toFile (root);
                parent = file.toFile (root).getParentFile ();

                if (!parent.exists ()) {
                    return parent.mkdirs () && thisFile.createNewFile ();
                } else {
                    return thisFile.createNewFile ();
                }
            } catch (IOException e) {
                logger.severe (
                        "Can't create " + file + " " +  e.getMessage ()
                        );
                return false;
            }
        }
    }

    @Override
    public synchronized boolean delete(Path path)
    {
        List <File> toDelete;

        if (path == null) {
            throw new NullPointerException ("Null path can't be deleted");
        }

        logger.info ("Deleting " + path);

        try {
            if (path.equals (new Path ())) {
                throw new SecurityException ("Can't delete root");
            } else if (!purge (path.toFile (root))) {
                throw new IllegalArgumentException (
                        "Couldn't purge the file " + path + " successfully"
                        );
            }
        } catch (Exception e) {
            logger.severe (e.getMessage ());
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
        if (toPurge != null) {
            if (toPurge.exists () && toPurge.isDirectory ()) {
                for (File f : toPurge.listFiles ()) {
                    if (!(purge (f))) {
                        return false;
                    }
                }
            }

            return toPurge.delete ();
        }

        return false;
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

    /** Test for equality of two <code>StorageServer</code>s.

        <p>
        Two storage servers are equal if they run on the same host and have the 
        same root directory.

        @param other Is the other StorageServer to test for equality with.
        @return true If both StorageServer instances run on the same host and 
                     have the same root location.
     */
    @Override
    public boolean equals (Object other)
    {
        StorageServer s;

        if (other instanceof StorageServer) {
            s = (StorageServer)other;

            return  s.storageInvoker.equals (storageInvoker) &&
                    s.commandInvoker.equals (commandInvoker);
        }

        return false;
    }

    /** Hash for a StorageServer. */
    @Override
    public int hashCode ()
    {
        return root.hashCode ();
    }
}
