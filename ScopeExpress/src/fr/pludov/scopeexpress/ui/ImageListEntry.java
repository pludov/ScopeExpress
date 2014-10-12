package fr.pludov.scopeexpress.ui;

import java.io.Serializable;
import java.util.Date;

import fr.pludov.scopeexpress.Image;
import fr.pludov.scopeexpress.ImageListener;
import fr.pludov.scopeexpress.correlation.ImageCorrelation;
import fr.pludov.scopeexpress.correlation.ImageCorrelationListener;
import fr.pludov.scopeexpress.ui.utils.GenericList;
import fr.pludov.scopeexpress.ui.utils.ListEntry;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

public class ImageListEntry 
		extends ListEntry<Image, ImageListEntry> implements Serializable 
{

	private static final long serialVersionUID = -4986397965526535539L;
	
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	ImageListEntry()
	{
		super(null);
	}
	
	ImageListEntry(Image image) {
		super(image);
	}
	
	boolean hasDate = false;
	Date date;

	transient ImageCorrelation imageCorrelation;
	
	public Date getCreationDate()
	{
		if (!hasDate) {
			hasDate = true;
			long modif = getTarget().getFile().lastModified();
			if (modif != 0L) {
				date = new Date(modif);
			} else {
				date = null;
			}
		}
		return date;
	}


	@Override
	protected void addedToGenericList(final GenericList<Image, ImageListEntry> imageList) 
	{
		getTarget().listeners.addListener(this.listenerOwner, new ImageListener() {
			@Override
			public void metadataChanged(Image source) {
				imageList.getTableModel().fireTableRowsUpdated(getRowId(), getRowId());
			}
			
			@Override
			public void starsChanged(Image source) {
				imageList.getTableModel().fireTableRowsUpdated(getRowId(), getRowId());
			}
			
			@Override
			public void scopePositionChanged(Image source) {
				imageList.getTableModel().fireTableRowsUpdated(getRowId(), getRowId());
			}
			
			@Override
			public void levelChanged(Image source) {
				imageList.getTableModel().fireTableRowsUpdated(getRowId(), getRowId());
			}			
		});
		imageCorrelation = ((ImageList)imageList).getCorrelation().getImageCorrelation(getTarget());
		imageCorrelation.listeners.addListener(this.listenerOwner, new ImageCorrelationListener() {
			@Override
			public void lockingChanged(ImageCorrelation correlation) {
				imageList.getTableModel().fireTableRowsUpdated(getRowId(), getRowId());
			}
			
			@Override
			public void imageTransformationChanged(ImageCorrelation correlation) {
				imageList.getTableModel().fireTableRowsUpdated(getRowId(), getRowId());				
			}
		});
	}
	
	@Override
	protected void removedFromGenericList(GenericList<Image, ImageListEntry> list) {
		getTarget().listeners.removeListener(this.listenerOwner);
		imageCorrelation.listeners.removeListener(this.listenerOwner);
	}
}