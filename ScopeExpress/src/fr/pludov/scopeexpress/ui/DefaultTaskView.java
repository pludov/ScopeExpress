package fr.pludov.scopeexpress.ui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.TaskDetailView;
import fr.pludov.scopeexpress.ui.log.LogViewPanel;

public class DefaultTaskView implements TaskDetailView{
	final LogViewPanel logView;
	
	public DefaultTaskView(FocusUi focusUi) {
		logView = new LogViewPanel();
		
	}

	@Override
	public JComponent getMainPanel() {
		return logView;
	}
	
	@Override
	public void setTask(BaseTask bt) {
		logView.setLogger(bt.logger);
		
	}
	
	public static TaskDetailView wrapInTabWithMessages(FocusUi focusUi, TaskDetailView wrapped, String title)
	{
		DefaultTaskViewWrapper result = new DefaultTaskViewWrapper(focusUi);
		result.wrappeds.add(wrapped);
		result.insertTab(title, null, (Component)wrapped, null, 0);
		result.setSelectedIndex(0);
		return result;
	}
	
	private static class DefaultTaskViewWrapper extends JTabbedPane implements TaskDetailView
	{
		LogViewPanel logs;
		List<TaskDetailView> wrappeds;

		DefaultTaskViewWrapper(FocusUi focusUi)
		{
			logs = new LogViewPanel();
			wrappeds = new ArrayList<>();
			insertTab("Messages", null, logs, "Afficher les messages de diagnostique de la tache", 0);
		}
		
		@Override
		public JComponent getMainPanel() {
			return this;
		}

		@Override
		public void setTask(BaseTask bt) {
			logs.setLogger(bt.logger);
			for(TaskDetailView wrapped: wrappeds) {
				wrapped.setTask(bt);
			}
		}
	}
}
