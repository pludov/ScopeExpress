package fr.pludov.scopeexpress.tasks.sequence;

import java.io.*;
import java.util.*;
import java.util.function.*;

import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.focus.MosaicListener.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.tasks.autofocus.*;
import fr.pludov.scopeexpress.tasks.guider.*;
import fr.pludov.scopeexpress.tasks.shoot.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.utils.*;

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
	// Positionné à la suite d'une vérif de focus
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
	
	static interface StepMessage
	{
		
	}
	
	static class StepAbortedMessage implements StepMessage
	{
		
	}
	
	static interface StepContainer
	{
		/** Selection du prochain noeud */
		public void advance(Step from);
		
		public void handleMessage(Step child, StepMessage err);
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
				parent.advance(this);
			} else {
				setFinalStatus(BaseStatus.Success);
			}
		}
		
		final void throwError(StepMessage stepError)
		{
			if (parent != null) {
				parent.handleMessage(this, stepError);
			} else {
				if (stepError instanceof StepAbortedMessage && TaskAbstractSequence.this.getInterrupting() != null)
				{
					setFinalStatus(TaskAbstractSequence.this.getInterrupting());
				} else {
					setFinalStatus(BaseStatus.Error, stepError.toString());
				}
			}
		}

		final void setParent(StepContainer stepSequence) {
			if (parent != null) {
				throw new RuntimeException("Multiple parent not allowed");
			}
			this.parent = stepSequence;
		}

		/** 
		 * Demande une terminaison. 
		 * Pas de garantie qu'elle soit honorée (un succès est toujours possible...)
		 * L'appelant devra alors gérer lui même la condition d'abort.
		 */
		abstract void abortRequest();		
	};
	
	public static interface StepCondition {
		boolean evaluate();
	}
	
	class While extends Step implements StepContainer {
		StepCondition condition;
		Step block;
		boolean abortRequested;
		
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
			abortRequested = false;
			if (condition.evaluate() && this.block != null) {
				this.block.enter();
			} else {
				leave();
			}
		}
		
		@Override
		public void advance(Step child) {
			assert(child == block);
			enter();
		}
		
		@Override
		public void resume() {
			this.block.resume();
		}
		
		@Override
		public void handleMessage(Step child, StepMessage err) {
			throwError(err);
		}
		
		@Override
		void abortRequest() {
			abortRequested = true;
			if (block == null) {
				throwError(new StepAbortedMessage());
			} else {
				block.abortRequest();
			}
		}
	}
	
	class If extends Step implements StepContainer {
		StepCondition condition;
		Step onTrue, onFalse;
		Boolean where;
		boolean abortRequested;
		
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
			if (where == null) {
				return null;
			}
			return where ? onTrue : onFalse;
		}
		
		@Override
		public void enter() {
			abortRequested = false;
			where = null;
			where = condition.evaluate();
			Step currentStep = getActiveStep();
			if (currentStep == null) {
				leave();
			} else {
				currentStep.enter();
			}
		}
		
		@Override
		public void advance(Step child) {
			assert(child == onTrue || child == onFalse);
			if (abortRequested) {
				throwError(new StepAbortedMessage());
			} else {
				leave();
			}
		}
		
		@Override
		public void resume() {
			Step currentStep = getActiveStep();
			currentStep.resume();
		}

		@Override
		public void handleMessage(Step child, StepMessage err) {
			throwError(err);
		}
		
		@Override
		void abortRequest() {
			abortRequested = true;
			if (getActiveStep() != null) {
				getActiveStep().abortRequest();
			} else {
				throwError(new StepAbortedMessage());
			}
		}
	}
	
	class StepSequence extends Step implements StepContainer
	{
		Step [] steps;
		int currentPosition;
		boolean abortRequested;
		
		
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
			abortRequested = false;
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
		public void advance(Step child) {
			assert(child == steps[currentPosition]);
			
			if (abortRequested) {
				throwError(new StepAbortedMessage());
				return;
			}
			
			currentPosition++;
			if (currentPosition >= steps.length) {
				leave();
			} else {
				steps[currentPosition].enter();
			}
		}
		
		@Override
		public void handleMessage(Step child, StepMessage err) {
			throwError(err);
		}
		
		@Override
		void abortRequest() {
			abortRequested = true;
			steps[currentPosition].abortRequest();	
		}
	}
	
	/** 
	 * Execute du code immédiatement. 
	 * Ne peut être interrompu ou mis en pause.
	 */
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
		
		@Override
		void abortRequest() {
			// Rien...
		}
	}
	
	interface SubTaskStatusChecker
	{
		void evaluate(BaseTask bt);
	}

	static class WrongSubTaskStatus implements StepMessage
	{
		final String message;
		final IStatus status;
		
		WrongSubTaskStatus(BaseTask bt)
		{
			this.status = bt.getStatus();
			message = "Tache " + bt.getTitle() + " : " + bt.getTitle();
		}
		
		@Override
		public String toString() {
			return message;
		}

		public IStatus getStatus() {
			return status;
		}
	}
	
	class SubTask extends Step implements StepContainer
	{
		final TaskLauncherDefinition child;
		
		Map<IStatus, Object> onStatus;
		Step runningStatus;
		Function<Void, String> titleProvider;
		boolean abortRequested;

		private ChildLauncher launcher;
		
		SubTask(TaskLauncherDefinition child)
		{
			this.child = child;
		}
		
		@Override
		void enter() {
			launcher = new ChildLauncher(TaskAbstractSequence.this, child) {
				@Override
				public void onDone(BaseTask bt) {
					if (launcher != this) {
						return;
					}
					launcher = null;
					
					if (abortRequested) {
						throwError(new StepAbortedMessage());
					} else {
						
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
								throwError(new WrongSubTaskStatus(bt));
							}
						}
					}
				}
			};
			runningStatus = null;
			abortRequested = false;
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
		public void advance(Step child) {
			assert(child == runningStatus);
			if (abortRequested) {
				throwError(new StepAbortedMessage());
			} else {
				leave();
			}
		}
		
		@Override
		void resume() {
			// FIXME: et en cas de pause pendant la tache fille ???
			runningStatus.resume();
		}
		
		@Override
		public void handleMessage(Step child, StepMessage err) {
			throwError(err);
		}
		
		@Override
		void abortRequest() {
			abortRequested = true;
			if (runningStatus == null) {
				// On est en train de faire tourner la tache...
				assert(launcher != null);
				launcher.getTask().requestCancelation(BaseStatus.Aborted);
			} else {
				runningStatus.abortRequest();
			}
		}
	}
	
	class Try extends Step implements StepContainer
	{
		final Step main;
		final List<Couple<Function<StepMessage, Boolean>, Step>> catches = new ArrayList<>();
		
		Step current;
		boolean abortRequested;
		
		Try(Step main)
		{
			this.main = main;
			this.main.setParent(this);
		}

		public Step Catch(Function<StepMessage, Boolean> filter, Step immediate) {
			catches.add(new Couple<>(filter, immediate));
			immediate.setParent(this);
			return this;
		}
		
		@Override
		void enter() {
			current = null;
			abortRequested = false;
			
			current = main;
			main.enter();
		}
		
		@Override
		public void advance(Step from) {
			assert(current == from);
			current = null;
			if (abortRequested) {
				throwError(new StepAbortedMessage());
			} else {
				leave();
			}
		}
		
		@Override
		public void handleMessage(Step child, StepMessage err) {
			assert(current == child);
			if (child != main) {
				throwError(err);
			} else {
				// Le catch
				for(Couple<Function<StepMessage, Boolean>, Step> catchItem : catches)
				{
					if (catchItem.getA().apply(err)) {
						current = catchItem.getB();
						current.enter();
						return;
					}
				}
				throwError(err);
			}
		}
		
		@Override
		void resume() {
			// FIXME: ça ne marche pas
		}
		
		@Override
		void abortRequest() {
			abortRequested = true;
			current.abortRequest();
		}
		
	}
	
	/** Lance plusieurs tache en //, remonte le premier message reçu (les autres taches sont interrompues) */
	class Fork extends Step implements StepContainer
	{
		class Status {
			int nextStartId;
			boolean [] runnings = new boolean[childs.size()];
			boolean [] abortRequested = new boolean[childs.size()];
			StepMessage message;
			boolean done = false;
		};
		List<Step> childs = new ArrayList<>();
		
		Status status;
		
		Fork Spawn(Step child)
		{
			this.childs.add(child);
			child.setParent(this);
			return this;
		}
		
		@Override
		void enter()
		{
			Status thisStatus = new Status();
			if (status != null) {
				throw new RuntimeException("not reentrant");
			}
			status = thisStatus;
			// Démarre tout, mais arrête en cas d'interruption
			while(status == thisStatus && status.nextStartId < childs.size())
			{
				status.runnings[status.nextStartId] = true;
				status.nextStartId++;
				childs.get(status.nextStartId - 1).enter();
			}
		}
		
		@Override
		void resume() {
			// On fait quoi ???
		}
		
		@Override
		void abortRequest() {
			status.done = true;
			status.message = new StepAbortedMessage();
			
			finish();
		}
		
		void finished(Step from, StepMessage sm)
		{
			int i;
			for(i = 0; i < childs.size(); ++i) {
				if (childs.get(i) == from) {
					break;
				}
			}
			if (i == childs.size()) {
				throw new RuntimeException("Not a child");
			}
			status.runnings[i] = false;
			
			if (!status.done) {
				// C'est le premier !
				status.done = true;
				status.message = sm;
			} else {
				logger.info("Le message est ignoré : " + sm);
			}
			
			finish();
		}
		
		void finish() {
			Status currentStatus = status;
			
			// Maintenant, si c'était le dernier, on quitte !
			for(int j = 0; j < childs.size() && status == currentStatus; ++j) {
				if (status.runnings[j] && !status.abortRequested[j])
				{
					status.abortRequested[j] = true;
					childs.get(j).abortRequest();
				}
			}
			// Et si tout le monde est terminé, on arrête !
			if (status == currentStatus) {
				for(int j = 0; j < childs.size(); ++j) {
					if (status.runnings[j]) {
						return;
					}
				}
				logger.info("Toutes les filles sont maintenant arrétées");
				StepMessage msg = status.message;
				status = null;
				
				if (msg == null) {
					leave();
				} else {
					throwError(msg);
				}
			}
		}
		
		@Override
		public void advance(Step from) {
			finished(from, null);
		}
		
		@Override
		public void handleMessage(Step child, StepMessage err) {
			finished(child, err);
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
					
					new Try(new Fork()
							.Spawn(new StepSequence(
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
							.Spawn(new SubTask(getDefinition().guiderMonitor)
									.SetTitle((Void v) -> ("Supervision du guidage"))
							))
						.Catch((StepMessage sm) -> ((sm instanceof WrongSubTaskStatus) 
													&& ((WrongSubTaskStatus)sm).getStatus() == TaskGuiderMonitor.GuiderOutOfRange),
								new Immediate(() -> {})
							)
			))
	);

	@Override
	public void start() {
		setStatus(BaseStatus.Processing);
		start.enter();
	}

	@Override
	public void requestCancelation(BaseStatus statusForInterrupting) {
		super.requestCancelation(statusForInterrupting);
		
		if (getStatus() == BaseStatus.Processing) {
			start.abortRequest();
		}
	}
}
