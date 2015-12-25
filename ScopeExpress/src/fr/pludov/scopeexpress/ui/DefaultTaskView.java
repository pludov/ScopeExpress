package fr.pludov.scopeexpress.ui;

import javax.swing.JComponent;
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
}
