package fr.pludov.scopeexpress.ui;

import java.io.Serializable;

import fr.pludov.scopeexpress.correlation.ViewPort;
import fr.pludov.scopeexpress.correlation.ViewPortListener;
import fr.pludov.scopeexpress.ui.utils.GenericList;
import fr.pludov.scopeexpress.ui.utils.ListEntry;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

public class ViewPortListEntry 
			extends ListEntry<ViewPort, ViewPortListEntry> implements Serializable
			
{
	private static final long serialVersionUID = 4237214584857675847L;
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

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
		getTarget().listeners.addListener(this.listenerOwner, new ViewPortListener() {
			@Override
			public void viewPortMoved(ViewPort vp) {
				list.getTableModel().fireTableRowsUpdated(getRowId(), getRowId());
			}
		});
	}
	
	@Override
	protected void removedFromGenericList(GenericList<ViewPort, ViewPortListEntry> list) {
		getTarget().listeners.removeListener(this.listenerOwner);
	}
}