package fr.pludov.scopeexpress.tasks.autofocus;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.tasks.shoot.*;
import fr.pludov.scopeexpress.ui.*;

public class TaskCheckFocusDefinition extends BaseTaskDefinition {
	
	public final DoubleParameterId fwhmSeuil = new DoubleParameterId(this, "fwhmSeuil", ParameterFlag.Input) {
		{
			setTitle("FWHM maxi");
			setTooltip("Considérer que la FWHM est mauvaise au delà de cette valeur (en pixels)");
			setDefault(3.2);
		}
	};

	public final TaskLauncherDefinition shoot = new TaskLauncherDefinition(this, "shoot", TaskShootDefinition.getInstance()) {
		{
//			shootExposure = new TaskLauncherOverride<>(this, TaskShootDefinition.getInstance().exposure);
			shootKind = new TaskLauncherOverride<>(this, TaskShootDefinition.getInstance().kind);
		}
	};
	
	public final IntegerParameterId passed = new IntegerParameterId(this, "passed") {
		{
			setTitle("Résultat 0/1");
		}
	};
//	TaskLauncherOverride<Double> shootExposure;
	TaskLauncherOverride<ShootKind> shootKind;

	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskCheckFocus(focusUi, tm, parentLauncher, this);
	}
	

	TaskCheckFocusDefinition() {
		super(getBuiltinRepository(), "checkfocus", "Check focus");
	}
	
	private static TaskCheckFocusDefinition instance;
	public static synchronized final TaskCheckFocusDefinition getInstance()
	{
		if (instance == null) {
			instance = new TaskCheckFocusDefinition();
		}
		return instance;
	}
}
