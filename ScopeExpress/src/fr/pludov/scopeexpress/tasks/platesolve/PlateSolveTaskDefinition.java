package fr.pludov.scopeexpress.tasks.platesolve;

import java.util.List;

import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.BaseTaskDefinition;
import fr.pludov.scopeexpress.tasks.BuiltinTaskDefinitionRepository;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.ComposedConfigurationDialog;
import fr.pludov.scopeexpress.tasks.DoubleParameterId;
import fr.pludov.scopeexpress.tasks.IConfigurationDialog;
import fr.pludov.scopeexpress.tasks.IntegerFieldDialog;
import fr.pludov.scopeexpress.tasks.IntegerParameterId;
import fr.pludov.scopeexpress.tasks.ObjectParameterId;
import fr.pludov.scopeexpress.tasks.ParameterFlag;
import fr.pludov.scopeexpress.tasks.TaskDetailView;
import fr.pludov.scopeexpress.tasks.TaskLauncherDefinition;
import fr.pludov.scopeexpress.tasks.TaskLauncherOverride;
import fr.pludov.scopeexpress.tasks.TaskManager;
import fr.pludov.scopeexpress.tasks.TaskParameterId;
import fr.pludov.scopeexpress.tasks.shoot.TaskShootDefinition;
import fr.pludov.scopeexpress.ui.FocusUi;

/** FIXME: on veut changer de filtre peut être ? Dans ce cas, il faut faire un refocus ? */
public final class PlateSolveTaskDefinition extends BaseTaskDefinition
{
	final IntegerParameterId minStarCount = 
			new IntegerParameterId(this, "minStarCount", ParameterFlag.Input, ParameterFlag.PresentInConfig) {
				{
					setTitle("Nombre d'étoiles");
					setTooltip("Ajuster le temps de pause pour avoir au moins ce nombre d'étoiles");
					setDefault(25);
				}
			};
	final DoubleParameterId startExposure = new DoubleParameterId(this, "startExposure", ParameterFlag.Input, ParameterFlag.Mandatory) {
		{
			setDefault(1.0);
		}
	};
	final DoubleParameterId maxExposure = new DoubleParameterId(this, "maxExposure", ParameterFlag.Input, ParameterFlag.Mandatory) {
		{
			setTooltip("Temps de pause maxi pour avoir suffisement d'étoiles");
			setDefault(32.0);
		}
	};
	
	
	
	final TaskLauncherDefinition shoot = new TaskLauncherDefinition(this, "shoot", TaskShootDefinition.getInstance()) {
		{
			shootExposure = new TaskLauncherOverride<>(this, TaskShootDefinition.getInstance().exposure);
		}
	};
	TaskLauncherOverride<Double> shootExposure;
	
	
	// final EnumParameterId<Status> status = new EnumParameterId<>(this, "status", Status.class, ParameterFlag.Internal);
	
	PlateSolveTaskDefinition() {
		super(getBuiltinRepository(), "platesolve", "Plate solve");
	}

	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new PlateSolveTask(focusUi, tm, parentLauncher, this);
	}
	
	@Override
	public TaskDetailView getViewer(FocusUi focusUi) {
		return null;
	}
	
	
	private static PlateSolveTaskDefinition instance;
	public static synchronized final PlateSolveTaskDefinition getInstance()
	{
		if (instance == null) {
			instance = new PlateSolveTaskDefinition();
		}
		return instance;
	}
}