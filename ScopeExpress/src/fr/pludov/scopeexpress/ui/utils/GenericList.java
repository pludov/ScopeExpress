package fr.pludov.scopeexpress.ui.utils;

import java.awt.Component;
import java.awt.Dimension;
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
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;


public class GenericList<Target, EffectiveListEntry extends ListEntry<Target, ?>> 
			extends JTable 
{

	protected abstract class ColumnDefinition {
		String title;
		Class clazz;
		DefaultTableCellRenderer renderer;
		boolean editable;
		Integer width;
		
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
		
		protected ColumnDefinition(String title, Class clazz, Integer width) {
			this.title = title;
			this.clazz = clazz;
			this.width = width;
			this.renderer = null;
		}
		
		protected ColumnDefinition(String title, Class clazz, Integer width, DefaultTableCellRenderer renderer) {
			this.title = title;
			this.clazz = clazz;
			this.width = width;
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

		public ColumnDefinition setEditable(boolean editable) {
			this.editable = editable;
			return this;
		}
	}
	
	protected ColumnDefinition [] columns;
	private final List<EffectiveListEntry> entries;
	private final Map<ListKey<Target, EffectiveListEntry>, EffectiveListEntry> listEntries;
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
	

	public void reorderSelection(int how)
	{
		
		
		if (how < 0) {
			int moveToIndex = 0;
			// On d�place vers 0
			for(int i = 0; i < entries.size(); ++i)
			{
				if (isModelRowSelected(i)) {
					// On la d�place vers moveToIndex;
					if (moveToIndex != i) {
						EffectiveListEntry eli = entries.remove(i);
						entries.add(moveToIndex, eli);
					}
					moveToIndex++;
				}
			}
			
			boolean sthChanged = false;
			for(int i = 0; i < entries.size(); ++i)
			{
				EffectiveListEntry eli = entries.get(i);
				if (eli.getRowId() != i) {
					sthChanged = true;
					eli.rowId = i;
				}
			}

			if (!sthChanged) return;
			
			tableModel.fireTableDataChanged();
			
			// Selectionner de 0 � moveToIndex--;
			getSelectionModel().setSelectionInterval(0, moveToIndex - 1);
		} else {
			int moveToIndex = entries.size() - 1;
			for(int i = entries.size() - 1; i >= 0; --i)
			{
				if (isModelRowSelected(i)) {
					// On d�place vers moveToIndex
					if (moveToIndex != i) {
						EffectiveListEntry eli = entries.remove(i);
						
						entries.add(moveToIndex, eli);
					}
					moveToIndex--;
				}
			}
			
			boolean sthChanged = false;
			for(int i = 0; i < entries.size(); ++i)
			{
				EffectiveListEntry eli = entries.get(i);
				if (eli.getRowId() != i) {
					sthChanged = true;
					eli.rowId = i;
				}
			}
			if (!sthChanged) return;
			
			tableModel.fireTableDataChanged();
			
			// Selectionner de 0 � moveToIndex--;
			getSelectionModel().setSelectionInterval(moveToIndex + 1,entries.size());
		}
	}
	
	
	protected GenericList()
	{
		super();
		entries = new ArrayList<EffectiveListEntry>();
		listEntries = new HashMap<ListKey<Target, EffectiveListEntry>, EffectiveListEntry>();
		new DefaultTableModel();
		
		tableModel = 
				new AbstractTableModel() {
			
					@Override
					public boolean isCellEditable(int rowIndex, int columnIndex) {
						return columns[columnIndex].isEditable();
					}
					
					@Override
					public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
						EffectiveListEntry ile = entries.get(rowIndex);
						try {
							columns[columnIndex].setValue(ile, aValue);
						} catch(Exception e) {
							getTableModel().fireTableRowsUpdated(rowIndex, rowIndex);
						}
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
						return entries.size();
					}
					
					@Override
					public Object getValueAt(int rowIndex, int columnIndex) {
						EffectiveListEntry ile = entries.get(rowIndex);
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

						// Si la cellule n'est pas s�lectionn�e, on la s�lectionne.
						if (!isRowSelected(row)) {
							getSelectionModel().setSelectionInterval(row, row);
						}

						showPopup(GenericList.this, e);
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
//						maybeShowPopup(e);
			}

		});
	}
	
	public boolean showPopup(Component widget, MouseEvent e) 
	{
		
		List<EffectiveListEntry> targets = new ArrayList<EffectiveListEntry>();
		for(EffectiveListEntry entry : entries)
		{
			if (isModelRowSelected(entry.getRowId()))
			{
				targets.add(entry);
			}
		}
		
		JPopupMenu contextMenu = createContextMenu(targets);

		if (contextMenu == null || contextMenu.getComponentCount() == 0) return false;
		contextMenu.setLightWeightPopupEnabled(false);
		
		cancelCellEditing();
		
		Point p = new Point(e.getX(), e.getY());
		
		// ... and show it
		contextMenu.show(widget, p.x, p.y);
		return true;
	}
	
	private void cancelCellEditing() {
		CellEditor ce = getCellEditor();
		if (ce != null) {
			ce.cancelCellEditing();
		}
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
		
		int totalWidth = 20;
		for(int i = 0 ; i < columnDefs.size(); ++i)
		{
			 
			if (this.columns[i].width != null) {
				int width = this.columns[i].width;
				totalWidth += width;
				getColumnModel().getColumn(i).setPreferredWidth(width);
			} else {
				totalWidth += 20;
			}
		}
		
		setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		
		
		setMinimumSize(new Dimension(totalWidth, 100));
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
		return Collections.unmodifiableList(entries);
	}
	
	public List<EffectiveListEntry> getSelectedEntryList() {
		List<EffectiveListEntry> result = new ArrayList<EffectiveListEntry>();
		for(EffectiveListEntry entry : entries) {
			if (isEntrySelected((ListEntry<Target, EffectiveListEntry>)entry)) {
				result.add(entry);
			}
		}
		return result;
	}
	
	
	protected boolean hasEntry(Target t)
	{
		return listEntries.containsKey(new ListKey<Target, EffectiveListEntry>(t));
	}

	protected boolean removeEntry(Target t)
	{
		ListKey<Target, EffectiveListEntry> key = new ListKey<Target, EffectiveListEntry>(t);
		
		EffectiveListEntry entry = listEntries.remove(key);
		if (entry == null) {
			return false;
		}

		// Supprime rowid...
		int rowId = entry.getRowId();
		
		entries.remove(rowId);
	
		for(int i = rowId; i < entries.size(); ++i)
		{
			entries.get(i).rowId = i;
		}
		entry.removedFromGenericList((GenericList)this);
		((AbstractTableModel)getModel()).fireTableRowsDeleted(rowId, rowId);
		
		return true;
	}
	
	protected void addEntry(EffectiveListEntry ile)
	{
		entries.add(ile);
		listEntries.put((ListKey<Target, EffectiveListEntry>)ile, ile);
		ile.rowId = entries.size() - 1;
		
		ile.addedToGenericList((GenericList)this);
		tableModel.fireTableRowsInserted(ile.rowId, ile.rowId);
	}
	
	public List<EffectiveListEntry> getContent()
	{
		return entries;
	}
	
	public void setContent(List<EffectiveListEntry> newEntries)
	{
		while(!entries.isEmpty())
		{
			removeEntry(entries.get(0).getTarget());
		}
		
		for(EffectiveListEntry entry : newEntries)
		{
			addEntry(entry);
		}
	}
	
	public EffectiveListEntry getEntryFor(Target target)
	{
		ListKey<Target, EffectiveListEntry> key = new ListKey<Target, EffectiveListEntry>(target);
		
		EffectiveListEntry entry = listEntries.get(key);
		
		return entry;
	}
	
	public boolean isEntrySelected(ListKey<Target, EffectiveListEntry> key)
	{
		if (key == null) return false;
		
		EffectiveListEntry e = listEntries.get(key);
		if (e == null) return false;
		
		int rowId = e.getRowId();
		
		return isModelRowSelected(rowId);
	}

	public boolean selectEntry(ListEntry<Target, EffectiveListEntry> entry)
	{
		if (entry != null) {
			int rowId = entry.getRowId();
			if (getRowSorter() != null) {
				rowId = getRowSorter().convertRowIndexToView(rowId);
				if (rowId == -1) return false;
			}
			
			getSelectionModel().setSelectionInterval(rowId, rowId);
			return true;
		} else {
			getSelectionModel().clearSelection();
			return true;
		}
	}
	
	public AbstractTableModel getTableModel() {
		return tableModel;
	}
	
}
