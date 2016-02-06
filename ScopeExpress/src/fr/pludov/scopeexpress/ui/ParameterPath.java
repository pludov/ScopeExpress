package fr.pludov.scopeexpress.ui;

import fr.pludov.scopeexpress.tasks.*;

public class ParameterPath<TYPE> {
	final SubTaskPath taskPath;
	final TaskParameterId<TYPE> parameter;
	
	ParameterPath(SubTaskPath taskPath, TaskParameterId<TYPE> parameter) {
		super();
		this.taskPath = taskPath;
		this.parameter = parameter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((parameter == null) ? 0 : parameter.hashCode());
		result = prime * result + ((taskPath == null) ? 0 : taskPath.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParameterPath other = (ParameterPath) obj;
		if (parameter == null) {
			if (other.parameter != null)
				return false;
		} else if (!parameter.equals(other.parameter))
			return false;
		if (taskPath == null) {
			if (other.taskPath != null)
				return false;
		} else if (!taskPath.equals(other.taskPath))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ParameterPath [taskPath=" + taskPath + ", parameter=" + parameter + "]";
	}

	public SubTaskPath getTaskPath() {
		return taskPath;
	}

	public TaskParameterId<TYPE> getParameter() {
		return parameter;
	}
	
	
}
