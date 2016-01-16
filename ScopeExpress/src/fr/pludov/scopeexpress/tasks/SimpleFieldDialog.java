package fr.pludov.scopeexpress.tasks;

import java.awt.*;

import javax.swing.*;
import javax.swing.text.*;

import net.miginfocom.swing.*;

/** Base pour les champs qui rentrent dans un layout nom: valeur (erreur) */
public abstract class SimpleFieldDialog<DATATYPE> implements IFieldDialog<DATATYPE> {
	final TaskParameterId<DATATYPE> id;
	final JPanel panel;
	final JLabel title;
	final JLabel error;
	final JLabel logicError;
	final IParameterEditionContext context;
	
	public SimpleFieldDialog(TaskParameterId<DATATYPE> id, IParameterEditionContext ipec) {
		this.id = id;
		this.context = ipec;
		this.panel = new JPanel();
		this.panel.setLayout(new MigLayout("ins 2", "[120px:120px:120px,trailing][grow,fill][left]", "[][]"));
		this.title = new JLabel();
		this.title.setAlignmentX(1);
		if (id.title != null) {
			this.title.setText("<html><div align=right>" + id.title+":");
		} else {
			this.title.setText(id.id + ":");
		}
		
	    {
		
	    	View view = (View) this.title.getClientProperty(
	    			javax.swing.plaf.basic.BasicHTML.propertyKey);
	    	if (view != null) {
		    	view.setSize(120,0);
			
		    	float w = view.getPreferredSpan(View.X_AXIS);
		    	float h = view.getPreferredSpan(View.Y_AXIS);
			
				Dimension d = new java.awt.Dimension((int) Math.ceil(w),
								(int) Math.ceil(h));
		    	this.title.setMinimumSize(d);
		    	this.title.setPreferredSize(d);
	    	}
		}
	
		if (id.tooltip != null) {
			panel.setToolTipText(id.tooltip);
		}
		this.panel.add(this.title, "cell 0 0");
		this.error = new JLabel();
		this.error.setText("!");
		this.error.setForeground(Color.red);
		this.error.setVisible(false);
		this.panel.add(this.error, "cell 2 0");
				
		this.logicError = new JLabel();
		this.logicError.setText(null);
		this.logicError.setForeground(Color.red);
		this.logicError.setVisible(true);
		this.panel.add(this.logicError, "cell 0 1 3 1");
	}
	
	
	
	public SimpleFieldDialog<DATATYPE> setTitleText(String text)
	{
		this.title.setText(text + ":");
		return this;
	}
	
	@Override
	public TaskParameterId<DATATYPE> getId() {
		return id;
	}

	@Override
	public abstract void set(DATATYPE value);

	@Override
	public abstract DATATYPE get();

	@Override
	public abstract boolean hasError();


	@Override
	public void setLogicError(String error) {
		this.logicError.setText(error);
	}

	@Override
	public IParameterEditionContext getContext() {
		return context;
	}
}
