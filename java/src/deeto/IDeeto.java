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
public interface IDeeto extends java.io.Serializable, Cloneable {
    <T> T clone();
}
