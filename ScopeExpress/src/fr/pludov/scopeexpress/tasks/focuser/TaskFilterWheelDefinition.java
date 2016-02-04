package fr.pludov.scopeexpress.tasks.focuser;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

/** Prend une photo */
public class TaskFilterWheelDefinition extends BaseTaskDefinition {
	

	public final StringParameterId filter = new StringParameterId(this, "filter", ParameterFlag.Input) {
		@Override
		public SimpleFieldDialog<String> buildDialog(FocusUi focusUi) {
			return new FilterNameFieldDialog(focusUi, this);
		}
		
		@Override
		public String sanitizeValue(FocusUi focusUi, String currentValue) {
			return FilterNameFieldDialog.sanitizeValue(focusUi, currentValue);
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
	public void validateSettings(FocusUi focusUi, ITaskParameterTestView taskView, ValidationContext validationContext) {
		if (validationContext.isConfiguration() == false && focusUi.getFilterWheelManager().getConnectedDevice() == null) {
			taskView.addTopLevelError(ITaskParameterTestView.filterWheelRequired);
		}
		super.validateSettings(focusUi, taskView, validationContext);
	}
	
	private static final TaskFilterWheelDefinition tsd = new TaskFilterWheelDefinition();
	public static final TaskFilterWheelDefinition getInstance() {
		return tsd;
	}
}
