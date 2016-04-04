package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.TaskFieldStatus.*;
import fr.pludov.scopeexpress.ui.utils.*;

/**
 * Lit/écrit les valeurs dans des widget.
 * 
 * L'algorithme est le suivant:
 *   Pour chaque item, faire un get.
 *   Le get évalue le status (eventuellement récursif)
 *     - on note alors les dépendances
 *   Si le status est visible (et était visible):
 *     * on lit la valeur depuis le widget
 *     * on valide la valeur
 *     * si la valeur n'est pas valide, on utilise la valeur par défaut (et on note l'erreur)
 *   Si le status est invisible, on utilise la valeur par défaut 
 */

public class TaskParameterPanel extends JPanel implements Scrollable {
	final static Object undecided = new Object();

	final BaseTaskDefinition root;
	final FocusUi focusUi;

	final Map<ParameterPath<?>, ParameterStatus<?>> fields;
	final IRootParameterView<? extends ISafeTaskParameterView> rootConfig/*, config*/;
	final IRootParameterView<? extends ITaskOptionalParameterView> rootPreviousValues/*, previousValues*/;
	
	final LinkedHashSet<ParameterStatus<?>> todoList = new LinkedHashSet<>();

	DialogButtonManager currentDialog;
	boolean hasError;
	
	class ParameterStatus<T>
	{
		ParameterPath<T> parameter;
		IFieldDialog<T> dialog;
		T lastDialogValue;
		
		final List<TaskFieldControler<T>> fieldControlers;
		/** Champs influencé (controlés) */
		final Set<ParameterPath<?>> controled;

		// Est-ce que le status du dialogue doit être raffraichi ?
		private boolean torefresh;
		
		// Est-ce que le champ doit être lu depuis la forme ?
		private boolean needParsing;
		// Est-ce que le dialog est visible ?
//		boolean dialogVisible;
		
		boolean evaluating;
		
		// Au départ, tout le monde doit être meaningless
		TaskFieldStatus<T> status;
		
		// Dernière valeur connue ?
		boolean hasValue;
		Object value;
		boolean isNotKnown;
		
		
		// Ignorer les notifications de change du dialogue
		int ignoreChange;
		
		ParameterStatus() {
			fieldControlers = new ArrayList<>();
			controled = new HashSet<>();
		}
		
		boolean isTodo()
		{
			return isTorefresh() || isNeedParsing();
		}
		
		private void updateTodo(boolean wasTodo)
		{
			if (isTodo() == wasTodo) {
				return;
			}
			if (wasTodo) {
				todoList.remove(this);
			} else {
				todoList.add(this);
			}
			
		}
		
		void setTorefresh(boolean val)
		{
			if (torefresh == val) {
				return;
			}
			
			boolean wasTodo = isTodo();
			torefresh = val;
			updateTodo(wasTodo);
		}
		
		void setNeedParsing(boolean val)
		{
			if (needParsing == val) {
				return;
			}
			boolean wasTodo = isTodo();
			needParsing = val;
			updateTodo(wasTodo);
		}

		boolean isTorefresh() {
			return torefresh;
		}

		boolean isNeedParsing() {
			return needParsing;
		}
		
	}
	
	/** 
	 * Vue sur un niveau particulier 
	 * Retourne par défaut la valeur de la forme
	 */
	class TaskParameterTestView implements ITaskParameterTestView
	{
		final SubTaskPath path;
		final BaseTaskDefinition taskDef;
		final Map<TaskParameterId<?>, Object> values;
		final Map<String, TaskParameterTestView> childs;
		
		final boolean configuration;
		boolean disabled;
		Set<Object> disabledChecking;

		LinkedHashSet<String> globalErrors;
		LinkedHashSet<String> topLevelErrors;
		final Map<TaskParameterId<?>, String> fieldErrors;
		
		
		TaskParameterTestView(boolean configuration)
		{
			this(null, null, configuration);
		}
		
		TaskParameterTestView(TaskParameterTestView parent, TaskLauncherDefinition child)
		{
			this(parent, child, false);
		}
		
		private TaskParameterTestView(TaskParameterTestView parent, TaskLauncherDefinition child, boolean configuration)
		{
			path = parent != null ? parent.path.forChild(child) : new SubTaskPath();
			this.configuration = parent != null ? parent.configuration : configuration;
			taskDef = parent == null ? root : child.getStartedTask();
			values = new HashMap<>();
			childs = new HashMap<>();
			globalErrors = new LinkedHashSet<>();
			topLevelErrors = parent != null ? parent.topLevelErrors : new LinkedHashSet<String>();
			fieldErrors = new LinkedHashMap<>();
			disabled = false;
			disabledChecking = null;
		}
		
		@Override
		public <TYPE> TYPE get(TaskParameterId<TYPE> key) throws ParameterNotKnownException {
			if (values.containsKey(key)) {
				Object o = values.get(key);
				if (o == undecided) {
					throw new ParameterNotKnownException();
				}
				return (TYPE)o;
			} else {
				return (TYPE)getParameterValue(path.forParameter(key));
			}
		}

		@Override
		public void setUndecided(TaskParameterId<?> key) {
			values.put(key, undecided);		
		}

		//@Override
		public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value) {
			values.put(key, value);
		}

		@Override
		public boolean hasValue(TaskParameterId<?> key) {
			// FIXME: ça doit dégager !
			return true;
		}

		@Override
		public TaskParameterTestView getSubTaskView(TaskLauncherDefinition taskLauncherDefinitionId) {
			TaskParameterTestView  result = childs.get(taskLauncherDefinitionId.getId());
			if (result == null) {
				result = new TaskParameterTestView(this, (TaskLauncherDefinition) taskDef.getChildById(taskLauncherDefinitionId.getId()));
				childs.put(taskLauncherDefinitionId.getId(), result);
			}
			return result;
		}

		@Override
		public void addTopLevelError(String error) {
			this.topLevelErrors.add(error);
		}
		
		@Override
		public void addError(String error) {
			this.globalErrors.add(error);
		}
		
		@Override
		public void addError(TaskParameterId<?> tpi, String error) {
			String previous = fieldErrors.put(tpi, error);
			if (previous != null) {
				fieldErrors.put(tpi, previous);
			}
		}
		
		@Override
		public boolean hasError() {
			if (!globalErrors.isEmpty()) return true;
			if (!fieldErrors.isEmpty()) return true;
			for(TaskParameterTestView tptv : childs.values()) {
				if (tptv.hasError()) return true;
			}
			return false;
		}
		
		@Override
		public String getFieldError(TaskParameterId<?> key) {
			return fieldErrors.remove(key);
		}
		
		@Override
		public List<String> getAllErrors() {
			List<String> result = new ArrayList<>();
			result.addAll(globalErrors);
			
			for(Map.Entry<TaskParameterId<?>, String> error : this.fieldErrors.entrySet())
			{
				String title = error.getKey().getTitle();
				if (title == null) {
					title = error.getKey().getId();
				}
				result.add(title + ": " + error.getValue());
			}
			this.fieldErrors.clear();

			for(Map.Entry<String, TaskParameterTestView> entry : this.childs.entrySet())
			{
				for(String error : entry.getValue().getAllErrors())
				{
					result.add(entry.getKey() + ": " + error);
				}
			}
			
			return result;
		}
		
		public List<String> getTopLevelErrors()
		{
			List<String> result = new ArrayList<>(topLevelErrors);
			topLevelErrors.clear();
			return result;
		}

		public String getFieldError(ParameterPath<?> parameter) {
			ITaskParameterTestView current = this;
			for(int i = 0; i < parameter.taskPath.getLength(); ++i)
			{
				TaskLauncherDefinition dir = parameter.taskPath.getElement(i);
				current = current.getSubTaskView(dir);
			}
			return current.getFieldError(parameter.parameter);
		}

		public List<String> getAllErrors(SubTaskPath path) {
			TaskParameterTestView current = this;
			for(int i = 0; i < path.getLength(); ++i) {
				current = current.getSubTaskView(path.getElement(i));
			}
			return current.getAllErrors();
		}

		public boolean hasTopLevelErrors() {
			return !topLevelErrors.isEmpty();
		}
		
		@Override
		public void disableValidation() {
			this.disabled = true;
			this.disabledChecking = null;
		}
		
		@Override
		public void disableValidation(Object enumValue) {
			if (this.disabled) return;
			if (this.disabledChecking == null) {
				this.disabledChecking = new HashSet<>();
			}
			this.disabledChecking = new HashSet<>();
		}
		
		@Override
		public boolean needValidation() {
			return !disabled;
		}
		
		@Override
		public boolean needValidation(Object enumValue) {
			if (disabled) {
				return false;
			}
			if (this.disabledChecking != null && this.disabledChecking.contains(enumValue)) {
				return false;
			}
			return true;
		}

		@Override
		public boolean isConfiguration() {
			return TaskParameterPanel.this.isForConfiguration;
		}
	}
	

	public boolean display(TaskParameterId<?> param, TaskLauncherOverride<?> override)
	{
		return override == null && param.is(ParameterFlag.Input);
	}

	Map<ParameterPath<?>, TaskLauncherOverride<?>> overrides;
	
	private void findOverrides(SubTaskPath from, BaseTaskDefinition btd)
	{
		overrides = new HashMap<>();
		for(TaskLauncherDefinition bt : btd.getSubTasks()) {
			if (!bt.getOverrides().isEmpty()) {
				for(TaskLauncherOverride<?> tlo : bt.getOverrides())
				{
					TaskLauncherOverride<?> previous = overrides.put(from.forChild(bt).forParameter(tlo.getParameter()), tlo);
					if (previous != null) {
						overrides.put(from.forChild(bt).forParameter(tlo.getParameter()), previous);
					}
				}
			}
		}

	}
	
	private <T> void createOneParameter(SubTaskPath from, TaskParameterId<T> param)
	{
		ParameterPath<T> paramPath = from.forParameter(param);
		if (display(param, overrides.get(paramPath))) {
			// Il faut créer le dialogue.
			final ParameterStatus<T> paramStatus = new ParameterStatus<>();
			
			IFieldDialog<T> dialog = param.buildDialog(focusUi);
			dialog.addListener(new Runnable() {
				@Override
				public void run() {
					System.out.println("Change for " + paramStatus.parameter);
					if (paramStatus.ignoreChange > 0) return;
					System.out.println("Calling update...");
					paramStatus.setNeedParsing(true);
					update();
				}
				
			});
			paramStatus.parameter = paramPath;
			paramStatus.dialog = dialog;
			paramStatus.setNeedParsing(true);
			paramStatus.evaluating = false;
			paramStatus.setTorefresh(true);
			
			paramStatus.status = new TaskFieldStatus<>(Status.MeaningLess);
			paramStatus.hasValue = false;
			paramStatus.dialog.adapt(paramStatus.status);
			
			fields.put(paramPath, paramStatus);
			
			add(dialog.getPanel());
		}
		
	}
	
	private void createParameters(SubTaskPath path)
	{
		// Parcours la structure à la recherche des paramètres...
		BaseTaskDefinition taskOfLevel = path.getLength() == 0 ? root : path.lastElement();
		
		for(TaskParameterId<?> param : taskOfLevel.getParameters())
		{
			createOneParameter(path, param);
		}
		
		for(TaskLauncherDefinition child : taskOfLevel.getSubTasks())
		{
			createParameters(path.forChild(child));
		}
	}
	
	
	
	class SubTaskView {
		SubTaskPath viewPath;
		public <TYPE> TYPE get(TaskParameterId<TYPE> key) throws ParameterNotKnownException
		{
			return (TYPE)getParameterValue(viewPath.forParameter(key));
		}
	}

	private void removeControlersFor(ParameterPath<?> pp)
	{
		for(ParameterStatus<?> ps : fields.values())
		{
			ps.controled.remove(pp);
		}
	}
	
	private void triggerControled(ParameterStatus<?> ps)
	{
		// Trouver les dépendances à l'envers
		for(ParameterPath<?> controledPath : ps.controled)
		{
			ParameterStatus<?> controled = fields.get(controledPath);
			if (controled != null) {
				System.out.println("propagating change to " + controled.parameter);
				controled.setTorefresh(true);
			}
			
		}
	}
	
	ISafeTaskParameterView getConfigFor(SubTaskPath path)
	{
		ISafeTaskParameterView result = rootConfig.getTaskView(root);
		for(int i = 0; i < path.getLength(); ++i) {
			TaskLauncherDefinition tld = path.getElement(i);
			result = result.getSubTaskView(tld);
		}
		return result;
	}
	

	ITaskOptionalParameterView getPreviousValuesFor(SubTaskPath path)
	{
		ITaskOptionalParameterView result = rootPreviousValues.getTaskView(root);
		for(int i = 0; i < path.getLength(); ++i) {
			TaskLauncherDefinition tld = path.getElement(i);
			result = result.getSubTaskView(tld);
		}
		return result;
	}
	
	
	
	
	<T> T getDefault(ParameterPath<T> pp)
	{
		
		TaskParameterId<T> key = pp.parameter;
		
		if (modification != null) {
			ITaskParameterView previousView = pp.getTaskPath().resolve(modification);
			return previousView.get(key);
		} else {
			if (rootConfig != null && key.getFlags().contains(ParameterFlag.PresentInConfig)) {
				// Aller chercher en conf de toute façon
				ISafeTaskParameterView configForTask = rootConfig.getTaskView(key.getTaskDefinition());
				return configForTask.get(key);
			}
	
			if (rootConfig != null && key.getFlags().contains(ParameterFlag.PresentInConfigForEachUsage)) {
				return getConfigFor(pp.taskPath).get(key);
			}
			
			if (rootPreviousValues != null && !key.getFlags().contains(ParameterFlag.DoNotPresentLasValue)) {
				ITaskOptionalParameterView viewForLevel = getPreviousValuesFor(pp.taskPath);
				if (viewForLevel.has(key)) {
					return getPreviousValuesFor(pp.taskPath).get(key);
				}
			}
	
			return key.getDefault();
		}
	}
	
	List<ParameterStatus<?>> evaluationStack = new ArrayList<>();
	
	public <T> T getParameterValue(ParameterPath<T> pp) throws ParameterNotKnownException
	{
		ParameterStatus<T> ps = (ParameterStatus<T>) fields.get(pp);
		if (ps == null) {
			if (isForConfiguration) {
				throw new ParameterNotKnownException();
			}
			return getDefault(pp);
		}
		
		return getParameterValueFromStatus(ps);
	}
	
	private <T> T getParameterValueFromStatus(ParameterStatus<T> ps) throws ParameterNotKnownException
	{
		if (ps.evaluating) {
			throw new RuntimeException("Recursive dependencie found for " + ps.parameter);
		}
		ParameterPath<T> pp = ps.parameter;
		T result;
		boolean isNotKnown = false;
		evaluationStack.add(ps);
		ps.evaluating = true;
		try {
			if (evaluationStack.size() > 1) {
				ParameterStatus<?> controled = evaluationStack.get(evaluationStack.size() - 2);
				ps.controled.add(controled.parameter);
				//controled.dependencies.add(pp);
				
			}
			if (ps.isTorefresh()) {
				System.out.println("Refreshing: " + ps.parameter);
				// Effacer les dépendance du champ
				removeControlersFor(pp);
				
				TaskFieldStatus<T> previousStatus = ps.status;
				
				// Maintenant, en mettre
				if (ps.dialog != null && !ps.fieldControlers.isEmpty()) {
					TaskFieldControlerChain<T> t = new TaskFieldControlerChain<>(ps.fieldControlers);
					
					ps.status = t.getFieldStatus(t);// (TaskFieldStatus) ps.fieldControlers.get(0).getFieldStatus(null);
				} else {
					ps.status = new TaskFieldStatus<>(Status.Visible);
				}
				
				boolean statusChanged = !previousStatus.equals(ps.status);
				
				if (statusChanged) {
					ps.dialog.adapt(ps.status);
					
					// En cas d'apparition, on affiche la valeur
					if (ps.status.isEditable() && !previousStatus.isEditable()) {
						// Sanitizer...
						T value = getDefault(pp);
						if (modification == null) {
							value = pp.getParameter().sanitizeValue(focusUi, value);
						}
						ps.ignoreChange++;
						try {
							ps.dialog.set(value);
						} finally {
							ps.ignoreChange--;
						}
					} else if (ps.status.isVisible() && !previousStatus.isVisible()) {
						// Forcement pas éditable avant
						T value = getDefault(pp);
						if (modification == null && ps.status.isEditable()) {
							value = pp.getParameter().sanitizeValue(focusUi, value);
						}
						ps.dialog.set(value);
					}
					
					TaskParameterGroup container = ps.dialog.getContainer();
					if (container != null) {
						container.statusUpdated(ps.dialog, ps.status);
					}
					
					ps.setNeedParsing(ps.isNeedParsing() | ps.status.isEditable());
				}
				ps.setTorefresh(false);
			}
			
			
			switch(ps.status.status) {
			case Forced:
				result = ps.status.forcedValue;
				isNotKnown = false;
				ps.setNeedParsing(false);
				break;
			case MeaningLess:
				isNotKnown = true;
				result = null;
				ps.setNeedParsing(false);
				break;
			case Visible:
				isNotKnown = false;
				if (ps.isNeedParsing()) {
					// FIXME: et en cas d'erreur ?
					result = ps.dialog.get();
					ps.lastDialogValue = result;
					ps.setNeedParsing(false);
				} else {
					result = ps.lastDialogValue;
				}
				break;
			default:
				throw new RuntimeException("invalid status");
			}

			if ((!ps.hasValue) || (!Objects.equals(ps.value, result)) || (isNotKnown != ps.isNotKnown)) {
				System.out.println("Change detected for " + ps.parameter);
				ps.hasValue = true;
				ps.value = result;
				ps.isNotKnown = isNotKnown;
				triggerControled(ps);
			}
			
			if (isNotKnown) {
				throw new ParameterNotKnownException();
			}
			return result;
		} finally {
			ps.evaluating = false;
			evaluationStack.remove(evaluationStack.size() - 1);
		}
	}
	
	void update()
	{
		// Tous ceux qui ne sont pas done doivent être réévalué.
		while(!todoList.isEmpty())
		{
			try {
				getParameterValueFromStatus(todoList.iterator().next());
			} catch (ParameterNotKnownException e) {
			}
		}
		
		// Reporter les erreurs...
		TaskParameterTestView taskView = new TaskParameterTestView(isForConfiguration);
		this.root.validateSettings(focusUi, taskView);
		hasError = taskView.hasError() || taskView.hasTopLevelErrors();
		for(ParameterStatus<?> field : fields.values())
		{
			if (field.status.isVisible()) {
				// Lire une erreur éventuelle
				String error = taskView.getFieldError(field.parameter);
				field.dialog.setLogicError(error);
				
				if (field.dialog.hasError()) {
					hasError = true;
				}
			}
		}
		
		rootContainer.loadErrors(taskView);
		
		if (currentDialog != null) {
			currentDialog.getOkButton().setEnabled(!hasError);
		}
	}

	void layoutFieldsOfTask(SubTaskPath taskPath, TaskParameterGroup ancestor)
	{
		BaseTaskDefinition taskDef = taskPath.getLength() == 0 ? root : taskPath.lastElement();
		
		for(TaskParameterId<?> tpi : taskDef.getParameters())
		{
			layoutFieldOfTask(taskPath, ancestor, tpi);
		}
		
		for(TaskLauncherDefinition child : taskDef.getSubTasks())
		{
			TaskParameterGroup childPanel = new TaskParameterGroup(taskPath.forChild(child), child.getTitle(), false);
			ancestor.add(childPanel);
			layoutFieldsOfTask(taskPath.forChild(child), childPanel);
		}
		
	}

	private <T> void layoutFieldOfTask(SubTaskPath taskPath, TaskParameterGroup ancestor, TaskParameterId<T> tpi) {
		ParameterStatus<T> ps = (ParameterStatus<T>) fields.get(taskPath.forParameter(tpi));
		if (ps == null)
			return;
		
		ancestor.add(ps.dialog, ps.status);
	}
	
	TaskParameterGroup rootContainer;

	private final boolean isForConfiguration;

	// Quand on est en modif, vue des paramètres
	private ITaskParameterView modification;
	
	void layoutFields()
	{
		rootContainer = new TaskParameterGroup(new SubTaskPath(), null, true);
		add(rootContainer.panel);
		
		// Ajouter au niveau racine tous les elements de BaseTask
		// Il faut qu'une taskDefinition puisse amener une sous-tache dans un parent
		// Ou un champs dans un fils (et où ?)
		layoutFieldsOfTask(new SubTaskPath(), rootContainer);
	}
	
	void init()
	{
		findOverrides(new SubTaskPath(), root);
		createParameters(new SubTaskPath());
		layoutFields();
		root.declareControlers(this, new SubTaskPath());
		update();
	}
	
	public TaskParameterPanel(final FocusUi focusUi, BaseTaskDefinition btd, boolean isForConfiguration) {
		this.root = btd;
		this.focusUi = focusUi;
		fields = new HashMap<>();
		
		this.rootConfig = focusUi.getApplication().getConfigurationTaskValues();
		this.rootPreviousValues = focusUi.getApplication().getLastUsedTaskValues();
		// final ITaskOptionalParameterView rootPreviousValues, previousValues;
		this.isForConfiguration = isForConfiguration;
		
		setLayout(new DialogLayout());
	}
	
	/** Dialog => into. */
	public boolean loadDialogValues(IWritableTaskParameterBaseView into, boolean saveDefaults)
	{
		for(ParameterStatus<?> pt : this.fields.values())
		{
			if (!pt.status.isEditable()) {
				continue;
			}
			if (pt.dialog.hasError()) {
				return false;
			}
			if (pt.needParsing) {
				throw new RuntimeException("data not ready");
			}
			Object value = pt.lastDialogValue;
			ITaskParameterView itv;
			ISafeTaskParameterView t;
			IWritableTaskParameterBaseView target = pt.parameter.getTaskPath().resolve(into);
			target.set((TaskParameterId)pt.parameter.getParameter(), value);
			
			
			if (saveDefaults && pt.parameter.getParameter().requireLastValueSave()) {
				IWritableTaskParameterBaseView previousValuesTarget = this.rootPreviousValues.getTaskView(root);
				previousValuesTarget = pt.parameter.getTaskPath().resolve(previousValuesTarget);
				previousValuesTarget.set((TaskParameterId)pt.parameter.getParameter(), value);
			}
		}
		return true;
	}
	
	boolean hasVisibleField()
	{
		for(ParameterStatus<?> ps : fields.values())
		{
			if (ps.status.isVisible()) return true;
		}
		return false;
	}
	

	/** Edition d'une tache en cours/finie */
	public void showEditDialog(Window parent, final BaseTask currentTask) {
		init();
		
		final JDialog jd = new JDialog(parent);
		jd.setTitle(root.getTitle() +" (modifier)");
		jd.setModal(false);
		jd.getContentPane().setLayout(new BorderLayout());

		
		modification = currentTask.getParameters();
		
		currentDialog = Utils.addDialogButton(jd, new Runnable() {

			@Override
			public void run() {
				if (!hasError) {
										
					// Copier tous les éditables visibles
					loadDialogValues(currentTask.getParameters(), false);
					
					jd.setVisible(false);
				}
			}
		});
		currentDialog.getOkButton().setEnabled(!hasError);

		JScrollPane scrollPane = new JScrollPane(this);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBorder(null);
		jd.getContentPane().add(scrollPane, BorderLayout.CENTER);

		jd.pack();
		if (parent != null) {
			jd.setLocationRelativeTo(parent);
		}
		jd.setVisible(true);
	}
	
	public void showStartDialog(Window parent)
	{
		init();
		
		// If req... On crée un dialogue...
		if (hasVisibleField()) {
			final JDialog jd = new JDialog(parent);
			jd.setTitle(root.getTitle() +" (démarrer)");
			jd.setModal(false);
			jd.getContentPane().setLayout(new BorderLayout());
			jd.add(this);
//			jd.setMaximumSize(maximumSize);
			currentDialog = Utils.addDialogButton(jd, new Runnable() {

				@Override
				public void run() {
					if (!hasError) {
						final ITaskParameterView view = new TaskParameterView(
								focusUi.getApplication().getConfigurationTaskValues(),
								focusUi.getApplication().getConfigurationTaskValues().getTaskView(root),
								focusUi.getApplication().getLastUsedTaskValues(),
								focusUi.getApplication().getLastUsedTaskValues().getTaskView(root)
								);
						
						// Copier tous les éditables visibles
						loadDialogValues(view, true);
						
						jd.setVisible(false);
						BaseTask task = focusUi.getApplication().getTaskManager().startTask(focusUi, root, view);
					}
				}
			});
			currentDialog.getOkButton().setEnabled(!hasError);
			JScrollPane scrollPane = new JScrollPane(this);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			scrollPane.setBorder(null);
			jd.getContentPane().add(scrollPane, BorderLayout.CENTER);

			jd.pack();
			if (parent != null) {
				jd.setLocationRelativeTo(parent);
			}
			jd.setVisible(true);
		} else {
			final ITaskParameterView view = new TaskParameterView(
					focusUi.getApplication().getConfigurationTaskValues(),
					focusUi.getApplication().getConfigurationTaskValues().getTaskView(root),
					focusUi.getApplication().getLastUsedTaskValues(),
					focusUi.getApplication().getLastUsedTaskValues().getTaskView(root)
					);
			BaseTask task = focusUi.getApplication().getTaskManager().startTask(focusUi, root, view);
		}
	}

	public <T> void addControler(ParameterPath<T> forParameter, TaskFieldControler<T> taskFieldControler) {
		ParameterStatus<T> pstatus = (ParameterStatus<T>) fields.get(forParameter);
		if (pstatus != null) {
			pstatus.fieldControlers.add(taskFieldControler);
		}
	}


	public void addControler(SubTaskPath wildCard, TaskFieldControler<?> taskFieldControler) {
		
		for(ParameterPath<?> parameter : fields.keySet())
		{
			if (parameter.isChildOf(wildCard)) {
				addControler((ParameterPath)parameter, (TaskFieldControler)taskFieldControler);
			}
		}
	}

	public ITaskParameterBaseView getView(final SubTaskPath path) {
		return new ITaskParameterBaseView() {
			
			@Override
			public ITaskParameterBaseView getSubTaskView(TaskLauncherDefinition child) {
				return getView(path.forChild(child));
			}
			
			@Override
			public <TYPE> TYPE get(TaskParameterId<TYPE> key) throws ParameterNotKnownException {
				return TaskParameterPanel.this.getParameterValue(path.forParameter(key));
			}
		};
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		
		Dimension d = getLayout().preferredLayoutSize(this);
		int maxHeight = focusUi.getMainWindow().getHeight();
		maxHeight -= 120;
		if (maxHeight < 120) {
			maxHeight = 120;
		}
			
		if (d.getHeight() > maxHeight) {
			d = new Dimension(d.width, maxHeight);
		}
		return d;
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 1;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 10;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}