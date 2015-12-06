package fr.pludov.scopeexpress.tasks;

/**
 * Indique q'un paramètre va être forcé par la tache appellante.
 */
public class TaskLauncherOverride<DATA> {
	// Le paramètre qui est surchargé
	final TaskParameterId<DATA> parameter;
	
	public TaskLauncherOverride(TaskLauncherDefinition parent, TaskParameterId<DATA> parameter) {
		this.parameter = parameter;
		parent.overrides.add(this);
	}

	public TaskParameterId<DATA> getParameter() {
		return parameter;
	}

}
