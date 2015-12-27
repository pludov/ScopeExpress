package fr.pludov.scopeexpress.ui.log;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import fr.pludov.scopeexpress.ui.log.LogMessage.Level;
import fr.pludov.scopeexpress.ui.preferences.EnumConfigItem;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;
import fr.pludov.scopeexpress.utils.WeakListenerCollection.AsyncKind;

public class LogViewPanel extends JPanel {
	private final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	final LogViewTable table;
	final JComboBox<LogMessage.Level> levelSelector;
	
	static final EnumConfigItem<LogMessage.Level> defaultLogLevel = new EnumConfigItem(LogViewPanel.class, "logLevel", LogMessage.Level.class, LogMessage.Level.Info);
	static final WeakListenerCollection<DefaultLogLevelChangeListener> defaultLogLevelListener = new WeakListenerCollection<>(DefaultLogLevelChangeListener.class, AsyncKind.SwingQueueIfRequired);
	
	public LogViewPanel() {
		setLayout(new BorderLayout());

		JPanel pseudoCommandLine = new JPanel();
		pseudoCommandLine.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		levelSelector = new JComboBox<LogMessage.Level>(LogMessage.Level.values());
		JLabel levelLabel = new JLabel("Niveau:");
		
		
		pseudoCommandLine.add(levelLabel);
		pseudoCommandLine.add(levelSelector);
		
		add(pseudoCommandLine, BorderLayout.NORTH);
		
		table = new LogViewTable();
		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);
		add(scrollPane);
		levelSelector.setSelectedItem(defaultLogLevel.get());
		defaultLogLevelListener.addListener(this.listenerOwner, new DefaultLogLevelChangeListener() {
			@Override
			public void levelChanged() {
				if (levelSelector.getSelectedItem() != defaultLogLevel.get()) {
					levelSelector.setSelectedItem(defaultLogLevel.get());
				}
			}
		});
		levelSelector.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				updateLoggerLevel();
				LogMessage.Level lm = (Level) levelSelector.getSelectedItem();
				defaultLogLevel.set(lm);;
				defaultLogLevelListener.getTarget().levelChanged();
			}
		});

		updateLoggerLevel();
	}

	public void setLogger(UILogger logger) {
		table.setLogger(logger);
	}
	
	public void updateLoggerLevel()
	{
		LogMessage.Level lm = (Level) levelSelector.getSelectedItem();
		table.getModel().setLogLevel(lm);
	}

}
