package fr.pludov.scopeexpress.tasks.guider;

import java.util.*;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

public class TaskGuiderStart extends TaskGuiderAbstractControl {
	
	public TaskGuiderStart(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, TaskGuiderStartDefinition tafd) {
		super(focusUi, tm, parentLauncher, tafd);
	}
	
	@Override
	public TaskGuiderStartDefinition getDefinition() {
		return (TaskGuiderStartDefinition) super.getDefinition();
	}


	@Override
	void prepareGuideQuery() {
		guideQuery.put("method", "guide");
		List<Object> params = new ArrayList<>();
		
		Map<String, Object> settle = new HashMap<>();
		settle.put("pixels", get(getDefinition().pixels));
		settle.put("time", get(getDefinition().time));
		settle.put("timeout", get(getDefinition().timeout));
		params.add(settle);
		params.add(false);
		guideQuery.put("params", params);
	}
}
