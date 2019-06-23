package deeto;

/** 
 * Convinience interface which can be extended by users DTO
 * interfaces.
 * 
 * By extending IDeeto the DTO types implement Serializable and
 * Cloneable. Note though that this is not a prerequisit for
 * serializing and cloning the DTOs Deeto proxy since the Deeto proxys
 * (which are Java dynamic proxys) are always Serializable and are not
 * cloned via Object.clone() (so there is no need to implement
 * Cloneable). As a bonus IDeeto defines a generic, public clone()
 * method so that the DTOs can be "cloned" by calling the accessable
 * clone() method.
 */
public interface IDeeto<T> extends java.io.Serializable, Cloneable {
    
    /**
     * Returns a deep copy of this.
     */
    T clone();
    
    /**
     * Returns a map which maps each property name (capitalized
     * string; e.g. "FooBar") to its cloned/copied value (possibly
     * null). I.e. the map will contain an entry for each property.
     */
    java.util.Map<String, Object> toMap();

    /**
     * Iterates over source's entry set. For each entry the key is
     * used as the property name and that property will be set to the
     * entry's (cloned/copied) value. So this method is a
     * mutator. It's not a factory.
     * 
     * Returns the mutated this.
     */
    T fromMap(java.util.Map<String, Object> source);

}
