package fr.pludov.cadrage.ui.utils;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.CellEditor;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;


public class GenericList<Target, EffectiveListEntry extends GenericList<Target, ?>.ListEntry> 
			extends JTable 
{

	public class ListKey {
		final Target viewPort;

		ListKey(Target image) {
			this.viewPort = image;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof GenericList.ListKey) {
				return ((ListKey)obj).viewPort == this.viewPort;
			}
			return super.equals(obj);
		}
		
		@Override
		public int hashCode() {
			return System.identityHashCode(viewPort);
		}

		public Target getTarget() {
			return viewPort;
		}
	}
	
	public class ListEntry extends ListKey
	{
		private Integer rowId;
		
		
		protected ListEntry(Target image) {
			super(image);
		}

		public Integer getRowId() {
			return rowId;
		}
	}
	
	

	protected abstract class ColumnDefinition {
		String title;
		Class clazz;
		DefaultTableCellRenderer renderer;
		boolean editable;
		
		protected ColumnDefinition(String title, Class clazz) {
			this.title = title;
			this.clazz = clazz;
			this.renderer = null;
		}
		
		protected ColumnDefinition(String title, Class clazz, DefaultTableCellRenderer renderer) {
			this.title = title;
			this.clazz = clazz;
			this.renderer = renderer;
		}
		
		public abstract Object getValue(EffectiveListEntry ile);
		
		public void setValue(EffectiveListEntry ile, Object value)
		{
			throw new RuntimeException("unimplemented");
		}

		public boolean isEditable() {
			return editable;
		}

		public void setEditable(boolean editable) {
			this.editable = editable;
		}
	}
	
	protected ColumnDefinition [] columns;
	private final List<EffectiveListEntry> images;
	private final Map<ListKey, EffectiveListEntry> listEntries;
	private final AbstractTableModel tableModel;
	
	protected int rowToModelRow(int mrow)
	{
		if (getRowSorter() == null) return mrow;
		return getRowSorter().convertRowIndexToModel(mrow);
	}
	
	protected boolean isModelRowSelected(int mrowId)
	{
		int rowId = mrowId;
		if (getRowSorter() != null) {
			rowId = getRowSorter().convertRowIndexToView(mrowId);
			if (rowId == -1) return false;
		}
		return isRowSelected(rowId);
	}
	
	protected GenericList()
	{
		super();
		images = new ArrayList<EffectiveListEntry>();
		listEntries = new HashMap<ListKey, EffectiveListEntry>();
		
		tableModel = 
				new AbstractTableModel() {
			
					@Override
					public boolean isCellEditable(int rowIndex, int columnIndex) {
						return columns[columnIndex].isEditable();
					}
					
					@Override
					public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
						EffectiveListEntry ile = images.get(rowIndex);
						columns[columnIndex].setValue(ile, aValue);
					};
			
					@Override
					public String getColumnName(int column) {
						return columns[column].title;
					}
					
					@Override
					public int getColumnCount() {
						return columns.length;
					}
					
					@Override
					public int getRowCount() {
						return images.size();
					}
					
					@Override
					public Object getValueAt(int rowIndex, int columnIndex) {
						EffectiveListEntry ile = images.get(rowIndex);
						return columns[columnIndex].getValue(ile);
					}
					
					@Override
					public Class getColumnClass(int columnIndex) {
						return columns[columnIndex].clazz;
					}
				};
				
		addMouseListener(new MouseAdapter() {

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger() && isEnabled()) {
					Point p = new Point(e.getX(), e.getY());
					int col = columnAtPoint(p);
					int row = rowAtPoint(p);

					
					if (row >= 0 && row < getRowCount()) {
						// translate table index to model index
						int mcol = getColumn(
								getColumnName(col)).getModelIndex();
						
						// int mrow = getRowSorter().convertRowIndexToModel(row);
						
						// create popup menu...

						// Si la cellule n'est pas sélectionnée, on la sélectionne.
						if (!isRowSelected(row)) {
							getSelectionModel().setSelectionInterval(row, row);
						}

						List<EffectiveListEntry> targets = new ArrayList<EffectiveListEntry>();
						for(EffectiveListEntry entry : images)
						{
							if (isModelRowSelected(entry.getRowId()))
							{
								targets.add(entry);
							}
						}
						
						JPopupMenu contextMenu = createContextMenu(targets);
						
						if (contextMenu == null || contextMenu.getComponentCount() == 0) return;
						
						cancelCellEditing();
						
						
						// ... and show it
						contextMenu.show(GenericList.this, p.x, p.y);
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
//				maybeShowPopup(e);
			}

			private void cancelCellEditing() {
				CellEditor ce = getCellEditor();
				if (ce != null) {
					ce.cancelCellEditing();
				}
			}
		});
	}
	

	protected JPopupMenu createContextMenu(List<EffectiveListEntry> entries) {
		return null;
	}
	
	protected void setColumnDefinitions(List<ColumnDefinition> columnDefs)
	{
		this.columns = (ColumnDefinition[])Array.newInstance(ColumnDefinition.class, columnDefs.size());
		for(int i = 0; i < columnDefs.size(); ++i)
		{
			this.columns[i] = columnDefs.get(i);
		}
		
		setModel(tableModel);
	}
	

	@Override
	public TableCellRenderer getCellRenderer(int row, int viewcolumn) {
		// Traduire column...
		int column = convertColumnIndexToModel(viewcolumn);
		if (columns[column].renderer != null) {
			return columns[column].renderer;
		}
		return super.getCellRenderer(row, viewcolumn);
	}

	public List<EffectiveListEntry> getEntryList() {
		return Collections.unmodifiableList(images);
	}
	
	public List<EffectiveListEntry> getSelectedEntryList() {
		List<EffectiveListEntry> result = new ArrayList<EffectiveListEntry>();
		for(EffectiveListEntry entry : images) {
			if (isEntrySelected((ListEntry)entry)) {
				result.add(entry);
			}
		}
		return result;
	}
	
	
	protected boolean hasEntry(Target t)
	{
		return listEntries.containsKey(new ListKey(t));
	}

	protected boolean removeEntry(Target t)
	{
		ListKey key = new ListKey(t);
		
		EffectiveListEntry entry = listEntries.remove(key);
		if (entry == null) {
			return false;
		}

		// Supprime rowid...
		int rowId = entry.getRowId();
		
		images.remove(rowId);
	
		for(int i = rowId; i < images.size(); ++i)
		{
			images.get(i).rowId = i;
		}
		
		((AbstractTableModel)getModel()).fireTableRowsDeleted(rowId, rowId);
		
		return true;
	}
	
	protected void addEntry(EffectiveListEntry ile)
	{
		images.add(ile);
		listEntries.put((ListKey)ile, ile);
		ile.rowId = images.size() - 1;
		tableModel.fireTableRowsInserted(ile.rowId, ile.rowId);
	}
	
	public EffectiveListEntry getEntryFor(Target target)
	{
		ListKey key = new ListKey(target);
		
		EffectiveListEntry entry = listEntries.get(key);
		
		return entry;
	}
	
	public boolean isEntrySelected(ListKey key)
	{
		if (key == null) return false;
		
		EffectiveListEntry e = listEntries.get(key);
		if (e == null) return false;
		
		int rowId = e.getRowId();
		
		return isModelRowSelected(rowId);
	}

	public boolean selectEntry(ListEntry entry)
	{
		int rowId = entry.getRowId();
		if (getRowSorter() != null) {
			rowId = getRowSorter().convertRowIndexToView(rowId);
			if (rowId == -1) return false;
		}
		
		getSelectionModel().setSelectionInterval(rowId, rowId);
		return true;
	}
	
	public AbstractTableModel getTableModel() {
		return tableModel;
	}
	
}
