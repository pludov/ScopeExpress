package fr.pludov.scopeexpress.tasks.sequence;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.tasks.autofocus.*;
import fr.pludov.scopeexpress.tasks.focuser.*;
import fr.pludov.scopeexpress.tasks.guider.*;
import fr.pludov.scopeexpress.tasks.shoot.*;
import fr.pludov.scopeexpress.ui.*;;

public class TaskSequenceDefinition extends BaseTaskDefinition {
	final IntegerParameterId shootCount = 
			new IntegerParameterId(this, "shootCount", ParameterFlag.Input) {
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
