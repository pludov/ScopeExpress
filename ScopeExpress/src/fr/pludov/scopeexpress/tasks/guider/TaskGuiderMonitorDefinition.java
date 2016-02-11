package fr.pludov.scopeexpress.tasks.guider;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

/**
 * Vérifie que le guidage est dans des bornes acceptables
 * Echoue quand le guider sort des bornes.
 */
public class TaskGuiderMonitorDefinition extends BaseTaskDefinition
{
	
	final DoubleParameterId guiderDriftTolerance = 
			new DoubleParameterId(this, "maxGuiderDriftDuration", ParameterFlag.Input, ParameterFlag.Mandatory) {
		{
			setTitle("Durée avant arrêt");
			setTooltip("Durée (secondes) pendant laquelle le guider peut rester au delà du seuil d'arrêt, ou rester sans réponse (étoile perdue)");
			setDefault(10.0);
		}
	};
	

	final DoubleParameterId maxGuiderDriftArcSec = 
			new DoubleParameterId(this, "maxGuiderDriftArcSec", ParameterFlag.Input) {
		{
			setTitle("Seuil d'arrêt");
			setTooltip("Distance maxi (en arcsec) tolérée pendant un cliché. Au delà, le cliché est abandonné. Vide pour désactiver (dans ce cas, on vérifie seulement que PHD est toujours là)");
			setDefault(2.0);
		}
	};
	

	/*
	public final DoubleParameterId pixels = new DoubleParameterId(this, "pixels", ParameterFlag.Input) {
		{
			setTitle("Distance acceptable (pixels)");
			setTooltip("maximum guide distance (pixels) for guiding to be considered stable or 'in-range'");
			setDefault(1.0);
		}
	};
	
	public final IntegerParameterId time = new IntegerParameterId(this, "time", ParameterFlag.Input) {
		{
			setTitle("Durée minimum de stabilité (s)");
			setTooltip("minimum time to be in-range before considering guiding to be stable");
			setDefault(10);
		}
	};
	
	public final IntegerParameterId timeout = new IntegerParameterId(this, "timeout", ParameterFlag.Input) {
		{
			setTitle("Temps max de stabilisation (s)");
			setTooltip("Time limit before settling is considered to have failed");
			setDefault(60);
		}
	};
	*/
	
	public TaskGuiderMonitorDefinition() {
		super(getBuiltinRepository(), "guiderMonitor", "Supervision autoguidage");
	}

	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskGuiderMonitor(focusUi, tm, parentLauncher, this);
	}

	
	@Override
	public void validateSettings(FocusUi focusUi, ITaskParameterTestView taskView) {
		if (taskView.isConfiguration() == false && focusUi.getGuiderManager().getConnectedDevice() == null) {
			taskView.addTopLevelError(ITaskParameterTestView.autoGuiderRequired);
		}

		try {
			if (taskView.get(guiderDriftTolerance) == null || taskView.get(guiderDriftTolerance) < 5) {
				taskView.addError(guiderDriftTolerance, "Doit être superieur à 5 secondes");
			}
		} catch (ParameterNotKnownException e) {
		}
		
		super.validateSettings(focusUi, taskView);
	}
	
	private static TaskGuiderMonitorDefinition instance;
	public static synchronized final TaskGuiderMonitorDefinition getInstance()
	{
		if (instance == null) {
			instance = new TaskGuiderMonitorDefinition();
		}
		return instance;
	}
}
