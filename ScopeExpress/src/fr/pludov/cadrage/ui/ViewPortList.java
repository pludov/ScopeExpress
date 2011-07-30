package fr.pludov.cadrage.ui;

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

import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.ImageListener;
import fr.pludov.cadrage.ImageStar;
import fr.pludov.cadrage.correlation.Correlation;
import fr.pludov.cadrage.correlation.CorrelationListener;
import fr.pludov.cadrage.correlation.ViewPort;
import fr.pludov.cadrage.correlation.ViewPortListener;
import fr.pludov.cadrage.ui.utils.GenericList;
import fr.pludov.cadrage.ui.utils.GenericList.ListEntry;
import fr.pludov.cadrage.utils.IdentityBijection;


public class ViewPortList extends GenericList<ViewPort, ViewPortList.ViewPortListEntry> implements CorrelationListener {
	protected final CorrelationUi correlationUi;
	Correlation target;
	
	
	public class ViewPortListEntry 
				extends GenericList<ViewPort, ViewPortListEntry>.ListEntry
				implements ViewPortListener
	{
		boolean visible;
		
		
		ViewPortListEntry(ViewPort image) {
			super(image);
			visible = true;
			image.listeners.addListener(this);
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
		public void viewPortMoved(ViewPort vp) {
			getTableModel().fireTableRowsUpdated(getRowId(), getRowId());
		}
	}
	
	
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
	
	final Correlation correlation;
	
	public ViewPortList(CorrelationUi correlationUi) {
		super();
		
		setColumnDefinitions(columns);
		
		this.correlationUi = correlationUi;
		this.correlation = correlationUi.getCorrelation();
		
		this.correlation.listeners.addListener(this);
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
	public void imageRemoved(Image viewPort) {
	}

	@Override
	public void scopeViewPortChanged() {
		// FIXME : il faut updater le telescope
	}
}
