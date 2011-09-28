/** The common functionality needed by some RMI modules.
    
    <p>
    Logging fascilities and a method serialization class.
 */
package rmi;

import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;
import java.io.*;

/** Convenience methods for use by RMI modules. */
public abstract class RMI
{
    // The internal logger for all RMI events
    static Logger   logger = Logger.getAnonymousLogger ();

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
