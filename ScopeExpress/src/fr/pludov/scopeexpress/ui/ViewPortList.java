package fr.pludov.scopeexpress.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import fr.pludov.scopeexpress.Image;
import fr.pludov.scopeexpress.ImageListener;
import fr.pludov.scopeexpress.ImageStar;
import fr.pludov.scopeexpress.correlation.Correlation;
import fr.pludov.scopeexpress.correlation.CorrelationListener;
import fr.pludov.scopeexpress.correlation.ImageCorrelation;
import fr.pludov.scopeexpress.correlation.ViewPort;
import fr.pludov.scopeexpress.ui.utils.GenericList;
import fr.pludov.scopeexpress.utils.IdentityBijection;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;


public class ViewPortList extends GenericList<ViewPort, ViewPortListEntry> implements CorrelationListener {
	protected final CorrelationUi correlationUi;
	Correlation target;
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
		
	
	@SuppressWarnings("unchecked")
	private final List<ColumnDefinition> columns = Arrays.asList(
			new ColumnDefinition("Titre", String.class) {
				@Override
				public Object getValue(ViewPortListEntry ile) {
					return ile.getTarget().getViewPortName();
				}
				
				public void setValue(ViewPortListEntry ile, Object value) {
					ile.getTarget().setViewPortName((String)value);
				}
				
				{
					setEditable(true);
				}
			},
			
			new ColumnDefinition("Voir", Boolean.class, 20) {
				@Override
				public Object getValue(ViewPortListEntry ile) {
					return ile.isVisible();
				}
				
				@Override
				public void setValue(ViewPortListEntry ile, Object value) {
					ile.setVisible((Boolean)value);
				}
				
				{
					setEditable(true);
				}
			});
	
	Correlation correlation;
	
	public ViewPortList(CorrelationUi correlationUi) {
		super();
		
		setColumnDefinitions(columns);
		
		this.correlationUi = correlationUi;
		this.correlation = correlationUi.getCorrelation();
		
		this.correlation.listeners.addListener(this.listenerOwner, this);
	}
	
	public void changeCorrelation(Correlation correlation)
	{
		this.correlation.listeners.removeListener(this.listenerOwner);
		this.correlation = correlation;
		this.correlation.listeners.addListener(this.listenerOwner, this);
	}
	
	@Override
	protected JPopupMenu createContextMenu(List<ViewPortListEntry> entries) {
		return correlationUi.getDynamicMenuForViewPortList(entries);
	}
	
	@Override
	public void viewPortAdded(ViewPort viewport) {
		if (hasEntry(viewport)) {
			return;
		}
		
		ViewPortListEntry ile = new ViewPortListEntry(viewport); 
		if (viewport.getViewPortName().indexOf("backup") >= 0) {
			ile.setVisible(false);
		}
		addEntry(ile);
	}
	
	@Override
	public void viewPortRemoved(ViewPort image) {
		removeEntry(image);	
	}

	@Override
	public void correlationUpdated() {
	}

	@Override
	public void imageAdded(Image viewPort) {
	}

	@Override
	public void imageRemoved(Image viewPort, ImageCorrelation imageCorrelation) {
	}

	@Override
	public void scopeViewPortChanged() {
		// FIXME : il faut updater le telescope
	}
}
