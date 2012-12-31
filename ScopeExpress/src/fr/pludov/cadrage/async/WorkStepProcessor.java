package fr.pludov.cadrage.async;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

import fr.pludov.cadrage.utils.IdentityHashSet;

/**
 * Travaille d'abord avec les resources disponibles
 */
public class WorkStepProcessor {

	List<WorkStep> todoList = new ArrayList<WorkStep>();
	
	static class WorkStepElected
	{
		WorkStep step;
		List<WorkStepResource> locked;
		
		Runnable requiredBackgroundBefore;
	}
	
	WorkStepElected chooseNextTask()
	{	
		for(Iterator<WorkStep> it = todoList.iterator(); it.hasNext();)
		{
			WorkStep step = it.next();
			
			if (!step.readyToProceed()) continue;
			boolean hasAllResources = true;

			List<WorkStepResource> locked = new ArrayList<WorkStepResource>();
		
			for(WorkStepResource resource: step.getRequiredResources())
			{
				if (resource.lock()) {
					locked.add(resource);
				} else {
					hasAllResources = false;
					break;
				}
			}
			
			if (hasAllResources)
			{
				it.remove();
				WorkStepElected result = new WorkStepElected();
				result.step = step;
				result.locked = locked;
				return result;
			}
			
			for(WorkStepResource resource : locked)
			{
				resource.unlock();
			}
		}
		
		// On n'a rien trouvé.
		// Dans ce cas, on va produire le premier venu ?
		for(WorkStep step : todoList)
		{
			if (!step.readyToProceed()) continue;

			final List<WorkStepResource> locked = new ArrayList<WorkStepResource>();
			
			for(WorkStepResource resource: step.getRequiredResources())
			{
				locked.add(resource);
			}
			
			WorkStepElected result = new WorkStepElected();
			result.step = step;
			result.locked = locked;
			result.requiredBackgroundBefore = new Runnable() {
				@Override
				public void run() {
					for(WorkStepResource resource: locked)
					{
						resource.produce();
					}
				}
			};
			return result;
		}
		
		return null;
	}
	
	// Cette tache est en cours de préparation (background)
	WorkStepElected backgroundPreparation = null;
	
	void proceedOne()
	{
		final WorkStepElected todo;
		if (backgroundPreparation != null) {
			if (backgroundPreparation.requiredBackgroundBefore == null) {
				todo = backgroundPreparation;
				backgroundPreparation = null;
			} else {
				return;
			}
		} else {
			todo = chooseNextTask();
		}
				
		if (todo != null) {
			if (todo.requiredBackgroundBefore != null) {
				backgroundPreparation = todo;
				Thread thread = new Thread() {
					public void run() {
						try {
							todo.requiredBackgroundBefore.run();
						} finally {
							todo.requiredBackgroundBefore = null;
							wakeUp();
						}
					}
				};
				thread.setPriority(Thread.MIN_PRIORITY);
				thread.start();
				
				return;
			}
			
			try {
				todo.step.proceed();
				wakeUp();
			} finally {
				for(WorkStepResource res : todo.locked)
				{
					res.unlock();
				}
			}
		}
	}
	
	boolean notified = false;
	void wakeUp()
	{
		if (notified) return;
		notified = true;
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				notified = false;
				proceedOne();
			}
		});
	}

	public void add(WorkStep step)
	{
		this.todoList.add(step);
		wakeUp();
	}
	
	public WorkStepProcessor() {
		this.todoList = new ArrayList<WorkStep>();
	}

}
