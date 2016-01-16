package fr.pludov.scopeexpress.tasks;

import javax.swing.*;

public interface IConfigurationDialog {
	public JPanel getPanel();
	
	/** Positionne la valeur dans widget d'édition. */
	<T> void set(TaskParameterId<T> parameter, T value);
	
	/** Fourni la valeur actuelle du widget d'edition (non utilisable si hasError) */
	<T> T get(TaskParameterId<T> parameter);
	
	/** Positionne tous les widget d'édition à partir des valeurs données */
	void setWidgetValues(ISafeTaskParameterView view);

	/** Reporte la valeur de tous les widget d'édition vers la vue donnée*/
	void loadWidgetValues(ITaskParameterBaseView view);
	
	/** Est-ce que les valeurs sont dispo pour tous les champs ? */
	boolean hasError();
	
	/** Une fois que chaque éditeur est content, on précise des erreurs de logique
	 * Les erreurs des taches filles doivent aussi être montrées
	 */
	public void setLogicErrors(ITaskParameterTestView testView);
}

