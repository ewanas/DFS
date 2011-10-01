package rmi;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.logging.*;

/** Represents the top level server for implementations of <code>T</code>.
 */
class SkeletonServer<T> implements Runnable
{
    private ServerSocket        server;
    private Skeleton <T>        parent;
    private ExecutorService     clientPool;
    volatile private boolean    stopped = false;
    private T                   implementation;
    static Logger               logger = Logger.getAnonymousLogger ();
    static Level                loggingLevel = Level.OFF;

    /** Creates a new <code>SkeletonServer</code> that is bound to the
        specified <code>InetSocketAddress</code>.

        <p>
        @throws IOException When it fails to create the socket or bind it.
        @param parent Is the <code>Skeleton</code> for which this server
                      accepts method invocations.
        @param c Is the implementation of <code>T</code> that the RMIs will
                 act on.
     */
    public SkeletonServer (Skeleton <T> parent, T c) throws IOException
    {
        logger.setLevel (loggingLevel);

        this.parent = parent;
        server = new ServerSocket ();
        server.bind (parent.address);
        parent.address = new InetSocketAddress (
                server.getInetAddress (),
                server.getLocalPort ()
                );

        clientPool = Executors.newCachedThreadPool ();

        implementation = c;

        logger.info ("Skeleton Server initialized");
    }

    /** Runs the server and listens for new connections.  */
    @Override
    public void run ()
    {
        logger.info ("Skeleton listening on " + server);

        try {
            while (server != null && !stopped) {
                clientPool.execute (
                        new SkeletonConnection <T> (
                            server.accept (),
                            implementation,
                            this
                            ));
            }

            logger.info ("Server stopped gracefully");
        } catch (IOException e) {
            if (!stopped) {
                logger.severe (
                        "Server stopped abnormally " + e.getMessage ()
                        );

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

    /** Called by connections to let the server report any errors. */
    public void listen_error (Exception e)
    {
        parent.listen_error (e);
    }
}
