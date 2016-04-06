package fr.pludov.scopeexpress.tasks.guider;

import java.util.*;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

public class TaskGuiderDither extends TaskGuiderAbstractControl {
	
	public TaskGuiderDither(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, TaskGuiderDitherDefinition tafd) {
		super(focusUi, tm, parentLauncher, tafd);
	}
	
	@Override
	public TaskGuiderDitherDefinition getDefinition() {
		return (TaskGuiderDitherDefinition) super.getDefinition();
	}

	@Override
	void prepareGuideQuery() {
		guideQuery.put("method", "dither");
		List<Object> params = new ArrayList<>();

		params.add(get(getDefinition().ammount));
		params.add(get(getDefinition().raOnly));
		Map<String, Object> settle = new HashMap<>();
		settle.put("pixels", get(getDefinition().pixels));
		settle.put("time", get(getDefinition().time));
		settle.put("timeout", get(getDefinition().timeout));
		params.add(settle);
		guideQuery.put("params", params);
	}
}
