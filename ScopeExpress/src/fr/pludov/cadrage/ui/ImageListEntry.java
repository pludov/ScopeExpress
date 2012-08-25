package fr.pludov.cadrage.ui;

import java.io.Serializable;
import java.util.Date;

import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.ImageListener;
import fr.pludov.cadrage.ui.utils.GenericList;
import fr.pludov.cadrage.ui.utils.ListEntry;

public class ImageListEntry 
		extends ListEntry<Image, ImageListEntry> implements Serializable 
{

	private static final long serialVersionUID = -4986397965526535539L;
	ImageListEntry()
	{
		super(null);
	}
	
	ImageListEntry(Image image) {
		super(image);
	}
	
	boolean hasDate = false;
	Date date;

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
		getTarget().listeners.addListener(this, new ImageListener() {
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
	}
	
	@Override
	protected void removedFromGenericList(GenericList<Image, ImageListEntry> list) {
		getTarget().listeners.removeListener(this);
	}
}