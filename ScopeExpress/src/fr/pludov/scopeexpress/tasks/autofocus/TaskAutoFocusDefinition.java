package fr.pludov.scopeexpress.tasks.autofocus;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.tasks.shoot.*;
import fr.pludov.scopeexpress.ui.*;

public final class TaskAutoFocusDefinition extends BaseTaskDefinition
{
	public final IntegerParameterId minStarCount = 
			new IntegerParameterId(this, "minStarCount", ParameterFlag.Input, ParameterFlag.PresentInConfig) {
				{
					setTitle("Nombre d'�toiles");
					setTooltip("Ajuster le temps de pause pour avoir au moins ce nombre d'�toiles");
					setDefault(25);
				}
			};
	public final IntegerParameterId initialFocuserPosition = new IntegerParameterId(this, "initialFocuserPosition", ParameterFlag.Input, ParameterFlag.Mandatory) {
		{
			setTitle("Position cibl�e sur le focuser");
			setTooltip("La premi�re passe de mise au point est centr�e sur cette position");
		}
	};
	public final IntegerParameterId initialFocuserRange = new IntegerParameterId(this, "initialFocuserRange", ParameterFlag.Input, ParameterFlag.PresentInConfig) {
		{
			setTitle("Largeur de la premi�re passe");
			setTooltip("Largeur (en pas de focuser) de la premi�re passe de mise au point");
			setDefault(1000);
		}
	};
	// Nombre de pas
	public final IntegerParameterId stepCount = new IntegerParameterId(this, "stepCount", ParameterFlag.Input, ParameterFlag.PresentInConfig) {
		{
			setTitle("Positions test�es");
			setTooltip("Nombre de positions du focuser test�s lors de la premi�re passe de mise au points");
			setDefault(10);
		}
	};
	
	// Nombre de photo par step
	public final IntegerParameterId photoCount = new IntegerParameterId(this, "photoCount", ParameterFlag.Input, ParameterFlag.PresentInConfig) {
		{
			setTitle("Photo par position");
			setTooltip("Nombre de pauses effectu�es sur chaque position test�e");
			setDefault(1);
		}
	};
	

	// Le backlash
	public final IntegerParameterId backlash = new IntegerParameterId(this, "backlash", ParameterFlag.PresentInConfig) {
		{
			setTitle("Backlash");
			setTooltip("Les mouvements demand� au focuseur reviendront toujours en arri�re d'au moins ce nombre de pas lors d'un changement de direction");
			setDefault(500);
		}
	};
	
//	final ObjectParameterId<TaskAutoFocusPasses> passes = new ObjectParameterId<>(this, "passes", TaskAutoFocusPasses.class, ParameterFlag.Input, ParameterFlag.Mandatory);
	
	public final TaskLauncherDefinition shoot = new TaskLauncherDefinition(this, "shoot", TaskShootDefinition.getInstance()) {
		{
			shootExposure = new TaskLauncherOverride<>(this, TaskShootDefinition.getInstance().exposure);
			shootKind = new TaskLauncherOverride<>(this, TaskShootDefinition.getInstance().kind);
		}
	};
	TaskLauncherOverride<Double> shootExposure;
	TaskLauncherOverride<ShootKind> shootKind;
	
	
	// final EnumParameterId<Status> status = new EnumParameterId<>(this, "status", Status.class, ParameterFlag.Internal);
	
	TaskAutoFocusDefinition() {
		super(getBuiltinRepository(), "autofocus", "Autofocus");
	}

	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskAutoFocus(focusUi, tm, parentLauncher, this);
	}
	
	@Override
	public TaskDetailView getViewer(FocusUi focusUi) {
		return DefaultTaskView.wrapInTabWithMessages(focusUi, new TaskAutoFocusDetails(focusUi), "D�tails");
	}
	
	
	private static TaskAutoFocusDefinition instance;
	public static synchronized final TaskAutoFocusDefinition getInstance()
	{
		if (instance == null) {
			instance = new TaskAutoFocusDefinition();
		}
		return instance;
	}
}