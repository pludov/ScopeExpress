package fr.pludov.scopeexpress.tasks;

import java.util.List;

public interface TaskDefinitionRepository {
	BaseTaskDefinition getById(String taskDefinitionId);
	List<BaseTaskDefinition> list();
}
