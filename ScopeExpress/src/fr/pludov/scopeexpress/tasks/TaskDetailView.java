package fr.pludov.scopeexpress.tasks;

import javax.swing.JComponent;

public interface TaskDetailView {
	JComponent getMainPanel();
	
	void setTask(BaseTask bt);
}
