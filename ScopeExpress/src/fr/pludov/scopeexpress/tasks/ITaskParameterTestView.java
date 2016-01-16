package fr.pludov.scopeexpress.tasks;

import java.util.*;

/** Vue de paramètre pour une tache données, mais avant instanciation */
public interface ITaskParameterTestView extends ITaskParameterBaseView{
	@Override
	public <TYPE> TYPE get(TaskParameterId<TYPE> key) throws ParameterNotKnownException;
	@Override
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value);
	public void setUndecided(TaskParameterId<?> key);
	/** Est-ce que set (ou setUndecided) a déjà été appellé ? */
	public boolean hasValue(TaskParameterId<?> key);
	
	@Override
	public ITaskParameterTestView getSubTaskView(String taskLauncherDefinitionId);
	
	public boolean hasError();
	
	void addError(String error);
	void addError(TaskParameterId<?> tpi, String error);
	void addTopLevelError(String error);
	/** Retourne (efface) l'erreur associée à un champ */
	public String getFieldError(TaskParameterId<?> key);
	
	/** Retourne (efface) toutes les erreurs */
	public List<String> getAllErrors();
	
}
