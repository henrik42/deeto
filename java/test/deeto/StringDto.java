package deeto;

public interface StringDto extends java.io.Serializable, java.lang.Cloneable {
    String getString();
    void setString(String x);
    StringDto string(String x);
}

