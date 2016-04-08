package fr.pludov.scopeexpress.tasks;

import java.util.*;

import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.TaskFieldStatus.*;

/** Conserve la définition de chaque paramètre, et comment les présenter à l'utilisateur */
public abstract class BaseTaskDefinition {
	private final String id;
	final String title;
	private final Map<String, TaskParameterId<?>> parameters;
	private final Map<String, TaskLauncherDefinition> taskLaunchers;
	final List<String> childsById;
	
	final TaskDefinitionRepository repository;
	
	public BaseTaskDefinition(WritableTaskDefinitionRepository repository, String id, String defaultTitle) {
		this.repository = repository;
		this.id = id;
		this.title = defaultTitle;
		this.taskLaunchers = new LinkedHashMap<>();
		this.parameters = new LinkedHashMap<>();
		this.childsById = new ArrayList<>();
		repository.declare(this);
	}
	
	public abstract BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher);
	
	public TaskDetailView getViewer(FocusUi focusUi)
	{
		return new DefaultTaskView(focusUi);
	}

	
	public List<TaskParameterId<?>> getParameters()
	{
		return new ArrayList<>(parameters.values());
	}

	public List<TaskLauncherDefinition> getSubTasks() {
		return new ArrayList<>(taskLaunchers.values());
	}

	public Object getChildById(String id)
	{
		TaskParameterId<?> param = parameters.get(id);
		if (param != null) return param;
		TaskLauncherDefinition tld = taskLaunchers.get(id);
		if (tld != null) return tld;
		return null;
	}
	
	public String getTitle() {
		return title;
	}

	public void validateSettings(FocusUi focusUi, ITaskParameterTestView taskView)
	{
		for(TaskLauncherDefinition child: this.taskLaunchers.values())
		{
			ITaskParameterTestView subTaskView = taskView.getSubTaskView(child);
			if (!subTaskView.needValidation()) {
				continue;
			}
			for(TaskLauncherOverride<?> tlo : child.overrides) {
				if (!subTaskView.hasValue(tlo.parameter)) {
					subTaskView.setUndecided(tlo.parameter);
				}
			}
			child.getStartedTask().validateSettings(focusUi, subTaskView);
		}
	}
	
	public TaskDefinitionRepository getRepository()
	{
		return repository;
	}
	
	protected static WritableTaskDefinitionRepository getBuiltinRepository()
	{
		return (WritableTaskDefinitionRepository) BuiltinTaskDefinitionRepository.getInstance();
	}
	
	
	public void declareControlers(TaskParameterPanel td, SubTaskPath path)
	{
		for(TaskLauncherDefinition child: this.taskLaunchers.values())
		{
			child.startedTask.declareControlers(td, path.forChild(child));
			for(final TaskLauncherOverride<?> override : child.overrides)
			{
				// On en met un qui dit qu'on ne sait pas.
				td.addControler(path.forChild(child).forParameter(override.parameter), new TaskFieldControler() {

					@Override
					public TaskFieldStatus getFieldStatus(TaskFieldControler parent) {
						return new TaskFieldStatus(Status.MeaningLess);
					}
				});
				
			}
		}
		
	}
	
	
	@Override
	public String toString() {
		return getTitle();
	}

	public String getId() {
		return id;
	}

	<TYPE> void declareParameter(TaskParameterId<TYPE> taskParameterId) {
		parameters.put(taskParameterId.id, taskParameterId);
		childsById.add(taskParameterId.id);
	}

	public void declareLauncher(TaskLauncherDefinition taskLauncherDefinition) {
		taskLaunchers.put(taskLauncherDefinition.id, taskLauncherDefinition);
		childsById.add(taskLauncherDefinition.id);
	}
	
	public TaskParameterId<?> getTaskParameterById(String id)
	{
		return parameters.get(id);
	}
	
	public TaskLauncherDefinition getLauncherById(String id)
	{
		return taskLaunchers.get(id);
	}
	
	public List<String> getChildIds()
	{
		return new ArrayList<>(childsById);
	}

	
	List<DisplayOrderConstraint> displayOrderConstraints;
	
	public final List<DisplayOrderConstraint> getDisplayOrderConstraints() {
//		if (displayOrderConstraints == null) {
			displayOrderConstraints = buildDisplayOrderConstraints();
//		}
		return displayOrderConstraints;
	}

	public ArrayList<DisplayOrderConstraint> buildDisplayOrderConstraints() {
		return new ArrayList<>();
	}
}
