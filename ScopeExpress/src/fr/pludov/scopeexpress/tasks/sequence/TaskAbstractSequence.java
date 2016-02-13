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

	
	/**
	 * Type d'interruption d'étape
	 * On peut demander pause puis Abort.
	 * Par contre une demande de Pause après un Abort sera ignorée
	 */
	enum InterruptType {
		Pause,
		Abort;

		/** true si a < b */
		public static boolean lower(InterruptType a, InterruptType b) {
			if (a == null) {
				return (b != null);
			}
			if (b == null) {
				return false;
			}
			return a.ordinal() < b.ordinal();
		}
	}
	
	/** 
	 * Ce message est émit par un step qui s'est interrompu,
	 * ou alors qui s'est mis en pause.
	 * 
	 * Un step en pause peut être redémarré par un appel à resume
	 */
	static class StepInterruptedMessage implements StepMessage
	{
		InterruptType type;
		
		public StepInterruptedMessage(InterruptType requested) {
			this.type = requested;
		}
	}
	
	static boolean isPausedMessage(StepMessage sm)
	{
		return ((sm instanceof StepInterruptedMessage) && ((StepInterruptedMessage)sm).type == InterruptType.Pause);
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
		void leave()
		{
			if (parent != null) {
				parent.advance(this);
			} else {
				setFinalStatus(BaseStatus.Success);
			}
		}
		
		void throwError(StepMessage stepError)
		{
			if (parent != null) {
				parent.handleMessage(this, stepError);
			} else {
				if (stepError instanceof StepInterruptedMessage && TaskAbstractSequence.this.getInterrupting() != null)
				{
					setFinalStatus(TaskAbstractSequence.this.getInterrupting());
				} else if (isPausedMessage(stepError) && TaskAbstractSequence.this.isPauseRequested()) {
					// TaskAbstractSequence.this.onUnpause
					pausing = false;
					setStatus(BaseStatus.Paused);
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
		 * Doit être appellé par un parent (pas déclenché par un fils, sinon race condition) 
		 * Pas de garantie qu'elle soit honorée (un succès est toujours possible...)
		 * L'appelant devra alors gérer lui même la condition d'abort.
		 */
		abstract void abortRequest(InterruptType type);		
	};
	
	public static interface StepCondition {
		boolean evaluate();
	}
	
	/**
	 * Logique de traitement des interruptions qui prend en compte l'arret et la relance des fils
	 * 
	 * Elle repose sur getActiveChilds() qui doit retourner les fils en cours
	 * 
	 * 
	 */
	abstract class StepInterruptionHandler
	{
		class InterruptionStatus
		{
			boolean done;
			InterruptType requested;
		}
		
		final Step step;
		
		InterruptType requested;
		Object requestIdentifier;
		Object resumeIdentifier;
		Runnable restart;
		IdentityHashMap<Step, InterruptionStatus> childStatus;
		
		StepInterruptionHandler(Step parent)
		{
			this.step = parent;
		}
		
		abstract Collection<Step> getActiveSteps();
		
		
		void reset()
		{
			requested = null;
			requestIdentifier = null;
			resumeIdentifier = null;
			childStatus = null;
			restart = null;
		}
		
		/**
		 * Traite une demande d'interruption.
		 * Elle est forwardée au fils
		 * @param it
		 */
		void interruptRequest(InterruptType it)
		{
			if (requested == null || requested.ordinal() < it.ordinal()) {
				requested = it;
				requestIdentifier = new Object();
				finish();
			}
		}
		
		InterruptionStatus getChildStatus(Step child)
		{
			if (childStatus == null) {
				childStatus = new IdentityHashMap<>();
			}
			InterruptionStatus result = childStatus.get(child);
			if (result == null) {
				result = new InterruptionStatus();
				childStatus.put(child, result);
			}
			return result;
		}

		void finish()
		{
			Object requestToStop = requestIdentifier;
			boolean restart = true;
		CheckActiveLoop:
			while(requestIdentifier == requestToStop) {
				restart = false;
				// Faire passer la demande à tous les fils actifs (non terminés vu du parents, mais peut être en pause...)
				Collection<Step> activeSteps = getActiveSteps();
				boolean somethingNotDone = false;
				for(Step s : activeSteps)
				{
					InterruptionStatus status = getChildStatus(s);
					if (status.done) {
						continue;
					}
					somethingNotDone = true;
					if (InterruptType.lower(status.requested, requested))
					{
						status.requested = requested;
						s.abortRequest(requested);
						continue CheckActiveLoop;
					}
				}
				
				if (somethingNotDone) {
					return;
				}
				
				// On arrive là quand tout le monde est terminé.
				// Dans ce cas, on peut emetrre une message comme quoi on peut redémarrer.
				
				if (requested == InterruptType.Pause) {
					this.restart = ()->{
						Object resumeIdentifierValue = new Object();
						resumeIdentifier = resumeIdentifierValue;
						// Comment on fait pour savoir que les restart doivent continuer en cas d'interruption ?
						for(Step step : activeSteps) {
							if (resumeIdentifier != resumeIdentifierValue) {
								return;
							}
							step.resume();
						}
						if (resumeIdentifier != resumeIdentifierValue) {
							return;
						}
						// Tous le monde a redémarré ?
						resumeIdentifier = null;
					};
				}
				StepInterruptedMessage stepError = new StepInterruptedMessage(requested);
				requested = null;
				requestIdentifier = null;
				step.throwError(stepError);
				return;
			}
		}
		
		/** Interrompt l'action de resume éventuellement en cours */
		void leave()
		{
			resumeIdentifier = null;
			childStatus = null;
		}
		
		void resume()
		{
			Runnable todo = this.restart;
			this.restart = null;
			todo.run();
		}
		
		/** 
		 * Filtres les messages relatifs aux fils
		 * On suppose qu'on ne reçoit que des messages pour des fils actifs 
		 */
		boolean handleChildMessage(Step step, StepMessage msg)
		{
			if ((requested != null) && (msg instanceof StepInterruptedMessage))
			{
				StepInterruptedMessage smi = (StepInterruptedMessage) msg;
				if (smi.type == requested) {
					// Il est dans le bon type... On prend le message et on l'ignore
					getChildStatus(step).done = true;
					finish();
					return true;
				}
			}
			return false;
		}

		/** 
		 * Traite une interruption en attente. Celà suppose qu'il n'y a plus de fils actifs 
		 * Si retourne true, la tache est arrété (il ne doit pas y avoir de code après) 
		 */
		public boolean doInterrupt(Runnable onRestart) {
			if (requested != null) {
				StepInterruptedMessage sim = new StepInterruptedMessage(requested);
				if (requested == InterruptType.Pause) {
					restart = onRestart;
				} else {
					restart = null;
				}
				requested = null;
				
				step.throwError(sim);
				return true;
			}
			return false;
		}
	}
	
	/**
	 * Pour l'utiliser, il faut:
	 *   surcharger handleMessage pour faire passer par l'interruptionHandler
	 */
	abstract class StepWithSimpleInterruptionHandler extends Step {
		protected final StepInterruptionHandler interruptionHandler;

		StepWithSimpleInterruptionHandler()
		{
			this.interruptionHandler = new StepInterruptionHandler(this) {
				@Override
				Collection<Step> getActiveSteps() {
					Collection<Step> rslt = StepWithSimpleInterruptionHandler.this.getActiveSteps();
					if (rslt == null) {
						return Collections.emptyList();
					}
					return rslt;
				}
			};
		}
		
		protected abstract Collection<Step> getActiveSteps();
		
		@Override
		void enter() {
			interruptionHandler.reset();
		}
		
		@Override
		void abortRequest(InterruptType type) {
			interruptionHandler.interruptRequest(type);
		}

		@Override
		public void resume() {
			interruptionHandler.resume();
		}
		
		@Override
		void leave()
		{
			interruptionHandler.leave();
			super.leave();
		}
		
		@Override
		void throwError(StepMessage stepError) {
			interruptionHandler.leave();
			super.throwError(stepError);
		}
		

		/** Implementation de base */
		public void handleMessage(Step child, StepMessage err) {
			if (interruptionHandler.handleChildMessage(child, err)) {
				return;
			}
			throwError(err);
		}
		
	}
	
	class While extends StepWithSimpleInterruptionHandler implements StepContainer {
		
		StepCondition condition;
		Step block;
		
		public While(StepCondition condition)
		{
			this.condition = condition;
		}
		
		@Override
		protected Collection<Step> getActiveSteps() {
			return block != null ? Collections.singletonList(block) : null;
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
			super.enter();
			if (condition.evaluate() && this.block != null) {
				this.block.enter();
			} else {
				leave();
			}
		}
		
		@Override
		public void advance(Step child) {
			assert(child == block);
			if (interruptionHandler.doInterrupt(() -> {advance(child);})) {
				return;
			}
			enter();
		}
		
	}
	
	class If extends StepWithSimpleInterruptionHandler implements StepContainer {
		StepCondition condition;
		Step onTrue, onFalse;
		Boolean where;
		
		public If(StepCondition condition)
		{
			this.condition = condition;
		}
		
		@Override
		protected Collection<Step> getActiveSteps() {
			Step result = getActiveStep();
			if (result == null) return null;
			return Collections.singletonList(result);
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
			super.enter();
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
			if (interruptionHandler.doInterrupt(()->{advance(child);})) {
				return;
			}
			leave();
		}
	}
	
	class StepSequence extends StepWithSimpleInterruptionHandler implements StepContainer
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
		protected Collection<Step> getActiveSteps() {
			if (currentPosition >= steps.length) {
				return null;
			}
			return Collections.singleton(steps[currentPosition]);
		}
		
		
		@Override
		void enter() {
			super.enter();
			currentPosition = 0;
			if (currentPosition >= steps.length) {
				leave();
			} else {
				steps[currentPosition].enter();
			}
		}
		
		@Override
		public void advance(Step child) {
			assert(child == steps[currentPosition]);
			
			if (interruptionHandler.doInterrupt(()->{advance(child);})) {
				return;
			}
			
			currentPosition++;
			if (currentPosition >= steps.length) {
				leave();
			} else {
				steps[currentPosition].enter();
			}
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
		void abortRequest(InterruptType type) {
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
		Runnable onResume;
		Function<Void, String> titleProvider;
		InterruptType abortRequested;

		private ChildLauncher launcher;
		
		SubTask(TaskLauncherDefinition child)
		{
			this.child = child;
		}
		
		@Override
		void enter() {
			launcher = new ChildLauncher(TaskAbstractSequence.this, child) {
				@Override
				public void onStatusChanged(BaseTask bt) {
					if (launcher != this) {
						return;
					}
					if ((bt.getStatus() == BaseStatus.Paused) && (abortRequested == InterruptType.Pause))
					{
						onResume = () -> {
							bt.resume();
						};
						StepInterruptedMessage stepError = new StepInterruptedMessage(abortRequested);
						abortRequested = null;
						throwError(stepError);
					}
				}
				
				@Override
				public void onDone(BaseTask bt) {
					// FIXME: ceci peut arriver pendant que l'étape est mise en pause... 
					// ça peut tout simplement redémarrer le processus !
					if (launcher != this) {
						return;
					}
					if (abortRequested != null) {
						onResume = () -> {onDone(bt);};
						StepInterruptedMessage stepError = new StepInterruptedMessage(abortRequested);
						abortRequested = null;
						throwError(stepError);
						return;
					}
					
					launcher = null;
	
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
			};
			runningStatus = null;
			abortRequested = null;
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
			if (abortRequested != null) {
				onResume = () -> { advance(child); };
				StepInterruptedMessage stepError = new StepInterruptedMessage(abortRequested);
				abortRequested = null;
				throwError(stepError);
			} else {
				leave();
			}
		}
		
		@Override
		void resume() {
			onResume.run();
		}
		
		@Override
		public void handleMessage(Step child, StepMessage err) {
			if (isPausedMessage(err)) {
				onResume = () -> { child.resume(); };
			}
			throwError(err);
		}
		
		@Override
		void abortRequest(InterruptType it) {
			if (InterruptType.lower(abortRequested, it))
			{
				abortRequested = it;
				if (launcher != null) {
					// On est en train de faire tourner la tache...
					switch(it) {
					case Abort:
						launcher.getTask().requestCancelation(BaseStatus.Aborted);
						return;
					case Pause:
						launcher.getTask().requestPause();
						return;
					}
					throw new RuntimeException("internal error");
				} else {
					runningStatus.abortRequest(it);
				}	
			}
		}
	}
	
	/** Les bloques catch ne sont pas interruptibles */
	class Try extends Step implements StepContainer
	{
		final Step main;
		final List<Couple<Function<StepMessage, Boolean>, Step>> catches = new ArrayList<>();
		
		InterruptType pendingInterruption;
		Runnable onResume;
		Step current;
		
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
			pendingInterruption = null;
			current = main;
			main.enter();
		}
		
		@Override
		public void advance(Step from) {
			assert(current == from);

			if (pendingInterruption != null) {
				onResume = () -> {advance(from);};
				StepInterruptedMessage stepError = new StepInterruptedMessage(pendingInterruption);
				pendingInterruption = null;
				throwError(stepError);
				return;
			}
			current = null;
			leave();
		}
		
		@Override
		public void handleMessage(Step child, StepMessage err) {
			assert(current == child);
			if (child != main) {
				throwError(err);
			} else {
				if (isPausedMessage(err) && pendingInterruption == InterruptType.Pause) {
					pendingInterruption = null;
					onResume = ()->{main.resume();};
					throwError(err);
					return;
				}
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
			onResume.run();
		}
		
		@Override
		void abortRequest(InterruptType pendingInterruption) {
			if (this.pendingInterruption == null || this.pendingInterruption.ordinal() < pendingInterruption.ordinal())
			{
				this.pendingInterruption = pendingInterruption;
				if (current == main) {
					current.abortRequest(pendingInterruption);
				}
			}
		}
		
	}
	
	/** 
	 * Lance plusieurs tache en //, remonte le premier message reçu (les autres taches sont interrompues)
	 * En cas de demande d'interruption, on fait simplmenet suivre au taches filles, et on remonte l'interruption quand toutes les filles sont terminées
	 * 
	 * En cas de demande de pause ???
	 * Si la demande arrive avant que toutes les filles soient démarrées => la reprise (nextStartId)
	 * Sinon, on se contente de la faire suivre
	 * 
	 * Ou alors, on simplifie: toutes les filles sont lancées, et le lancement est ininterruptible.
	 * A la fin du lancement, on controle l'état
	 * 
	 */
	class Fork extends Step implements StepContainer
	{
		class Status {
			boolean [] runnings = new boolean[childs.size()];
			boolean [] paused = new boolean[childs.size()];
			InterruptType [] abortRequested = new InterruptType[childs.size()];
			
			boolean starting = true;
			
			InterruptType pendingInterruption;
			// Est-ce qu'il y en a un qui s'est terminé ?
			boolean done = false;
			// C'est quoi le résultat global ?
			StepMessage message;
			Runnable onResume;
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
			status = thisStatus;
			// Démarre tout, mais arrête en cas d'interruption
			for(int i = 0; i < childs.size(); ++i)
			{
				status.runnings[i] = true;
				childs.get(i).enter();
			}
			status.starting = false;
			checkGlobalCondition();
		}
		
		@Override
		void abortRequest(InterruptType wtf) {
			logger.debug("Fork.abortRequest:" + wtf);
			if (status.pendingInterruption == null || status.pendingInterruption.ordinal() < wtf.ordinal()) {
				status.pendingInterruption = wtf;
				if (!status.starting) {
					checkGlobalCondition();
				}
			}
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
			status.paused[i] = isPausedMessage(sm);
			
			if (!status.done) {
				// C'est le premier !
				status.done = true;
				status.message = sm;
			} else {
				logger.debug("Le message est ignoré : " + sm);
			}
			
			checkGlobalCondition();
		}
		
		void checkGlobalCondition() {
			if (status.starting) {
				logger.debug("Ignore checkGlobalCondition (starting");
				return;
			}
			
			Status currentStatus = status;

			Restart: while(status == currentStatus)
			{
				// Lancer des demandes d'interruption si nécessaire
				InterruptType wantedInterrupt;
				if (status.done && !isPausedMessage(status.message)) {
					wantedInterrupt = InterruptType.Abort;
				} else {
					wantedInterrupt = status.pendingInterruption;
				}
				if (wantedInterrupt != null) {
					for(int j = 0; j < childs.size() && status == currentStatus; ++j) {
						if (status.runnings[j] && InterruptType.lower(status.abortRequested[j], wantedInterrupt))
						{
							logger.debug("Asking " + wantedInterrupt + " to " + j);
							status.abortRequested[j] = wantedInterrupt;
							childs.get(j).abortRequest(wantedInterrupt);
							continue Restart;
						}
					}
				}
				
			
				// Et si tout le monde est terminé, on arrête !
				for(int j = 0; j < childs.size(); ++j) {
					if (status.runnings[j]) {
						return;
					}
				}
				logger.debug("Toutes les filles sont maintenant arrétées");
				StepMessage msg = status.message;				
				status.pendingInterruption = null;
				status.done = false;
				status.message = null;
				
				// la mise en pause n'est possible que si toutes les taches sont paused
				if (isPausedMessage(msg)) {
					// On verifie que tout le monde est arreté en pause.
					// Si ce n'est pas le cas, on se rabat sur le aborted
					
					for(int j = 0; j < childs.size(); ++j) {
						if (!status.paused[j]) {
							logger.debug("Mise en pause en echec");
							msg = new StepInterruptedMessage(InterruptType.Abort);
							break;
						}
					}
					if (isPausedMessage(msg)) {
						Status newStatus = new Status();
						newStatus.starting = false;
						for(int i = 0; i < childs.size(); ++i) {
							newStatus.paused[i] = true;
						}
						newStatus.onResume = () -> {
							status.starting = true;
							for(int i = 0; i < childs.size(); ++i) {
								status.runnings[i] = true;
								status.abortRequested[i] = null;
								status.paused[i] = false;
								childs.get(i).resume();
							}
							status.starting = false;
							checkGlobalCondition();
						};
						
						status = newStatus;
					}
				}
				if (msg == null) {
					leave();
				} else {
					throwError(msg);
				}
			}
		}
		
		@Override
		void resume() {
			status.onResume.run();
		}
		
		@Override
		public void advance(Step from) {
			logger.debug("Advance: " + from);
			finished(from, null);
		}
		
		@Override
		public void handleMessage(Step child, StepMessage err) {
			logger.debug("message: " + child + " = " + err);
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
			start.abortRequest(InterruptType.Abort);
		}
	}
	
	@Override
	public boolean requestPause() {
		if (super.requestPause()) {
			onUnpause = ()->{
				setStatus(BaseStatus.Processing);
				start.resume(); 
			};
			start.abortRequest(InterruptType.Pause);
			return true;
		}
		return false;
	}
}
