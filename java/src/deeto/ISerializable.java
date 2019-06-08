package deeto;

public interface ISerializable extends java.io.Serializable {
    public Object writeReplace() throws java.io.ObjectStreamException;
    public Object readResolve() throws java.io.ObjectStreamException;
}
