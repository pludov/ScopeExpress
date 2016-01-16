package fr.pludov.scopeexpress.tasks;

import javax.swing.*;

public interface IConfigurationDialog {
	public JPanel getPanel();
	
	/** Positionne la valeur dans widget d'�dition. */
	<T> void set(TaskParameterId<T> parameter, T value);
	
	/** Fourni la valeur actuelle du widget d'edition (non utilisable si hasError) */
	<T> T get(TaskParameterId<T> parameter);
	
	/** Positionne tous les widget d'�dition � partir des valeurs donn�es */
	void setWidgetValues(ISafeTaskParameterView view);

	/** Reporte la valeur de tous les widget d'�dition vers la vue donn�e*/
	void loadWidgetValues(ITaskParameterBaseView view);
	
	/** Est-ce que les valeurs sont dispo pour tous les champs ? */
	boolean hasError();
	
	/** Une fois que chaque �diteur est content, on pr�cise des erreurs de logique
	 * Les erreurs des taches filles doivent aussi �tre montr�es
	 */
	public void setLogicErrors(ITaskParameterTestView testView);
}

