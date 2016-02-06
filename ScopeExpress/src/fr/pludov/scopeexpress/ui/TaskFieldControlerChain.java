package fr.pludov.scopeexpress.ui;

import java.util.*;

import fr.pludov.scopeexpress.ui.TaskFieldStatus.*;

public class TaskFieldControlerChain<TYPE> implements TaskFieldControler<TYPE> {

	final List<TaskFieldControler<TYPE>> childs;
	
	int currentPos;
	
	TaskFieldControlerChain(List<TaskFieldControler<TYPE>> childs)
	{
		this.childs = childs;
	}
	
	@Override
	public TaskFieldStatus<TYPE> getFieldStatus(TaskFieldControler<TYPE> parent) {
		// demander à currentPos...
		if (currentPos < childs.size()) {
			currentPos++;
			try {
				return childs.get(currentPos - 1).getFieldStatus(this);
			} finally {
				currentPos--;
			}
		} else {
			if (parent != null && parent != this) {
				return parent.getFieldStatus(null);
			} else {
				return new TaskFieldStatus<>(Status.Visible);
			}
		}
	}

}
