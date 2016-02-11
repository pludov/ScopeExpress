package fr.pludov.scopeexpress.tasks.guider;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

/**
 * Suspend l'auto guider. C'est requis éventuellement si:
 *   * pendant le changement de RAF, si le guidage est fait derrière une roue à filtre
 *   * pendant le focus si le guidage est fait derrière un DO
 *
 */
public class TaskGuiderSuspendDefinition extends BaseTaskDefinition
{
	public TaskGuiderSuspendDefinition() {
		super(getBuiltinRepository(), "guiderOff", "Désactivation autoguidage");
	}

	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskGuiderSuspend(focusUi, tm, parentLauncher, this);
	}

	
	@Override
	public void validateSettings(FocusUi focusUi, ITaskParameterTestView taskView) {
		if (taskView.isConfiguration() == false && focusUi.getGuiderManager().getConnectedDevice() == null) {
			taskView.addTopLevelError(ITaskParameterTestView.autoGuiderRequired);
		}

		super.validateSettings(focusUi, taskView);
	}
	
	private static TaskGuiderSuspendDefinition instance;
	public static synchronized final TaskGuiderSuspendDefinition getInstance()
	{
		if (instance == null) {
			instance = new TaskGuiderSuspendDefinition();
		}
		return instance;
	}
}
