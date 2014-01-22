/*
 * Created on Dec 1, 2005
 *
 */
package net.ivoa.fits;

/**
 * 
 * @author Petr Kubanek <petr.kubanek@obs.unige.ch>
 */
public class FitsExceptionBadValue extends FitsException { 
    private String key;

	public FitsExceptionBadValue (String key) {
		this.key = key;
	}
}
