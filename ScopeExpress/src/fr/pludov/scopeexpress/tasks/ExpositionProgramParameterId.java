package fr.pludov.scopeexpress.tasks;

import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.tasks.focuser.*;
import fr.pludov.scopeexpress.ui.*;

public class ExpositionProgramParameterId extends TaskParameterId<ExpositionProgram>{


	public ExpositionProgramParameterId(BaseTaskDefinition td, String id, ParameterFlag ... scope) {
		super(td, id, scope);
	}

	@Override
	public IFieldDialog<ExpositionProgram> buildDialog(FocusUi focusUi) {
		return new ExpositionProgramFieldDialog(this);
	}

	@Override
	public Object toJavascript(ExpositionProgram value, Scriptable scope) {
		throw new RuntimeException("unsupported");
	}

	@Override
	public ExpositionProgram fromJavascript(Object o, Scriptable scope) {
		throw new RuntimeException("unsupported");
	}

	@Override
	public ExpositionProgram sanitizeValue(FocusUi focusUi, ExpositionProgram currentValue) {
		return currentValue;
	}

}
