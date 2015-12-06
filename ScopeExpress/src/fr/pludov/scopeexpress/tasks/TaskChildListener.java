package fr.pludov.scopeexpress.tasks;

public interface TaskChildListener {
	void childAdded(ChildLauncher launcher);
	void childRemoved(ChildLauncher launcher);
	
}
