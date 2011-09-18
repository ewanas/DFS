package rmi;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

/** Represents the top level server for implementations of <code>T</code>.
 */
class SkeletonServer<T> implements Runnable
{
    private ServerSocket    server;
    private Skeleton <T>    parent;
    private ExecutorService clientPool;
    private boolean         stopped = false;

    /** Creates a new <code>SkeletonServer</code> that is bound to the
        specified <code>InetSocketAddress</code>.

        @throws IOException when it fails to create the socket or bind it.
     */
    public SkeletonServer (Skeleton <T> parent) throws IOException
    {
        this.parent = parent;

        server = new ServerSocket ();

        server.bind (parent.address);

        clientPool = Executors.newCachedThreadPool ();
    }

    /** Runs the server and listens for new connections.  */
    @Override
    public void run ()
    {
        try {
            while (server != null) {
                clientPool.execute (
                        new SkeletonConnection <T> (server.accept ())
                        );
            }
        } catch (IOException e) {
            if (!stopped) {
                parent.listen_error (e);
            }
        }
    }

    /** Stops the server. */
    public void stop ()
    {
        stopped = true;
        try {
            server.close ();
        } catch (IOException e) {
            stopped = false;
        }
    }
}
