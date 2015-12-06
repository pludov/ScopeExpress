package fr.pludov.scopeexpress.tasks;

/**
 * Indique q'un param�tre va �tre forc� par la tache appellante.
 */
public class TaskLauncherOverride<DATA> {
	// Le param�tre qui est surcharg�
	final TaskParameterId<DATA> parameter;
	
	public TaskLauncherOverride(TaskLauncherDefinition parent, TaskParameterId<DATA> parameter) {
		this.parameter = parameter;
		parent.overrides.add(this);
	}

	public TaskParameterId<DATA> getParameter() {
		return parameter;
	}

}
