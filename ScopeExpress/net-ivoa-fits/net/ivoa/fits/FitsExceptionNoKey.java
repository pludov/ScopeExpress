/*
 * Created on Dec 1, 2005
 *

 */
package net.ivoa.fits;

/**
 * Execption thrown when key cannot be found.
 * 
 * @author Petr Kubanek <petr.kubanek@obs.unige.ch>
 */
public class FitsExceptionNoKey extends FitsException {
    private String key;

    /**
     * @param key
     *            Name of key which caused exception.
     */
    public FitsExceptionNoKey(String key) {
        this.key = key;
    }
    
    /**
     * Get key which caused exception to be raised.
     * 
     * @return Key name which was missing.
     */
    public String getKey() {
        return key;
    }
}