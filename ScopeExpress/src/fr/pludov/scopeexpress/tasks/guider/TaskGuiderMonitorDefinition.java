package fr.pludov.scopeexpress.tasks.guider;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

/**
 * V�rifie que le guidage est dans des bornes acceptables
 * Echoue quand le guider sort des bornes.
 */
public class TaskGuiderMonitorDefinition extends BaseTaskDefinition
{
	
	final DoubleParameterId guiderDriftTolerance = 
			new DoubleParameterId(this, "maxGuiderDriftDuration", ParameterFlag.Input, ParameterFlag.Mandatory) {
		{
			setTitle("Dur�e avant arr�t");
			setTooltip("Dur�e (secondes) pendant laquelle le guider peut rester au del� du seuil d'arr�t, ou rester sans r�ponse (�toile perdue)");
			setDefault(10.0);
		}
	};
	

	final DoubleParameterId maxGuiderDriftArcSec = 
			new DoubleParameterId(this, "maxGuiderDriftArcSec", ParameterFlag.Input) {
		{
			setTitle("Seuil d'arr�t");
			setTooltip("Distance maxi (en arcsec) tol�r�e pendant un clich�. Au del�, le clich� est abandonn�. Vide pour d�sactiver (dans ce cas, on v�rifie seulement que PHD est toujours l�)");
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
			setTitle("Dur�e minimum de stabilit� (s)");
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
				taskView.addError(guiderDriftTolerance, "Doit �tre superieur � 5 secondes");
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
