package fr.pludov.scopeexpress.script;

import java.util.*;

import fr.pludov.scopeexpress.utils.*;

public interface TaskOrGroup {
	
	public boolean isTerminated();
	TaskOrGroup getParent();
	List<Task> getChilds();
	public WeakListenerCollection<TaskStatusListener> getStatusListeners();
	public WeakListenerCollection<TaskChildListener> getChildListeners();
	public String getTitle();
	public String getStatusIconId();

}
