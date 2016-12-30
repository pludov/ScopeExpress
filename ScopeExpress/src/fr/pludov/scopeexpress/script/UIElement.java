package fr.pludov.scopeexpress.script;

import java.awt.*;
import java.lang.ref.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.mozilla.javascript.*;

public final class UIElement {
	final TaskGroup taskGroup;
	/** Only during initialisation */
	JSTask task;
	JComponent target;
	List<DataBinder> binders = new LinkedList<>();
	
	/** Simple encapsulation */
	public UIElement(JComponent target) {
		this.target = target;
		this.task = null;
		this.taskGroup = null;
	}

	public UIElement(JSTask task) {
		this.task = task;
		this.taskGroup = task.taskGroup;
	}


	/** When the UIElement was built, record it */
	void setTarget(JComponent jc)
	{
		this.target = jc;
		// GC Friendly
		this.task = null;
		taskGroup.uiElements.add(new WeakReference<>(this));

	}
	
	boolean performBinders()
	{
		if (target == null) return false;
		
		for(Iterator<DataBinder> dbit = binders.iterator(); dbit.hasNext(); )
		{
			DataBinder db = dbit.next();
			db.perform();
		}
		return true;
	}


	public void bind(NativeFunction load, NativeFunction write)
	{
		binders.add(new DataBinder(task, load, write));
	}

	public Component getComponent() {
		return target;
	}

	public void dispose() {
		if (task != null) {
			task.taskGroup.removeUiElements(this);
		}
	}
	
}
