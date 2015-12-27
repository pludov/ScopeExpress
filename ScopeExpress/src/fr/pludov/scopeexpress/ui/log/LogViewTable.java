package fr.pludov.scopeexpress.ui.log;

import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import fr.pludov.scopeexpress.ui.log.LogViewModel.ColumnDef;

public class LogViewTable extends JTable {
	final LogViewModel lvm;
	
	public LogViewTable() {
		super(new LogViewModel());
		lvm = getModel();
		
		getTableHeader().setReorderingAllowed(false);
		for(int i = 0; i < LogViewModel.columns.length; ++i)
		{
			ColumnDef def = LogViewModel.columns[i];
			TableColumn tc = getTableHeader().getColumnModel().getColumn(i);
			tc.setHeaderValue(def.name);
			tc.setResizable(false);
			Integer width = def.getWidth(this);
			
			if (width != null) {
				tc.setMinWidth(width);
				tc.setMaxWidth(width);
			}
			
			tc.setCellRenderer(def.getCellRenderer());
		}
		lvm.addTableModelListener(new TableModelListener() {
			
			@Override
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.INSERT && getSelectedRowCount() == 0) {
					programScrollDown();
				}
			}
		});
	}
	
	@Override
	public LogViewModel getModel() {
		return (LogViewModel)super.getModel();
	}
	
	public void setLogger(UILogger logger) {
		lvm.setLogger(logger);
	}


	boolean pendingScrollDown;
	
	/** Make the table scroll to the last line */
	void programScrollDown()
	{
		if (pendingScrollDown) return;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				pendingScrollDown = false;
				
				if (getRowCount() == 0) {
					return;
				}
				Rectangle rect = 
						   getCellRect(getRowCount() - 1, 0, true);
				scrollRectToVisible(rect);
			}
			
		});
		pendingScrollDown = true;
	}
}
