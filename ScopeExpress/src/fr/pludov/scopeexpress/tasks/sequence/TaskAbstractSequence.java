package fr.pludov.scopeexpress.tasks.sequence;

import java.io.*;
import java.util.*;
import java.util.function.*;

import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.focus.MosaicListener.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.tasks.autofocus.*;
import fr.pludov.scopeexpress.tasks.shoot.*;
import fr.pludov.scopeexpress.ui.*;

/**
 * 
 * Tentative pour trouver une meilleure syntaxe à TaskSequence...
 * 
 * 
 */
public class TaskAbstractSequence extends BaseTask {
	// Nombre d'image effectivement prises
	int imageCount;
	// Nombre d'image prises depuis la dernière vérification de mise au point
	int consecutiveCountWithoutChecking;
	boolean needFocus;
	public TaskAbstractSequence(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher,
			BaseTaskDefinition taskDefinition) {
		super(focusUi, tm, parentLauncher, taskDefinition);
	}
	
	@Override
	public TaskSequenceDefinition getDefinition() {
		return (TaskSequenceDefinition) super.getDefinition();
	}
	

	@Override
	protected void cleanup() {
		super.cleanup();
		
		// unlistenPhd();
	}
	
	static interface StepContainer
	{
		/** Selection du prochain noeud */
		public void advance();
	}
	
	/** Les étapes sont sans état. */
	abstract class Step {
		StepContainer parent;
		
		abstract void enter();

		// Appellé après pause ?
		abstract void resume();
		
		/** Quitte l'étape, et avance au suivant */
		final void leave()
		{
			if (parent != null) {
				parent.advance();
			} else {
				setFinalStatus(BaseStatus.Success);
			}
			
		}

		void setParent(StepContainer stepSequence) {
			if (parent != null) {
				throw new RuntimeException("Multiple parent not allowed");
			}
			this.parent = stepSequence;
		}
		
	};
	
	public static interface StepCondition {
		boolean evaluate();
	}
	
	class While extends Step implements StepContainer {
		StepCondition condition;
		Step block;
		
		public While(StepCondition condition)
		{
			this.condition = condition;
		}

		public Step Do(Step stepSequence) {
			if (block != null) {
				throw new RuntimeException("Multiple do");
			}
			this.block = stepSequence;
			this.block.setParent(this);
			return this;
		}
		 

		@Override
		public void enter() {
			if (condition.evaluate() && this.block != null) {
				this.block.enter();
			} else {
				leave();
			}
		}
		
		@Override
		public void advance() {
			enter();
		}
		
		@Override
		public void resume() {
			this.block.resume();
		}
	}
	
	class If extends Step implements StepContainer {
		StepCondition condition;
		Step onTrue, onFalse;
		Boolean where;
		
		public If(StepCondition condition)
		{
			this.condition = condition;
		}
		
		public If Then(Step step)
		{
			if (onTrue != null) {
				throw new RuntimeException("Multiple then for one if");
			}
			onTrue = step;
			onTrue.setParent(this);
			return this;
		}
		
		public If Else(Step step)
		{
			if (onFalse != null) {
				throw new RuntimeException("Multiple false for one if");
			}
			onFalse = step;
			onFalse.setParent(this);

			return this;
		}
		
		private Step getActiveStep()
		{
			return where ? onTrue : onFalse;
		}
		
		@Override
		public void enter() {
			where = condition.evaluate();
			Step currentStep = getActiveStep();
			if (currentStep == null) {
				leave();
			} else {
				currentStep.enter();
			}
		}
		
		@Override
		public void advance() {
			leave();
		}
		
		@Override
		public void resume() {
			Step currentStep = getActiveStep();
			currentStep.resume();
		}
	}
	
	class StepSequence extends Step implements StepContainer
	{
		Step [] steps;
		int currentPosition;
		
		StepSequence(Step ...steps) {
			this.steps = steps;
			for(int i = 0; i < steps.length; ++i)
			{
				steps[i].setParent(this);
			}
		}
		
		
		@Override
		void enter() {
			currentPosition = 0;
			if (currentPosition >= steps.length) {
				leave();
			} else {
				steps[currentPosition].enter();
			}
		}
		
		@Override
		void resume() {
			steps[currentPosition].resume();
		}
		
		@Override
		public void advance() {
			currentPosition++;
			if (currentPosition >= steps.length) {
				leave();
			} else {
				steps[currentPosition].enter();
			}
		}
	}
	
	class Immediate extends Step
	{
		final Runnable runnable;
		
		Immediate(Runnable runnable)
		{
			this.runnable = runnable;
		}

		@Override
		void enter() {
			runnable.run();
			leave();
		}

		@Override
		void resume() {
			throw new RuntimeException("Cannot be suspended");
		}
	}
	
	interface SubTaskStatusChecker
	{
		void evaluate(BaseTask bt);
	}
	
	class SubTask extends Step implements StepContainer
	{
		final TaskLauncherDefinition child;
		
		Map<IStatus, Object> onStatus;
		Step runningStatus;
		Function<Void, String> titleProvider;
		
		SubTask(TaskLauncherDefinition child)
		{
			this.child = child;
		}
		
		@Override
		void enter() {
			ChildLauncher launcher = new ChildLauncher(TaskAbstractSequence.this, child) {
				@Override
				public void onDone(BaseTask bt) {
					
					Object todo = onStatus != null ? onStatus.get(bt.getStatus()) : null;
					
					if (todo != null) {
						if (todo instanceof Step) {
							runningStatus = (Step) todo;
							runningStatus.enter();
						} else {
							// FIXME : propagation d'exception ici
							((SubTaskStatusChecker)todo).evaluate(bt);
							leave();
						}
					} else {
						if (bt.getStatus() == BaseStatus.Success) {
							leave();
						} else {
							setFinalStatus(BaseStatus.Error);
						}
					}
				}
			};
			runningStatus = null;
			if (titleProvider != null)
			{
				launcher.getTask().setTitle(titleProvider.apply(null));
			}
			launcher.start();
		}
		
		public SubTask SetTitle(Function<Void, String> titleProvider)
		{
			this.titleProvider = titleProvider;
			return this;
		}
		
		
		public SubTask On(IStatus status, SubTaskStatusChecker checker)
		{
			if (onStatus == null) {
				onStatus = new HashMap<>();
			}
			if (onStatus.containsKey(status)) {
				throw new RuntimeException("Multiple path for status: " + status);
			}
			onStatus.put(status, checker);
			return this;
		}
		
		public SubTask On(IStatus status, Step child)
		{
			if (onStatus == null) {
				onStatus = new HashMap<>();
			}
			if (onStatus.containsKey(status)) {
				throw new RuntimeException("Multiple path for status: " + status);
			}
			child.setParent(this);
			onStatus.put(status, child);
			return this;
		}
		
		@Override
		public void advance() {
			if (runningStatus == null) {
				throw new RuntimeException("advance cannot be called out of status invocation");
			}
			leave();
		}
		
		@Override
		void resume() {
			// FIXME: et en cas de pause pendant la tache fille ???
			runningStatus.resume();
			
		}
	}
	
	List<Step> currentStep;
	
	
	Step startStep;
	
	Step stopAutoGuider = null;
	
	Step start = new StepSequence(
		new SubTask(getDefinition().filterWheel),
		
		// Si possible démarre l'auto guidage avant la MEP...
		new If(()->(get(getDefinition().guiderHandling) == GuiderHandling.Activate && !get(getDefinition().guiderStopForFilterFocuser)))
			.Then(new SubTask(getDefinition().guiderStart)),
		
		new If(()->(get(getDefinition().initialFocusHandling) == InitialFocusHandling.Forced))
			.Then(new StepSequence(
					new If(()->(get(getDefinition().guiderHandling) == GuiderHandling.Activate
								&& get(getDefinition().guiderStopForFilterFocuser)))
						.Then(new SubTask(getDefinition().guiderStop)),
					new SubTask(getDefinition().autofocus),
					new Immediate(() -> {consecutiveCountWithoutChecking = 0;})
			))
			.Else(
				new If(()->(get(getDefinition().initialFocusHandling) == InitialFocusHandling.Verified))
					.Then(new StepSequence(
							new SubTask(getDefinition().focusCheck)
								.On(BaseStatus.Success, (BaseTask bt)->{ 
										Integer r = bt.get(TaskCheckFocusDefinition.getInstance().passed);
										needFocus = r != null && r.intValue() != 0;
								}),
							new If(()->needFocus)
								.Then(new StepSequence(
										new If(()->(get(getDefinition().guiderHandling) == GuiderHandling.Activate
												&& get(getDefinition().guiderStopForFilterFocuser)))
											.Then(new SubTask(getDefinition().guiderStop)),
										new SubTask(getDefinition().autofocus)
								)),
							new Immediate(() -> {consecutiveCountWithoutChecking = 0;})
					))
			),
		
		// Si l'auto guidage est incompatible avec la MEP, démarre l'autoguidage que maintenant
		new If(()->(get(getDefinition().guiderHandling) == GuiderHandling.Activate && get(getDefinition().guiderStopForFilterFocuser)))
			.Then(new SubTask(getDefinition().guiderStart)),
		
		new While(()->(imageCount < get(getDefinition().shootCount)))
			.Do(new StepSequence(
					new If(()->(get(getDefinition().focusCheckInterval) != null 
								&& consecutiveCountWithoutChecking >= get(getDefinition().focusCheckInterval)))
						.Then(new StepSequence(
							new SubTask(getDefinition().focusCheck)
								.On(BaseStatus.Success, (BaseTask bt)->{ 
										Integer r = bt.get(TaskCheckFocusDefinition.getInstance().passed);
										needFocus = r != null && r.intValue() != 0;
								}),
							new If(()->needFocus)
								.Then(new StepSequence(
										// Arreter l'autoguidage si il est incompatible avec le focuseur
										new If(()->(get(getDefinition().guiderHandling) == GuiderHandling.Activate
												&& get(getDefinition().guiderStopForFilterFocuser)))
											.Then(new SubTask(getDefinition().guiderStop)),
											
										new SubTask(getDefinition().autofocus),

										// Redémarrer l'autoguidage si il était incompatible avec le focuseur
										new If(()->(get(getDefinition().guiderHandling) == GuiderHandling.Activate
												&& get(getDefinition().guiderStopForFilterFocuser)))
											.Then(new SubTask(getDefinition().guiderStart))
								)),
							new Immediate(() -> {consecutiveCountWithoutChecking = 0;})
					)),
					
					new SubTask(getDefinition().shoot)
						.SetTitle((Void v) -> ("Exposition " + (imageCount + 1) + " / " + get(getDefinition().shootCount)))
						.On(BaseStatus.Success, (BaseTask bt)->{
							String path = bt.get(TaskShootDefinition.getInstance().fits);
							Image image = focusUi.getApplication().getImage(new File(path));
							
							Mosaic targetMosaic = focusUi.getImagingMosaic();
							
							MosaicImageParameter mip = targetMosaic.addImage(image, ImageAddedCause.AutoDetected);
							

							LoadMetadataTask loadTask = new LoadMetadataTask(targetMosaic, mip);
							focusUi.getApplication().getBackgroundTaskQueue().addTask(loadTask);
							
							FindStarTask task = new FindStarTask(targetMosaic, image);
							focusUi.getApplication().getBackgroundTaskQueue().addTask(task);
							
							
							imageCount++; 
						})
						.On(BaseStatus.Aborted, (BaseTask bt)-> {
							logger.warn("Image abandonnée. Nouvel essai");
						}),
					new Immediate(() -> {consecutiveCountWithoutChecking++;})
			))
	);

	@Override
	public void start() {
		setStatus(BaseStatus.Processing);
		start.enter();
	}
	
}
