package fr.pludov.scopeexpress.tasks.guider;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

public class TaskGuiderDitherDefinition extends TaskGuiderAbstractControlDefinition {
	public final DoubleParameterId ammount = new DoubleParameterId(this, "ammount", ParameterFlag.Input, ParameterFlag.Mandatory) {
		{
			setTitle("Dithering distance (pix)");
			setTooltip("The value (in pixel) is actually multiplied by the Dither Scale value in the Brain of phd2.");
			setDefault(1.0);
		}
	};
	public final BooleanParameterId raOnly = new BooleanParameterId(this, "raonly", ParameterFlag.Input) {
		{
			setTitle("RA only");
			setTooltip("Limit dithering to RA");
			setDefault(false);
		}
	};

	public TaskGuiderDitherDefinition() {
		super(getBuiltinRepository(), "guiderDither", "Dithering de l'autoguidage");
	}

	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskGuiderDither(focusUi, tm, parentLauncher, this);
	}

	
	private static TaskGuiderDitherDefinition instance;
	public static synchronized final TaskGuiderDitherDefinition getInstance()
	{
		if (instance == null) {
			instance = new TaskGuiderDitherDefinition();
		}
		return instance;
	}

}
