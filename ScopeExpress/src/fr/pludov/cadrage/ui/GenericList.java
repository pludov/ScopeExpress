package fr.pludov.cadrage.ui;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;


public class GenericList<Target, EffectiveListEntry extends GenericList<Target, ?>.ListEntry> extends JTable {

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
		Integer rowId;
		
		
		ListEntry(Target image) {
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
		
		ColumnDefinition(String title, Class clazz) {
			this.title = title;
			this.clazz = clazz;
			this.renderer = null;
		}
		
		ColumnDefinition(String title, Class clazz, DefaultTableCellRenderer renderer) {
			this.title = title;
			this.clazz = clazz;
			this.renderer = renderer;
		}
		
		abstract Object getValue(EffectiveListEntry ile);
		
		void setValue(EffectiveListEntry ile, Object value)
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
	public TableCellRenderer getCellRenderer(int row, int column) {
		if (columns[column].renderer != null) {
			return columns[column].renderer;
		}
		return super.getCellRenderer(row, column);
	}

	public List<EffectiveListEntry> getEntryList() {
		return Collections.unmodifiableList(images);
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
		
		EffectiveListEntry entry = listEntries.remove(key);
		
		return entry;
	}

	public AbstractTableModel getTableModel() {
		return tableModel;
	}
	
}
