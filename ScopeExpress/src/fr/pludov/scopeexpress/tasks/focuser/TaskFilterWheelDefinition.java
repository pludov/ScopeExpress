package fr.pludov.scopeexpress.tasks.focuser;

import java.util.Arrays;

import fr.pludov.scopeexpress.filterwheel.FilterWheel;
import fr.pludov.scopeexpress.filterwheel.FilterWheelException;
import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.BaseTaskDefinition;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.IParameterEditionContext;
import fr.pludov.scopeexpress.tasks.ParameterFlag;
import fr.pludov.scopeexpress.tasks.SimpleFieldDialog;
import fr.pludov.scopeexpress.tasks.StringParameterId;
import fr.pludov.scopeexpress.tasks.TaskManager;
import fr.pludov.scopeexpress.ui.FocusUi;

/** Prend une photo */
public class TaskFilterWheelDefinition extends BaseTaskDefinition {
	

	public final StringParameterId filter = new StringParameterId(this, "filter", ParameterFlag.Input) {
		@Override
		public SimpleFieldDialog<String> buildDialog(FocusUi focusUi, IParameterEditionContext ipec) {
			return new FilterNameFieldDialog(focusUi, this, ipec);
		}
		
		@Override
		public String sanitizeValue(FocusUi focusUi, IParameterEditionContext paramCtxt, String currentValue) {
			String[] filters = {};

			FilterWheel fw = focusUi.getFilterWheelManager().getConnectedDevice();
			if (fw != null) {
				try {
					filters = fw.getFilters();
				} catch (FilterWheelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (filters.length > 0) {
				if (currentValue == null || !Arrays.asList(filters).contains(currentValue)) {
					currentValue = filters[0];
				}
			}

			return currentValue;
		};

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

/*	@Override
	public IConfigurationDialog parameterUi(List<TaskParameterId<?>> required) {
		ComposedConfigurationDialog dialog = new ComposedConfigurationDialog();
		if (required.contains(bin)) {
			dialog.add(new IntegerFieldDialog(bin).setTitleText("Binning"));
		}
		return dialog;
	}*/
	
	private static final TaskFilterWheelDefinition tsd = new TaskFilterWheelDefinition();
	public static final TaskFilterWheelDefinition getInstance() {
		return tsd;
	}
}
