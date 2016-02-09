package fr.pludov.scopeexpress.tasks;

import java.util.*;

/** Vue de param�tre pour une tache donn�es, mais avant instanciation */
public interface ITaskParameterTestView extends ITaskParameterBaseView{
	@Override
	public <TYPE> TYPE get(TaskParameterId<TYPE> key) throws ParameterNotKnownException;
	
	public void setUndecided(TaskParameterId<?> key);
	/** Est-ce que set (ou setUndecided) a d�j� �t� appell� ? */
	public boolean hasValue(TaskParameterId<?> key);
	
	@Override
	public ITaskParameterTestView getSubTaskView(TaskLauncherDefinition taskLauncherDefinitionId);
	
	public boolean hasError();
	
	void addError(String error);
	void addError(TaskParameterId<?> tpi, String error);
	void addTopLevelError(String error);
	/** Retourne (efface) l'erreur associ�e � un champ */
	public String getFieldError(TaskParameterId<?> key);
	
	/** Retourne (efface) toutes les erreurs */
	public List<String> getAllErrors();
	
	
	/** Est-ce qu'on valide une vue sur la configuration ? */
	public boolean isConfiguration();
	
	public boolean needValidation();
	public boolean needValidation(Object enumValue);
	
	public void disableValidation();
	public void disableValidation(Object enumValue);
	
	
	public static final String focuserRequired = "Requiert un focuseur connect�";
	public static final String cameraRequired = "Requiert une cam�ra connect�e";
	public static final String filterWheelRequired = "Requiert une roue � filtre connect�e";
	public static final String autoGuiderRequired = "Requiert la connection PHD2";
}
