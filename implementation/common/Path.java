package common;

import java.io.*;
import java.util.*;

/** Distributed filesystem paths.

    <p>
    Objects of type <code>Path</code> are used by all filesystem interfaces.
    Path objects are immutable.

    <p>
    The string representation of paths is a forward-slash-delimeted sequence of
    path components. The root directory is represented as a single forward
    slash.

    <p>
    The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
    not permitted within path components. The forward slash is the delimeter,
    and the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Serializable
{
    final String    SEPARATOR   =   "/";
    final String    ROOT        =   "/";

    String      pathComponent;
    Path        pathPrefix;

    String      debug = "";

    private boolean isValid (String p)
    {
        if (p.indexOf (":") == -1) {
            if (p.indexOf (SEPARATOR) == -1) {
                if (!p.equals ("")) {
                    return true;
                }
            }
        }

        return false;
        //return p.indexOf (":") == -1 && p.indexOf (SEPARATOR) == -1 && !p.equals ("");
    }

    /** Creates a new path which represents the root directory. */
    public Path()
    {
        pathComponent = "";

        debug = "This was created in a blank way";
    }

    /** Creates a new path by appending the given component to an existing path.

        @param path The existing path.
        @param component The new component.
        @throws IllegalArgumentException If <code>component</code> includes the
                                         separator, a colon, or
                                         <code>component</code> is the empty
                                         string.
    */
    public Path(Path path, String component)
    {
        this ();

        debug = "This was created with another path and: [" + component + "]";

        if (component == null || !isValid (component)) {
            throw new IllegalArgumentException (
                    "Path contains an illegal character"
                    );
        }

        pathPrefix      = path;
        pathComponent   = component;
    }

    /** Creates a new path from a path string.

        <p>
        The string is a sequence of components delimited with forward slashes.
        Empty components are dropped. The string must begin with a forward
        slash.

        @param path The path string.
        @throws IllegalArgumentException If the path string does not begin with
                                         a forward slash, or if the path
                                         contains a colon character.
     */
    public Path(String path)
    {
        this ();

        debug = "This was created using a string: [" + path + "]";

        String [] components;

        if (path != null && path.indexOf (SEPARATOR) == 0) {
            path = path.substring (1);

            components = path.split (SEPARATOR);

            if (components.length >= 1) {
                pathComponent = components [components.length - 1];

                components = Arrays.copyOfRange (components, 0, components.length - 1);

                System.out.println (Arrays.asList (components));

                for (String component : components) {
                    if (!component.equals ("")) {
                        pathPrefix = new Path (pathPrefix, component);
                    }
                }
            } else if (path.equals (ROOT)) {
                return;
            }
        } else {
            throw new IllegalArgumentException (
                    "Invalid path string"
                    );
        }
    }

    /** Returns an iterator over the components of the path.

        <p>
        The iterator cannot be used to modify the path object - the
        <code>remove</code> method is not supported.

        @return The iterator.
     */
    @Override
    public Iterator<String> iterator()
    {
        List <String> components = new ArrayList <String> ();

        components.add (this.toString ());

        for (Path p = pathPrefix; p != null; p = p.pathPrefix) {
            components.add (0, p.pathComponent.toString ());
        }

        return components.iterator ();
    }

    /** Lists the paths of all files in a directory tree on the local
        filesystem.

        @param directory The root directory of the directory tree.
        @return An array of relative paths, one for each file in the directory
                tree.
        @throws FileNotFoundException If the root directory does not exist.
        @throws IllegalArgumentException If <code>directory</code> exists but
                                         does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException
    {
        List <Path> contents = new ArrayList <Path> ();

        if (!directory.exists ()) {
            throw new FileNotFoundException (
                    "File " + directory.getName () + " doesn't exist"
                    );
        } else if (!directory.isDirectory ()) {
            throw new IllegalArgumentException (
                    "Not a directory"
                    );
        } else {
            for (String file : directory.list ()) {
                contents.add (new Path (directory.toString () + file));
            }
        }

        return contents.toArray (new Path [0]);
    }

    /** Determines whether the path represents the root directory.

        @return <code>true</code> if the path does represent the root directory,
                and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
        return pathPrefix == null && pathComponent == "";
    }

    /** Returns the path to the parent of this path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
     */
    public Path parent()
    {
        if (isRoot ()) {
            throw new IllegalArgumentException (
                    "Root directory doesn't have a parent"
                    );
        }

        return pathPrefix;
    }

    /** Returns the last component in the path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no last
                                         component.
     */
    public String last()
    {
        if (isRoot ()) {
            throw new IllegalArgumentException (
                    "Root directory has no other components"
                    );
        }

        return pathComponent;
    }

    /** Determines if the given path is a subpath of this path.

        <p>
        The other path is a subpath of this path if is a prefix of this path.
        Note that by this definition, each path is a subpath of itself.

        @param other The path to be tested.
        @return <code>true</code> If and only if the other path is a subpath of
                this path.
     */
    public boolean isSubpath(Path other)
    {
        if (this.toString ().indexOf (other.toString ()) != -1) {
            return true;
        }

        return false;
    }

    /** Converts the path to <code>File</code> object.

        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Compares two paths for equality.

        <p>
        Two paths are equal if they share all the same components.

        @param other The other path.
        @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other)
    {
        return toString ().equals (other.toString ());
        // throw new UnsupportedOperationException("not implemented");
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Converts the path to a string.

        <p>
        The string may later be used as an argument to the
        <code>Path(String)</code> constructor.

        @return The string representation of the path.
     */
    @Override
    public String toString()
    {
        List <String> components = new ArrayList <String> ();

        components.add (pathComponent);

        for (Path p = pathPrefix; p != null; p = p.pathPrefix) {
            components.add (0, p.pathComponent);
        }

        String res = "/";

        for (int i = 0; i < components.size (); i++) {
            res += components.get (i);
        }

        synchronized ("A") {
            System.out.println ("The path component is: " + pathComponent);
            System.out.println ("My guess is: " + res);
            System.out.println ("Debug says: " + debug);
        }

        return res;
    }
}
