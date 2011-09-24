/** Remote method invocation (RMI) library.

    <p>
    This package contains two major classes, <code>Skeleton</code> and
    <code>Stub</code>, which can be used to implement <em>remote method
    invocation</em> (RMI). In RMI, a client possesses a <em>stub object</em>.
    The stub object purports to implement a certain functionality. In fact, the
    functionality is implemented remotely by a server. The stub object merely
    marshals the arguments given to its methods and transmits them over a
    network to the server. It then waits for the server to provide the result,
    which is returned to the client. RMI hides the network communication from
    the client. Network requests and responses appear to the client as regular
    method calls on the stub object.

    <p>
    The <em>skeleton</em> is an object on the server that is responsible for
    maintaining network connections and unmarshaling arguments. It is the
    server's counterpart to the client's stub.

    <p>
    The <code>Skeleton</code> class includes a multithreaded server which
    communicates with stubs over TCP connections. The <code>Stub</code> class
    provides methods for creating stubs. Each stub object is given the network
    address of the skeleton with which it is to communicate when it is created.

    <p>
    To use the library, first define a <em>remote interface</em>: an interface
    in which all public methods are marked as throwing
    <code>RMIException</code>. Skeletons can be created using the
    <code>Skeleton</code> constructors, and stubs with one of the
    <code>create</code> methods in <code>Stub</code>. For example, with the
    following definitions:

    <pre>
    public interface TestInterface
    {
        public void testMethod() throws RMIException;
    }

    public class TestClass implements TestInterface
    {
        ...
    }
    </pre>

    An RMI skeleton and stub can be created as follows:

    <pre>
    TestClass               object = new TestClass();

    Skeleton&lt;TestInterface&gt; skeleton =
                new Skeleton&lt;TestInterface&gt;(TestInterface.class, object);
    skeleton.start();

    TestInterface           stub = Stub.create(TestInterface.class, skeleton);
    </pre>

    <p>
    The methods declared in the remote interface may throw their own exceptions.
    In case an exception is thrown remotely, it is transmitted back to the
    client.

    <p>
    In the typical case, the server will create both the skeleton and the
    corresponding stub, as above. The stub will then be transmitted to any
    clients that wish to use the services provided by the server. Stubs may also
    be created directly by the client - this is provided primarily to bootstrap
    RMI. If all stubs were created by the server, then it would be necessary to
    use a means other than the RMI library to transmit an initial stub to the
    client. To avoid this, the RMI library allows the client to create an
    initial stub by directly providing a network address to a version of
    <code>create</code>.
 */
package rmi;

import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;
import java.io.*;

class RMI
{
    // The internal logger for all RMI events
    static ConsoleHandler   logger = new ConsoleHandler ();

    /** Checks if the given interface implements a remote interface.
        
        <p>
        @param spec is the <code>Class</code> to inspect.
        @return true if all methods of the interface <code>spec</code> throw an
            RMIException.
     */
    public static boolean isRemoteInterface (Class spec)
    {
        if (!spec.isInterface ()) {
            return false;
        } else {
            for (Method m : spec.getMethods ()) {
                if (!Arrays.asList (m.getExceptionTypes ()).contains (
                            RMIException.class)
                   )
                {
                    return false;
                }
            }
        }

        return true;
    }

    /** The Serialized form of a <code>Method</code>. */
    public static class SerializedMethod implements Serializable
    {
        List <String>   exceptions = new ArrayList <String> ();
        List <String>   parameters = new ArrayList <String> ();
        String          name = "";
        String          returnType = "";

        /** Creates a new serialized form of a method.

            <p>
            @param m Is the method to be described by this serialized form.
         */
        public SerializedMethod (Method m)
        {
            name = m.getName ();
            returnType = m.getReturnType ().getName ();

            for (Class <?> parameter : m.getParameterTypes ()) {
                parameters.add (parameter.getName ());
            }

            for (Class <?> exception : m.getExceptionTypes ()) {
                exceptions.add (exception.getName ());
            }
        }

        /** A hash for a serialized method.

            <p>
            Two hashes will be equal if the names and return types of two
            methods have the same hashCode().

            @return The hash.
         */
        public int hashCode ()
        {
            return name.hashCode () ^ returnType.hashCode ();
        }

        /** Tests if two serialized forms of a method are equal.

            <p>
            @param other Is a <code>SerializedMethod</code>
            @return true If the <code>String</code> representation of the
                         return type's name, the name of the method, the
                         parameter type names and the exception type names
                         are identical.
         */
        public boolean equals (Object other)
        {
            SerializedMethod m;

            if (other instanceof SerializedMethod) {
                m = (SerializedMethod)other;

                return  m.exceptions.equals (exceptions) &&
                        m.parameters.equals (parameters) &&
                        m.name.equals (name) &&
                        m.returnType.equals (returnType);
            }

            return false;
        }

        /** A <code>String</code> representation of a serialized method.

            <p>
            @return A <code>String</code> starting with the return type's name
                    forllowed by the name of the method, the parameter and
                    exception names enclosed in square brackets.
         */
        public String toString ()
        {
            StringBuffer spec = new StringBuffer ("");

            spec.append (returnType + " ");
            spec.append (name + " ");
            spec.append (parameters + " ");
            spec.append (exceptions);

            return spec.toString ();
        }

        /** Searches for a method conforming to the description of the given
            serialized method in the given class.

            <p>
            @param c Is the class to search within the methods of.
            @param method Is the serialized method.
            @return null If no method conforms to the description of
                         <code>method</code>.
         */
        public static Method findMethod (Class <?> c, SerializedMethod method)
        {
            for (Method m : c.getMethods ()) {
                if (new SerializedMethod (m).equals (method)) {
                    return m;
                }
            }

            return null;
        }
    }
}
