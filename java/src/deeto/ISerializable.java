package deeto;

/**
 * Clojures proxy macro need to know which methods to generate with
 * which signature. We need these so the Javas serialization mechanism
 * calls writeResolve on the Clojure proxy.
 */
public interface ISerializable extends java.io.Serializable {
    public Object writeReplace() throws java.io.ObjectStreamException;
    public Object readResolve() throws java.io.ObjectStreamException;
}
