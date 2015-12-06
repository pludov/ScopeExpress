package fr.pludov.scopeexpress.tasks;

import fr.pludov.scopeexpress.camera.ShootParameters;
import fr.pludov.scopeexpress.tasks.autofocus.TaskAutoFocusDefinition;
import fr.pludov.scopeexpress.tasks.shoot.TaskShootDefinition;

public class BuiltinTaskDefinitionRepository extends WritableTaskDefinitionRepository {

	public BuiltinTaskDefinitionRepository(TaskDefinitionRepository inherit) {
		super(inherit);
	}

	private static BuiltinTaskDefinitionRepository instance;
	
	public static TaskDefinitionRepository getInstance() {
		synchronized(BuiltinTaskDefinitionRepository.class) {
			if (instance == null) {
				instance = new BuiltinTaskDefinitionRepository(null);
				TaskShootDefinition.getInstance();
				TaskAutoFocusDefinition.getInstance();
			}
			return instance;
		}

	}
}
