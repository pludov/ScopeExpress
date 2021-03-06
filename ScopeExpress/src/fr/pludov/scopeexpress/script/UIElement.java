package fr.pludov.scopeexpress.script;

import java.awt.*;
import java.awt.event.*;
import java.lang.ref.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.tasks.javascript.*;
import fr.pludov.scopeexpress.ui.utils.Utils;

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
	
	private void triggerEvent(Scriptable eventScope, Object target, String event, Object ... args)
	{
		if (target instanceof NativeFunction) {
			taskGroup.enqueueEvent(eventScope, (NativeFunction)target, event, args);
		} else if (target instanceof DataBinder){
			// On veut un scope javascript (neuf !)
			try(JSContext jsc = JSContext.open(((DataBinder)target).jsTask.modules.getContextFactory())) {
				((DataBinder)target).forceUpdate();
			}
		}
	}

	public void on(JComponent component, String event, Object evt)
	{
		final Scriptable eventScope = task.scope;
		// Ajoute un listener sur le component... Tant qu'il est l� !
		if ("click".equals(event)) {
			if (component instanceof JButton) {
				((JButton)component).addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						triggerEvent(eventScope, evt, event, component, e);
					}
				});
				return;
			}
		}
		if ("change".equals(event)) {
			if (component instanceof JTextField) {
				JTextField textField = (JTextField) component;
				Utils.addTextFieldChangeListener(textField, new Runnable() {
					@Override
					public void run() {
						triggerEvent(eventScope, evt, event, textField.getText());
					}
				});
				return;
			}
		}
		if ("resize".equals(event)) {
			component.addComponentListener(new ComponentListener() {
				@Override
				public void componentResized(ComponentEvent e) {
					triggerEvent(eventScope, evt, event);
				}

				@Override
				public void componentMoved(ComponentEvent e) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void componentShown(ComponentEvent e) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void componentHidden(ComponentEvent e) {
					// TODO Auto-generated method stub
					
				}
			});
			return;
		}
		throw Context.reportRuntimeError("unsupported event for object");
	}

	public DataBinder bind(NativeFunction load, NativeFunction write)
	{
		DataBinder result; 
		binders.add(result = new DataBinder(task, load, write));
		return result;
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
