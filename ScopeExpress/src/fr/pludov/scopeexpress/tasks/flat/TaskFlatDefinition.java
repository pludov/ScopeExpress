package fr.pludov.scopeexpress.tasks.flat;

import java.util.*;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.tasks.focuser.*;
import fr.pludov.scopeexpress.tasks.guider.*;
import fr.pludov.scopeexpress.tasks.shoot.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.TaskFieldStatus.*;

public class TaskFlatDefinition extends BaseTaskDefinition {
	
	
	final IntegerParameterId shootCount = 
			new IntegerParameterId(this, "shootCount", ParameterFlag.Input) {
		{
			setTitle("Nombre de prises");
			setTooltip("Nombre de prises de vues");
			setDefault(10);
		}
	};

	final EnumParameterId<ExposureMethod> exposureMethod = 
			new EnumParameterId<ExposureMethod>(this, "exposureMethod", ExposureMethod.class, ParameterFlag.Input) {
		{
			setTitle("Choix d'exposition");
			setTooltip("Décide l'algorithme utilisé pour déterminer la durée d'exposition des flats");
			setDefault(ExposureMethod.TargetAdu);
		}
	};


	final DoubleParameterId forcedExposure =
			new DoubleParameterId(this, "forcedExposure", ParameterFlag.Input) {
		{
			setTitle("Durée d'exposition");
			setTooltip("Temps de pause utilisé pour tous les clichés de flat");
			setDefault(1.0);
		}
	};
	
	final IntegerParameterId targetAdu =
			new IntegerParameterId(this, "targetAdu", ParameterFlag.Input) {
		{
			setTitle("Valeur ADU");
			setTooltip("Le temps de pause sera ajusté sur le premier filtre pour avoir ce niveau d'ADU (valeur moyenne)");
			setDefault(16000);
		}
	};

	final IntegerParameterId maxCalibrationShoot = 
			new IntegerParameterId(this, "maxCalibrationShoot", ParameterFlag.Input) {
		{
			setTitle("Essai max");
			setTooltip("Nombre maxi de prises de vues de calibration");
			setDefault(10);
		}
	};
	
	
	final EnumParameterId<GuiderHandling> guiderHandling = 
			new EnumParameterId<GuiderHandling>(this, "guiderHandling", GuiderHandling.class, ParameterFlag.Input) {
		{
			setTitle("Guidage");
			setTooltip("Contrôle la collaboration avec Phd Guiding");
			setDefault(GuiderHandling.Interrupt);
		}
	};

	final TaskLauncherDefinition guiderStop = new TaskLauncherDefinition(this, "guiderStop", TaskGuiderSuspendDefinition.getInstance()) {
		{
		}
	};

	final TaskLauncherDefinition filterWheel = new TaskLauncherDefinition(this, "filterWheel", TaskFilterWheelDefinition.getInstance()) {
		{
		}
	};


	final TaskLauncherDefinition shoot = new TaskLauncherDefinition(this, "shoot", TaskShootDefinition.getInstance());
	
	final TaskLauncherOverride<Double> shootExposure = new TaskLauncherOverride<>(this.shoot, TaskShootDefinition.getInstance().exposure);
	final TaskLauncherOverride<ShootKind> shootKind = new TaskLauncherOverride<>(this.shoot, TaskShootDefinition.getInstance().kind);

	
	TaskFlatDefinition() {
		super(getBuiltinRepository(), "flat", "Flat");
	}

	@Override
	public ArrayList<DisplayOrderConstraint> buildDisplayOrderConstraints()
	{
		ArrayList<DisplayOrderConstraint> result = super.buildDisplayOrderConstraints();
		return result;
	}
	
	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskFlat(focusUi, tm, parentLauncher, this);
	}
	
	@Override
	public void declareControlers(final TaskParameterPanel td, final SubTaskPath path) {
		
		td.addControler(path.forParameter(this.forcedExposure), onlyForView(td, path, ExposureMethod.FixedExposure));
		td.addControler(path.forParameter(this.maxCalibrationShoot), onlyForView(td, path, ExposureMethod.TargetAdu));
		td.addControler(path.forParameter(this.targetAdu), onlyForView(td, path, ExposureMethod.TargetAdu));
		
		super.declareControlers(td, path);
	}

	private <T> TaskFieldControler<T> onlyForView(final TaskParameterPanel td, final SubTaskPath path, final ExposureMethod onlyThisEm) {
		return new TaskFieldControler<T>() {
			@Override
			public TaskFieldStatus<T> getFieldStatus(TaskFieldControler<T> parent) {
				ExposureMethod em;
				try {
					em = td.getParameterValue(path.forParameter(exposureMethod));
					if (em != null && em != onlyThisEm) {
						return new TaskFieldStatus<>(Status.MeaningLess);
					}
				} catch (ParameterNotKnownException e) {
				}
				
				return parent.getFieldStatus(null);
			}
		};
	}

	@Override
	public void validateSettings(FocusUi focusUi, ITaskParameterTestView taskView) {

		try {
			if (taskView.get(this.guiderHandling) != GuiderHandling.Interrupt) {
				taskView.getSubTaskView(this.guiderStop).disableValidation();
			}
		} catch (ParameterNotKnownException e) {
		}
		taskView.getSubTaskView(this.shoot).setUndecided(TaskShootDefinition.getInstance().exposure);
		taskView.getSubTaskView(this.shoot).setUndecided(TaskShootDefinition.getInstance().kind);
		
				
		super.validateSettings(focusUi, taskView);
	}
	
	private static TaskFlatDefinition instance;
	public static synchronized final TaskFlatDefinition getInstance()
	{
		if (instance == null) {
			instance = new TaskFlatDefinition();
		}
		return instance;
	}
}
