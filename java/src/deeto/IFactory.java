package deeto;

/**
 * A generic factory for DTO instances. 
 * 
 * copyOf should be a generic static method but with Clojure we cannot
 * define generic type information. So I (lazily) put the method in
 * this interface.
 */
public interface IFactory {
    <T> T newInstance(Class<T> c);
    <T extends java.io.Serializable> T copyOf(T o);
}
