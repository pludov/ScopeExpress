package fr.pludov.scopeexpress.ui;

import javax.swing.*;

import fr.pludov.scopeexpress.script.*;

/** Tourne dans le thread swing */
public class ApplicationTaskGroup extends TaskGroup {

	boolean pending = false;
	
	public ApplicationTaskGroup() {
	}
	
	@Override
	public void addRunnableTask(Task jsTask) {
		super.addRunnableTask(jsTask);
		
		restartEval();
	}
	
	void restartEval() {
		if (!pending) {
			pending = true;
			SwingUtilities.invokeLater(() -> { 
				pending = false; 
				if (advance()) {
					restartEval();
				}
			});
		}
	}
}
