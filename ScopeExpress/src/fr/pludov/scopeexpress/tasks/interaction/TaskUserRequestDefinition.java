package fr.pludov.scopeexpress.tasks.interaction;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

public class TaskUserRequestDefinition extends BaseTaskDefinition {

	public final StringParameterId userMessage = new StringParameterId(this, "userMessage", ParameterFlag.Input, ParameterFlag.Mandatory) {
		{
			setTitle("Label");
			setTooltip("Ce texte sera affiché lors de la demande de confirmation de l'utilisateur");
		}
	};

	TaskUserRequestDefinition()
	{
		super(getBuiltinRepository(), "userRequest", "Interaction utilisateur");
	}
	
	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskUserRequest(focusUi, tm, parentLauncher, this);
	}

	@Override
	public TaskDetailView getViewer(FocusUi focusUi) {
		return DefaultTaskView.wrapInTabWithMessages(focusUi, new TaskUserRequestDetails(), "Détails");
	}
	
	private static TaskUserRequestDefinition instance;
	public static synchronized final TaskUserRequestDefinition getInstance()
	{
		if (instance == null) {
			instance = new TaskUserRequestDefinition();
		}
		return instance;
	}
}
