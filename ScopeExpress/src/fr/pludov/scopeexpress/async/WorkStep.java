package fr.pludov.scopeexpress.async;

import java.util.List;

public interface WorkStep {
	// Est lancé une fois que les resources sont toutes lockées
	void proceed();
	
	// Proceed ne sera appellé que lorsque readyToProceed sera vrai
	// Doit être thread safe
	boolean readyToProceed();

	// proceed ne sera appellé que quand toutes les resources auront été lockées
	List<WorkStepResource> getRequiredResources();

}
