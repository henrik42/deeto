package deeto;

public interface DoubleDto extends IDeeto {
    Double getDoubleProp();
    void setDoubleProp(Double x);
    DoubleDto doubleProp(Double x);
    
    double getNativeDoubleProp();
    void setNativeDoubleProp(double x);
    DoubleDto nativeDoubleProp(double x);
}
