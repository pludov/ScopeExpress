package fr.pludov.scopeexpress.tasks;

import java.util.*;

import fr.pludov.scopeexpress.ui.DefaultTaskView;
import fr.pludov.scopeexpress.ui.FocusUi;

/** Conserve la définition de chaque paramètre, et comment les présenter à l'utilisateur */
public abstract class BaseTaskDefinition {
	private final String id;
	final String title;
	final Map<String, TaskParameterId<?>> parameters;
	final Map<String, TaskLauncherDefinition> taskLaunchers;
	final TaskDefinitionRepository repository;
	
	public BaseTaskDefinition(WritableTaskDefinitionRepository repository, String id, String defaultTitle) {
		this.repository = repository;
		this.id = id;
		this.title = defaultTitle;
		this.taskLaunchers = new LinkedHashMap<>();
		this.parameters = new LinkedHashMap<>();
		
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
	
	public Collection<String> getChildIds()
	{
		List<String> t = new ArrayList<>(parameters.keySet());
		t.addAll(taskLaunchers.keySet());
		return t;
	}
	
	public IConfigurationDialog parameterUi(List<? extends IParameterEditionContext> required)
	{
		ComposedConfigurationDialog ccd = new ComposedConfigurationDialog();
		for(IParameterEditionContext paramCtxt : required)
		{
			IFieldDialog<?> ifd = paramCtxt.getParameter().buildDialog(paramCtxt);
			if (ifd instanceof SimpleFieldDialog<?>) {
				ccd.add((SimpleFieldDialog<?>) ifd);
			}
		}
		return ccd;
	}

	public String getTitle() {
		return title;
	}
	
	public TaskDefinitionRepository getRepository()
	{
		return repository;
	}
	
	protected static WritableTaskDefinitionRepository getBuiltinRepository()
	{
		return (WritableTaskDefinitionRepository) BuiltinTaskDefinitionRepository.getInstance();
	}
	
	@Override
	public String toString() {
		return getTitle();
	}

	public String getId() {
		return id;
	}
}
