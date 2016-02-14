package fr.pludov.scopeexpress.tasks.steps;

import java.util.*;

/** 
 * Lance plusieurs tache en parallèle, et remonte le premier message reçu (les autres taches sont interrompues)
 * 
 * Lors du démarrage/sortie de pause: toutes les filles sont lancées, et le lancement est ininterruptible.
 * A la fin du lancement seulement, on controle l'état
 */
public class Fork extends Step implements StepContainer
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
		EndMessage message;
		Runnable onResume;
	};


	List<Step> childs = new ArrayList<>();

	Status status;

	public Fork Spawn(Step child)
	{
		this.childs.add(child);
		child.setParent(this);
		return this;
	}

	@Override
	public void enter()
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
	public void abortRequest(InterruptType wtf) {
		if (status.pendingInterruption == null || status.pendingInterruption.ordinal() < wtf.ordinal()) {
			status.pendingInterruption = wtf;
			if (!status.starting) {
				checkGlobalCondition();
			}
		}
	}

	void finished(Step from, EndMessage sm)
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
		status.paused[i] = EndMessage.isPausedMessage(sm);

		if (!status.done) {
			// C'est le premier !
			status.done = true;
			status.message = sm;
		}

		checkGlobalCondition();
	}

	void checkGlobalCondition() {
		if (status.starting) {
			return;
		}

		Status currentStatus = status;

		Restart: while(status == currentStatus)
		{
			// Lancer des demandes d'interruption si nécessaire
			InterruptType wantedInterrupt;
			if (status.done && !EndMessage.isPausedMessage(status.message)) {
				wantedInterrupt = InterruptType.Abort;
			} else {
				wantedInterrupt = status.pendingInterruption;
			}
			if (wantedInterrupt != null) {
				for(int j = 0; j < childs.size() && status == currentStatus; ++j) {
					if (status.runnings[j] && InterruptType.lower(status.abortRequested[j], wantedInterrupt))
					{
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
			EndMessage msg = status.message;				
			status.pendingInterruption = null;
			status.done = false;
			status.message = null;

			// la mise en pause n'est possible que si toutes les taches sont paused
			if (EndMessage.isPausedMessage(msg)) {
				// On verifie que tout le monde est arreté en pause.
				// Si ce n'est pas le cas, on se rabat sur le aborted

				for(int j = 0; j < childs.size(); ++j) {
					if (!status.paused[j]) {
						msg = new StepInterruptedMessage(InterruptType.Abort);
						break;
					}
				}
				if (EndMessage.isPausedMessage(msg)) {
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
				terminate(EndMessage.success());
			} else {
				terminate(msg);
			}
		}
	}

	@Override
	public void resume() {
		status.onResume.run();
	}

	@Override
	public void handleMessage(Step child, EndMessage err) {
		finished(child, err);
	}
}