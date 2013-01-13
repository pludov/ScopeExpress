package fr.pludov.cadrage.ui.focus;

import java.io.Serializable;
import java.util.Date;

import fr.pludov.cadrage.correlation.ImageCorrelation;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.ui.utils.ListEntry;

public class MosaicImageListEntry extends ListEntry<Image, MosaicImageListEntry> implements Serializable{

	public MosaicImageListEntry(Image image) {
		super(image);
	}
	
	transient boolean hasDate = false;
	transient Date date;
	
	public Date getCreationDate()
	{
		if (!hasDate) {
			hasDate = true;
			long modif = getTarget().getPath().lastModified();
			if (modif != 0L) {
				date = new Date(modif);
			} else {
				date = null;
			}
		}
		return date;
	}

}
