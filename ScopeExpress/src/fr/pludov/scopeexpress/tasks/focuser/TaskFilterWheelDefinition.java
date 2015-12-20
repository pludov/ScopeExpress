package fr.pludov.scopeexpress.tasks.focuser;

import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.BaseTaskDefinition;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.ParameterFlag;
import fr.pludov.scopeexpress.tasks.StringParameterId;
import fr.pludov.scopeexpress.tasks.TaskManager;
import fr.pludov.scopeexpress.ui.FocusUi;

/** Prend une photo */
public class TaskFilterWheelDefinition extends BaseTaskDefinition {
	

	public final StringParameterId filter = new StringParameterId(this, "filter", ParameterFlag.Input) {
		{
			setTitle("Filtre");
			setTooltip("Nom du filtre pour le driver ASCOM");
		}
	};

	TaskFilterWheelDefinition() {
		super(getBuiltinRepository(), "focuser", "Focuser");
	}
	
	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskFilterWheel(focusUi, tm, parentLauncher, this);
	}

/*	@Override
	public IConfigurationDialog parameterUi(List<TaskParameterId<?>> required) {
		ComposedConfigurationDialog dialog = new ComposedConfigurationDialog();
		if (required.contains(bin)) {
			dialog.add(new IntegerFieldDialog(bin).setTitleText("Binning"));
		}
		return dialog;
	}*/
	
	@Override
	public fr.pludov.scopeexpress.tasks.TaskDetailView getViewer(FocusUi focusUi) {
		return null;
	};
	
	private static final TaskFilterWheelDefinition tsd = new TaskFilterWheelDefinition();
	public static final TaskFilterWheelDefinition getInstance() {
		return tsd;
	}
}
