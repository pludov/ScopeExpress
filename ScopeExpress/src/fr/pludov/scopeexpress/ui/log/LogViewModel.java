package fr.pludov.scopeexpress.ui.log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.table.AbstractTableModel;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

public class LogViewModel extends AbstractTableModel {
	private final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	abstract static class ColumnDef { 
		final String name;
		
		ColumnDef(String name) {
			this.name = name;
		}
		
		abstract Object get(LogMessage lm);

		Integer getWidth(LogViewTable logViewTable) {
			return null;
		}
		
		Integer sizeForContent(LogViewTable table, Object[] strings) {
			int max = 20;
			for(int i = -1; i < strings.length; ++i) {
				String s = (i == -1 ? name : strings[i].toString());
	            
				double w = new JLabel(s).getPreferredSize().getWidth();
				w += 5;
				if (i == -1) {
					w += 10;
				}
				if (w > max) {
					max = (int)w;
				}
			}
			return max;
		}

	}
	
	static ColumnDef [] columns = new ColumnDef[] {
			new ColumnDef("Heure") {
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
				
				@Override
				Object get(LogMessage lm) {
					return sdf.format(lm.time);
				}
				@Override
				Integer getWidth(LogViewTable logViewTable) {
					return sizeForContent(logViewTable, new Object[] {
							"00:00:00.000",
							"99:99:99.999",
					});
				}
			},
			new ColumnDef("Niveau") {
				@Override
				Object get(LogMessage lm) {
					return lm.level;
				}
				Integer getWidth(LogViewTable logViewTable) {
					return sizeForContent(logViewTable, LogMessage.Level.values());
				};
				
			},
			new ColumnDef("Détails") {
				@Override
				Object get(LogMessage lm) {
					return lm.message;
				}
			},
			
	};
	
	UILogger logger;

	int lastLoggerCount;
	List<LogMessage> logOfCurrentLevel;
	LogMessage.Level minLevel;
	
	public LogViewModel() {
		logger = null;
		lastLoggerCount = 0;
		logOfCurrentLevel = new ArrayList<>();
		minLevel = LogMessage.Level.Info;
	}

	void setLogger(UILogger newLogger)
	{
		if (logger != null) {
			logger.listeners.removeListener(this.listenerOwner);
			logger = null;
		}
		clearCurrentContent();
		logger = newLogger;
		logger.listeners.addListener(this.listenerOwner, new UILoggerListener() {
			
			@Override
			public void newMessageReceived() {
				updateDataSize();
			}
		});
		updateDataSize();
	}

	private void clearCurrentContent() {
		if (!logOfCurrentLevel.isEmpty()) {
			fireTableRowsDeleted(0, logOfCurrentLevel.size() - 1);
			logOfCurrentLevel = new ArrayList<>();
		}
		lastLoggerCount = 0;
	}
	
	boolean filter(LogMessage lm)
	{
		return lm.level.ordinal() >= minLevel.ordinal();
	}
	
	void updateDataSize() {
		int newDataSize = logger.getMessageCount();
		int oldLogOfCurrentLevel = logOfCurrentLevel.size();
		while(lastLoggerCount < newDataSize) {
			
			LogMessage lm = logger.getMessage(lastLoggerCount);
			if (filter(lm)) {
				logOfCurrentLevel.add(lm);
			}
			
			lastLoggerCount++;
		}
		if (logOfCurrentLevel.size() != oldLogOfCurrentLevel) {
			fireTableRowsInserted(oldLogOfCurrentLevel, logOfCurrentLevel.size() - 1);
		}
	}
	
	private void refilter()
	{
		clearCurrentContent();
		updateDataSize();
	}
	
	
	public void setLogLevel(LogMessage.Level l)
	{
		if (this.minLevel == l) {
			return;
		}
		this.minLevel = l;
		refilter();
	}
	
	
	@Override
	public String getColumnName(int column) {
		// TODO Auto-generated method stub
		return super.getColumnName(column);
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
	
	@Override
	public int getColumnCount() {
		return columns.length;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		super.setValueAt(aValue, rowIndex, columnIndex);
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		LogMessage lm = logOfCurrentLevel.get(rowIndex);
		return columns[columnIndex].get(lm);
	}
	
	@Override
	public int getRowCount() {
		return logOfCurrentLevel.size();
	}
	
}
