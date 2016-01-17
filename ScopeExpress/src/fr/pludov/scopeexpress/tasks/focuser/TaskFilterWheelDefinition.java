package fr.pludov.scopeexpress.tasks.focuser;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

/** Prend une photo */
public class TaskFilterWheelDefinition extends BaseTaskDefinition {
	

	public final StringParameterId filter = new StringParameterId(this, "filter", ParameterFlag.Input) {
		@Override
		public SimpleFieldDialog<String> buildDialog(FocusUi focusUi, IParameterEditionContext ipec) {
			return new FilterNameFieldDialog(focusUi, this, ipec);
		}
		
		@Override
		public String sanitizeValue(FocusUi focusUi, IParameterEditionContext paramCtxt, String currentValue) {
			return FilterNameFieldDialog.sanitizeValue(focusUi, paramCtxt, currentValue);
		}

		{
			setTitle("Filtre");
			setTooltip("Nom du filtre pour le driver ASCOM");
		}
	};

	TaskFilterWheelDefinition() {
		super(getBuiltinRepository(), "filterwheel", "Roue à filtre");
	}
	
	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskFilterWheel(focusUi, tm, parentLauncher, this);
	}


	@Override
	public void validateSettings(FocusUi focusUi, ITaskParameterTestView taskView) {
		if (focusUi.getFilterWheelManager().getConnectedDevice() == null) {
			taskView.addTopLevelError(ITaskParameterTestView.filterWheelRequired);
		}
		super.validateSettings(focusUi, taskView);
	}
	
	private static final TaskFilterWheelDefinition tsd = new TaskFilterWheelDefinition();
	public static final TaskFilterWheelDefinition getInstance() {
		return tsd;
	}
}
