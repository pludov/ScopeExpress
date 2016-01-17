package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.utils.*;

public class TaskParameterPanel extends JPanel {

	
	final BaseTaskDefinition taskDef;
	final Requirement req;
	final FocusUi focusUi;
	
	// Les inputs requis
	class Requirement
	{
		// Pour cette définition
		BaseTaskDefinition forTask;
		// On a besoin de ces paramètres
		List<ParameterEditionContext> parameters;
		// Et dans ces sous-taches aussi, il y a des paramètres à demander
		Map<String, Requirement> childs;
		
		/** Lorsqu'on construit des input */
		IConfigurationDialog dialog;
		
		
		Requirement(BaseTaskDefinition ft, List<TaskLauncherOverride<?>> overriden)
		{
			this.forTask = ft;
			this.parameters = new ArrayList<>();
			
			ParamLoop: for(TaskParameterId<?> param : ft.getParameters())
			{
				TaskLauncherOverride<?> override = null;
				for(TaskLauncherOverride<?> tlo : overriden) {
					if (tlo.getParameter() == param) {
						override = tlo;
						break;
					}
				}
				if (display(param, override)) {
//				if (param.is(ParameterFlag.Input)) {
					// On le met, sauf si on a un override dans la tache
					
					parameters.add(new ParameterEditionContext(param));
				}
			}
			
			
			this.childs = new LinkedHashMap<>();
			for(TaskLauncherDefinition bt : ft.getSubTasks()) {
				Requirement child = new Requirement(bt.getStartedTask(), bt.getOverrides());
				
				if (!child.isEmpty()) {
					this.childs.put(bt.getId(), child);
				}
			}
		}
		
		boolean isEmpty() {
			return parameters.isEmpty() && childs.isEmpty();
		}
		
		private <A> void sanitizeParameter(ISafeTaskParameterView view, TaskParameterId<A> id,
				IParameterEditionContext paramCtxt) {
			A currentValue = view.get(id);
			A newvalue = id.sanitizeValue(focusUi, paramCtxt, currentValue);
			if (!Objects.equals(currentValue, newvalue)) {
				view.set(id, newvalue);
			}
		}

		/** Fait en sorte que les valeurs respectent les contraintes en cours */
		void sanitizeValues(ISafeTaskParameterView view) {
			for (IParameterEditionContext paramCtxt : parameters) {
				if (paramCtxt.isEditable()) {
					sanitizeParameter(view, paramCtxt.getParameter(), paramCtxt);
				}
			}
			for (Map.Entry<String, Requirement> child : childs.entrySet()) {
				child.getValue().sanitizeValues(view.getSubTaskView(child.getKey()));
			}
		}

		void ask(Container parent)
		{
			if (!parameters.isEmpty()) {
				dialog = forTask.parameterUi(focusUi, parameters);
				parent.add(dialog.getPanel());
			} else {
				dialog = null;
			}
			for(Map.Entry<String, Requirement> child : childs.entrySet()) {
				JPanel childPanel = new JPanel();
				childPanel.setLayout(new DialogLayout());
				childPanel.setBorder(new TitledBorder(child.getKey()));
				child.getValue().ask(childPanel);
				
				parent.add(childPanel);
			}
		}

		void setDialogValues(ISafeTaskParameterView view)
		{
			if (dialog != null) {
				dialog.setWidgetValues(view);
			}
			for(Map.Entry<String, Requirement> child : childs.entrySet()) {
				
				child.getValue().setDialogValues(view.getSubTaskView(child.getKey()));
			}
		}

		void getDialogValues(ITaskParameterBaseView view)
		{
			if (dialog != null) {
				dialog.loadWidgetValues(view);
			}
			for(Map.Entry<String, Requirement> child : childs.entrySet()) {
				
				child.getValue().getDialogValues(view.getSubTaskView(child.getKey()));
			}
		}
		
		boolean dialogHasError() {
			if (dialog != null && dialog.hasError()) {
				return true;
			}
			for (Map.Entry<String, Requirement> child : childs.entrySet()) {

				if (child.getValue().dialogHasError()) {
					return true;
				}
			}
			return false;
		}

		
		// Affiche les erreurs (et les retire de testView)
		void dispatchErrors(ITaskParameterTestView testView)
		{
			for(Map.Entry<String, Requirement> child : childs.entrySet())
			{
				child.getValue().dispatchErrors(testView.getSubTaskView(child.getKey()));
			}
			if (dialog != null) {
				dialog.setLogicErrors(testView);
			}
		}
		
		
		boolean hasRootError() {
			ITaskParameterTestView testView = new TaskParameterTestView(
					focusUi.getApplication().getConfigurationTaskValues(), focusUi.getApplication().getConfigurationTaskValues().getSubTaskView(forTask.getId()),
					focusUi.getApplication().getLastUsedTaskValues(), focusUi.getApplication().getLastUsedTaskValues().getSubTaskView(forTask.getId())
				);
			getDialogValues(testView);
			
			forTask.validateSettings(focusUi, testView);
			// Il faut les prendre maintenant car le dispatch va les effacer
			boolean result = testView.hasError();
			//dialog.setLogicErrors(testView);
			dispatchErrors(testView);
			
			return result;
		}
		
		void loadDefault(ITaskParameterView itp, ITaskParameterView config, ITaskOptionalParameterView lastUsed)
		{
			for(IParameterEditionContext desc : parameters) {
				TaskParameterId<?> id = desc.getParameter();
			
				Object value;
				if (id.is(ParameterFlag.PresentInConfig)) {
					value = config.get(id);
				} else if ((!id.is(ParameterFlag.DoNotPresentLasValue)) && lastUsed.has(id)) {
					value = lastUsed.get(id);
				} else {
					value = id.getDefault();
				}
				// Bouh le vilain cast
				itp.set((TaskParameterId)id, value);
			}
			for(Map.Entry<String, Requirement> child : childs.entrySet()) {
				
				child.getValue().loadDefault(itp.getSubTaskView(child.getKey()), 
								  config.getSubTaskView(child.getKey()),
								  lastUsed.getSubTaskView(child.getKey()));
			}
		}
		
		/**
		 * A la fin d'une édition, enregistre les valeurs par défaut
		 */
		void saveDefault(ISafeTaskParameterView values, ITaskOptionalParameterView defaults)
		{
			for(IParameterEditionContext desc : parameters) {
				TaskParameterId<?> id = desc.getParameter();
			
				if (id.is(ParameterFlag.PresentInConfig)) {
					continue;
				} else if (id.is(ParameterFlag.DoNotPresentLasValue)){
					continue;
				}
				if (!desc.isEditable()) {
					continue;
				}
				
				Object value = values.get((TaskParameterId)id);
				// Bouh le vilain cast
				Object current = defaults.get((TaskParameterId)id);
				if (!Objects.equals(value, current)) {
					defaults.set((TaskParameterId)id, value);
				}
			}
			for(Map.Entry<String, Requirement> child : childs.entrySet()) {
				
				child.getValue().saveDefault(
											values.getSubTaskView(child.getKey()), 
								  			defaults.getSubTaskView(child.getKey()));
			}
		}
		
		void makeEditable()
		{
			// Tous les paramètres deviennent éditables
			for(ParameterEditionContext p : parameters) {
				p.setEditable(true);
			}
			
			for(Map.Entry<String, Requirement> child : childs.entrySet()) {
				child.getValue().makeEditable();
			}
		}
	}
	
	
	public TaskParameterPanel(final FocusUi focusUi, final BaseTaskDefinition taskDef) {
		
		this.focusUi = focusUi;
		this.taskDef = taskDef;
		req = new Requirement(taskDef, Collections.<TaskLauncherOverride<?>> emptyList());

		setLayout(new DialogLayout());

	}
	
	public boolean display(TaskParameterId<?> param, TaskLauncherOverride<?> override)
	{
		return override == null && param.is(ParameterFlag.Input);
	}
	
	public void showStartDialog(Window parent) {
		final ITaskParameterView view = new TaskParameterView(
				focusUi.getApplication().getConfigurationTaskValues(),
				focusUi.getApplication().getConfigurationTaskValues().getSubTaskView(taskDef.getId()),
				focusUi.getApplication().getLastUsedTaskValues(),
				focusUi.getApplication().getLastUsedTaskValues().getSubTaskView(taskDef.getId())
				);
		req.makeEditable();
		
		// If req... On crée un dialogue...
		if (!req.isEmpty()) {
			final JDialog jd = new JDialog(parent);
			jd.setTitle(taskDef.getTitle());
			jd.setModal(false);
			jd.getContentPane().setLayout(new BorderLayout());
			req.ask(this);
			// panel.add(Box.createVerticalGlue());
			req.sanitizeValues(view);
			req.setDialogValues(view);
			
			Utils.addDialogButton(jd, new Runnable() {

				@Override
				public void run() {
					if ((!req.dialogHasError())&&(!req.hasRootError())) {
						req.getDialogValues(view);
						req.saveDefault(view,
								focusUi.getApplication().getLastUsedTaskValues().getSubTaskView(taskDef.getId()));
						jd.setVisible(false);

						BaseTask task = focusUi.getApplication().getTaskManager().startTask(focusUi, taskDef, view);
					}
				}
			});
			jd.getContentPane().add(this, BorderLayout.CENTER);
			jd.pack();
			if (parent != null) {
				jd.setLocationRelativeTo(parent);
			}
			jd.setVisible(true);
		} else {
			req.saveDefault(view, focusUi.getApplication().getLastUsedTaskValues().getSubTaskView(taskDef.getId()));
			BaseTask task = focusUi.getApplication().getTaskManager().startTask(focusUi, taskDef, view);			
		}
	}
	
	public void loadAndEdit(ISafeTaskParameterView view)
	{
		req.makeEditable();
		req.ask(this);
		req.setDialogValues(view);
	}
	
	public void editCurrentParameter(Window parent, BaseTask bt)
	{
		final ITaskParameterView currentView = bt.getParameters();
		if (!req.isEmpty()) {
			final JDialog jd = new JDialog(parent);
			jd.setModal(false);
			jd.getContentPane().setLayout(new BorderLayout());

			req.ask(this);
			req.setDialogValues(currentView);;
			
			Utils.addDialogButton(jd, new Runnable() {

				@Override
				public void run() {
					// FIXME: update des paramètres ?
					if ((!req.dialogHasError())&&(!req.hasRootError())) {
						req.getDialogValues(currentView);
						jd.setVisible(false);
					}
				}
			});
			
			jd.getContentPane().add(this, BorderLayout.CENTER);
			jd.pack();
			jd.setVisible(true);
		}
	}
	
}
