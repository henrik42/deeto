package deeto;

public interface IFactory {
    <T> T newInstance(Class<T> c);
    <T extends java.io.Serializable> T copyOf(T o);
}
