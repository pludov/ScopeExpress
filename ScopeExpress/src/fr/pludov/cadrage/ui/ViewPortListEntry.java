package fr.pludov.cadrage.ui;

import java.io.Serializable;

import fr.pludov.cadrage.correlation.ViewPort;
import fr.pludov.cadrage.correlation.ViewPortListener;
import fr.pludov.cadrage.ui.utils.GenericList;
import fr.pludov.cadrage.ui.utils.ListEntry;

public class ViewPortListEntry 
			extends ListEntry<ViewPort, ViewPortListEntry> implements Serializable
			
{
	private static final long serialVersionUID = 4237214584857675847L;

	/**
	 * 
	 */
	boolean visible;
	
	
	ViewPortListEntry(ViewPort image) {
		super(image);
		visible = true;
		
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		if (this.visible == visible) return;
		this.visible = visible;
		
		getTarget().listeners.getTarget().viewPortMoved(getTarget());
	}
	
	@Override
	protected void addedToGenericList(final GenericList<ViewPort, ViewPortListEntry> list) {
		getTarget().listeners.addListener(this, new ViewPortListener() {
			@Override
			public void viewPortMoved(ViewPort vp) {
				list.getTableModel().fireTableRowsUpdated(getRowId(), getRowId());
			}
		});
	}
	
	@Override
	protected void removedFromGenericList(GenericList<ViewPort, ViewPortListEntry> list) {
		getTarget().listeners.removeListener(this);
	}
}