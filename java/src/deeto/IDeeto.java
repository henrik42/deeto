package deeto;

public interface IDeeto<T> extends java.io.Serializable, Cloneable {
    T clone();
}
