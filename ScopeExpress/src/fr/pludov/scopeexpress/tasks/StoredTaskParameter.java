package fr.pludov.scopeexpress.tasks;

import java.util.*;

import fr.pludov.scopeexpress.database.content.*;

public class StoredTaskParameter implements ITaskOptionalParameterView {

	final TaskConfigMap storage;
	final String prefix;
	
	public StoredTaskParameter(TaskConfigMap storage, String prefix) {
		this.storage = storage;
		this.prefix = prefix;
	}

	@Override
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value) {
		boolean hadKey = storage.getContent().containsKey(key);
		Object previous = storage.getContent().put(prefix + key.id, value);
		if ((!hadKey) || (!Objects.equals(previous, value))) {
			storage.getContainer().asyncSave();
		}
	
	}

	@Override
	public boolean has(TaskParameterId<?> t) {
		return storage.getContent().containsKey(prefix + t.id);
	}

	@Override
	public void remove(TaskParameterId<?> t) {
		if (storage.getContent().containsKey(prefix + t.id)) {
			storage.getContent().remove(prefix + t.id);
			storage.getContainer().asyncSave();
		}

	}

	@Override
	public <TYPE> TYPE get(TaskParameterId<TYPE> key) {
		if (!has(key)) {
			return key.defaultValue;
		}
		return (TYPE)storage.getContent().get(prefix + key.id);
	}

	@Override
	public ITaskOptionalParameterView getSubTaskView(TaskLauncherDefinition taskLauncherDefinitionId) {
		return getViewById(taskLauncherDefinitionId.getId());
	}

	StoredTaskParameter getViewById(String id) {
		return new StoredTaskParameter(storage, prefix + id + ".");
	}
	
	public IRootParameterView<StoredTaskParameter> getRootParameterView()
	{
		return new IRootParameterView<StoredTaskParameter>() {
			@Override
			public StoredTaskParameter getTaskView(BaseTaskDefinition btd) {
				return getViewById(btd.getId());
			}
		};
	}
	
}
