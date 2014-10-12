package fr.pludov.scopeexpress.async;

import java.util.List;

public interface WorkStep {
	// Est lanc� une fois que les resources sont toutes lock�es
	void proceed();
	
	// Proceed ne sera appell� que lorsque readyToProceed sera vrai
	// Doit �tre thread safe
	boolean readyToProceed();

	// proceed ne sera appell� que quand toutes les resources auront �t� lock�es
	List<WorkStepResource> getRequiredResources();

}
