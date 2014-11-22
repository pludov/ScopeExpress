package fr.pludov.scopeexpress.ui.settings;

import fr.pludov.scopeexpress.ImageDisplayParameterListener;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

public class LevelDisplayPosition {
	public transient WeakListenerCollection<LevelDisplayPositionListener> listeners;

	/** Taille (1 - 65536) représentée */
	int zoom;
	/** Offset offset + zoom <= 65536 */
	int offset;
	
	
	public LevelDisplayPosition() {
		this.listeners = new WeakListenerCollection<LevelDisplayPositionListener>(LevelDisplayPositionListener.class);
		zoom = 65536;
	}
	
	void setPosition(int zoom, int offset)
	{
		if (this.zoom == zoom && this.offset == offset) return;
		this.zoom = zoom;
		this.offset = offset;
		this.listeners.getTarget().positionChanged();
	}
	
	public void setOffset(int offset)
	{
		if (offset < 0) offset = 0;
		if (offset + zoom > 65536) offset = 65536 - zoom;
		if (this.offset == offset) return;
		this.offset = offset;
		this.listeners.getTarget().positionChanged();
	}

	public int getZoom() {
		return zoom;
	}

	public void setZoom(int zoom) {
		this.zoom = zoom;
	}

	public int getOffset() {
		return offset;
	}

	public void zoomAt(int wheelRotation, double where) {

		int d = wheelRotation;
		
		int newZoom = getZoom();
		int newOffset = getOffset();
		// ADU invariant
		int center = (int) Math.round(newOffset + where * newZoom);
		for(int i = 0; i < Math.abs(d); ++i)
		{
			if (d > 0) {
				if (newZoom < 65536) {
					newZoom = newZoom * 5 /4;
					if (newZoom > 65536) {
						newZoom = 65536;
					}
				}
				
			} else {
				if (newZoom > 1024) {
					newZoom = newZoom * 4 / 5;
					if (newZoom < 1024) {
						newZoom = 1024;
					}
				}
			}
		}
		// On veut que newOffset + where * newZoom = center
		// newOffset = center - where * newZoom
		newOffset = (int)Math.round(center - where * newZoom);
		if (newOffset < 0) newOffset = 0;
		if (newZoom + newOffset > 65536) {
			newOffset = 65536 - newZoom;
		}
		setPosition(newZoom, newOffset);
	}
	
	
}
