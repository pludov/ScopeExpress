package fr.pludov.scopeexpress.database.content;

import java.io.*;

import fr.pludov.scopeexpress.database.*;
import fr.pludov.scopeexpress.utils.*;

public class Root extends BaseDatabaseItem<Root> {
	private static final long serialVersionUID = -7951772614918908726L;

	// Peut être null
	Target currentTarget;
	public transient WeakListenerCollection<CurrentTargetListener> currentTargetListeners;
	
	DatabaseItemCollection<Root, Target> targets;
	
	public Root(Database<Root> db) {
		super(db);
		
		sanitize();
	}
	
	private void sanitize()
	{
		if (targets == null) {
			targets = new DatabaseItemCollection<>();
		}
		currentTargetListeners = new WeakListenerCollection<>(CurrentTargetListener.class);
	}

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    	in.defaultReadObject();
    	sanitize();
    }

	public DatabaseItemCollection<Root, Target> getTargets() {
		return targets;
	}
	
	public Target getCurrentTarget() {
		return currentTarget;
	}
	
	public void setCurrentTarget(Target t)
	{
		if (currentTarget == t) {
			return;
		}
		
		currentTarget = t;
		currentTargetListeners.getTarget().currentTargetChanged(t);
	}
	
	public interface CurrentTargetListener {
		void currentTargetChanged(Target newValue);
	}
	
}
