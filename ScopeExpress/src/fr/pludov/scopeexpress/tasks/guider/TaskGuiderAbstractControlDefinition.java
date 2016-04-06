package fr.pludov.scopeexpress.tasks.guider;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

public abstract class TaskGuiderAbstractControlDefinition extends BaseTaskDefinition {

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

	public TaskGuiderAbstractControlDefinition(WritableTaskDefinitionRepository repository, String id,
			String defaultTitle) {
		super(repository, id, defaultTitle);
	}

	@Override
	public void validateSettings(FocusUi focusUi, ITaskParameterTestView taskView) {
		if (taskView.isConfiguration() == false && focusUi.getGuiderManager().getConnectedDevice() == null) {
			taskView.addTopLevelError(ITaskParameterTestView.autoGuiderRequired);
		}
	
		super.validateSettings(focusUi, taskView);
	}

}