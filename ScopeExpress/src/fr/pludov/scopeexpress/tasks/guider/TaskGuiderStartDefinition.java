package fr.pludov.scopeexpress.tasks.guider;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

public class TaskGuiderStartDefinition extends TaskGuiderAbstractControlDefinition
{
	public TaskGuiderStartDefinition() {
		super(getBuiltinRepository(), "guiderOn", "Activation autoguidage");
	}

	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskGuiderStart(focusUi, tm, parentLauncher, this);
	}

	
	private static TaskGuiderStartDefinition instance;
	public static synchronized final TaskGuiderStartDefinition getInstance()
	{
		if (instance == null) {
			instance = new TaskGuiderStartDefinition();
		}
		return instance;
	}
}
