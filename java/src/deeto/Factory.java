package deeto;

public interface Factory {
    <T> T newInstance(Class<T> c);
}
