package fr.pludov.scopeexpress.ui;

import java.io.Serializable;
import java.util.Date;

import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.ui.utils.ListEntry;

public class MosaicImageListEntry extends ListEntry<MosaicImageParameter, MosaicImageListEntry> implements Serializable{

	public MosaicImageListEntry(MosaicImageParameter image) {
		super(image);
	}
	
	transient boolean hasDate = false;
	transient Date date;
	
	public Date getCreationDate()
	{
		if (!hasDate) {
			hasDate = true;
			long modif = getTarget().getImage().getPath().lastModified();
			if (modif != 0L) {
				date = new Date(modif);
			} else {
				date = null;
			}
		}
		return date;
	}

}
