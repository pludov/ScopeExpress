package fr.pludov.scopeexpress.tasks;

import javax.swing.JPanel;

public interface TaskDetailView {
	JPanel getMainPanel();
	
	void setTask(BaseTask bt);
}
