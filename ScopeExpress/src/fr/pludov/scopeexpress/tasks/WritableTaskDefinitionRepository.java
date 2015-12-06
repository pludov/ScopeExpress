package fr.pludov.scopeexpress.tasks;

import java.util.*;

public class WritableTaskDefinitionRepository implements TaskDefinitionRepository {
	final TaskDefinitionRepository inherit;
	final Map<String, BaseTaskDefinition> definitions;

	public WritableTaskDefinitionRepository(TaskDefinitionRepository inherit) {
		this.inherit = inherit;
		definitions = new TreeMap<>();
	}

	@Override
	public BaseTaskDefinition getById(String taskDefinitionId) {
		BaseTaskDefinition result;
		synchronized(this) {
			result = definitions.get(taskDefinitionId);
		}
		if (result != null) return result;
		if (inherit != null) return inherit.getById(taskDefinitionId);
		return null;
	}
	

	void declare(BaseTaskDefinition baseTaskDefinition)
	{
		synchronized(this) {
			definitions.put(baseTaskDefinition.getId(), baseTaskDefinition);
		}
	}
	
	@Override
	public List<BaseTaskDefinition> list() {
		ArrayList<BaseTaskDefinition> result = new ArrayList<>();
		
		if (inherit != null) {
			result.addAll(inherit.list());
		}
		result.addAll(definitions.values());
		
		return result;
	}
}
