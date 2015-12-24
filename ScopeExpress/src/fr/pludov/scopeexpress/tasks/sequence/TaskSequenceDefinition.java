package fr.pludov.scopeexpress.tasks.sequence;

import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.BaseTaskDefinition;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.IntegerParameterId;
import fr.pludov.scopeexpress.tasks.ParameterFlag;
import fr.pludov.scopeexpress.tasks.TaskDetailView;
import fr.pludov.scopeexpress.tasks.TaskLauncherDefinition;
import fr.pludov.scopeexpress.tasks.TaskLauncherOverride;
import fr.pludov.scopeexpress.tasks.TaskManager;
import fr.pludov.scopeexpress.tasks.autofocus.TaskAutoFocus;
import fr.pludov.scopeexpress.tasks.autofocus.TaskAutoFocusDefinition;
import fr.pludov.scopeexpress.tasks.focuser.TaskFilterWheelDefinition;
import fr.pludov.scopeexpress.tasks.guider.TaskGuiderStartDefinition;
import fr.pludov.scopeexpress.tasks.shoot.TaskShootDefinition;
import fr.pludov.scopeexpress.ui.FocusUi;;

public class TaskSequenceDefinition extends BaseTaskDefinition {
	final IntegerParameterId shootCount = 
			new IntegerParameterId(this, "shootCount", ParameterFlag.Input, ParameterFlag.PresentInConfig) {
				{
					setTitle("Nombre de prises");
					setTooltip("Nombre de prises de vues");
					setDefault(10);
				}
			};

	final TaskLauncherDefinition autofocus = new TaskLauncherDefinition(this, "autofocus", TaskAutoFocusDefinition.getInstance()) {
		{
			// autofocusShootExposure = new TaskLauncherOverride<>(this, TaskAutoFocusDefinition.getInstance());
		}
	};

	final TaskLauncherDefinition guiderStart = new TaskLauncherDefinition(this, "guiderStart", TaskGuiderStartDefinition.getInstance()) {
		{
			// autofocusShootExposure = new TaskLauncherOverride<>(this, TaskAutoFocusDefinition.getInstance());
		}
	};

	final TaskLauncherDefinition filterWheel = new TaskLauncherDefinition(this, "filterWheel", TaskFilterWheelDefinition.getInstance()) {
		{
			// autofocusShootExposure = new TaskLauncherOverride<>(this, TaskAutoFocusDefinition.getInstance());
			
		}
	};


	final TaskLauncherDefinition shoot = new TaskLauncherDefinition(this, "shoot", TaskShootDefinition.getInstance());
	
	
	TaskSequenceDefinition() {
		super(getBuiltinRepository(), "sequence", "Sequence");
	}

	@Override
	public TaskDetailView getViewer(FocusUi focusUi) {
		return null;
	}

	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskSequence(focusUi, tm, parentLauncher, this);
	}
	
	private static TaskSequenceDefinition instance;
	public static synchronized final TaskSequenceDefinition getInstance()
	{
		if (instance == null) {
			instance = new TaskSequenceDefinition();
		}
		return instance;
	}
}
