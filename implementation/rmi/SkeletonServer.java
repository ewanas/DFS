package rmi;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.logging.*;

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

        RMI.logger.publish (new LogRecord (
                    Level.INFO,
                    "Skeleton Server initialized"
                    ));
    }

    /** Runs the server and listens for new connections.  */
    @Override
    public void run ()
    {
        RMI.logger.publish (new LogRecord (
                    Level.INFO,
                    "Skeleton listening on " + server
                    ));
        try {
            while (server != null && !stopped) {
                clientPool.execute (
                        new SkeletonConnection <T> (server.accept ())
                        );
            }

            RMI.logger.publish (new LogRecord (
                        Level.INFO,
                        "Server stopped gracefully"
                        ));
        } catch (IOException e) {
            RMI.logger.publish (new LogRecord (
                        Level.INFO,
                        "Server stopped abnormally " + e.getMessage ()
                        ));

            parent.listen_error (e);
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
