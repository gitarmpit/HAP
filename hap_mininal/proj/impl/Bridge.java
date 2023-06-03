package impl;


public interface Bridge extends HomekitAccessory {

    @Override
    default void identify() {}
    
}
