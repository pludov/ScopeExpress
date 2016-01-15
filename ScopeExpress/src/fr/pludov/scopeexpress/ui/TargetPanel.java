package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import fr.pludov.scopeexpress.database.content.*;
import fr.pludov.scopeexpress.ui.utils.*;

/**
 * Montre les détails d'une cible et permet de les éditer
 * Calcul les suggestion à chaque changement de RA/DEC
 */
public class TargetPanel extends TargetPanelDesign {

	// L'objet qui va être modifié lors de save
	Target target;
	// La copie de travail
	Target workingCopy;

	boolean hasError;
	
	public TargetPanel() {
		super();
		this.nameCombo.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		
		Utils.addComboChangeListener(this.nameCombo, new Runnable() {
			@Override
			public void run() {
				updateWorkingFromForm();
			}
		});
		
		Utils.addTextFieldChangeListener(this.raTextField, new Runnable() {
			@Override
			public void run() {
				updateWorkingFromForm();
				updateProposals();
			}
		});
		this.raTextField.setToolTipText(Utils.hourMinSecTooltip);
		
		Utils.addTextFieldChangeListener(this.decTextField, new Runnable() {
			@Override
			public void run() {
				updateWorkingFromForm();
				updateProposals();
			}
		});
		this.decTextField.setToolTipText(Utils.degMinSecTooltip);
	}

	class DialogErrorBuilder {
		
		
		private JLabel [] labels;
		private boolean [] wasSet;
		
		DialogErrorBuilder(JLabel ... labels)
		{
			this.labels = labels;
			wasSet = new boolean[labels.length];
		}
		
		void set(JLabel jbl, String message)
		{
			for(int i = 0; i < labels.length; ++i) {
				if (labels[i] == jbl) {
					if (!wasSet[i]) {
						jbl.setVisible(true);
						jbl.setToolTipText(message);
						wasSet[i] = true;
					}
					break;
				}
			}
		}
		
		public void done()
		{
			for(int i = 0; i < labels.length; ++i) {
				if (!wasSet[i]) {
					labels[i].setVisible(false);
					labels[i].setToolTipText("");
				}
			}
		}
		
		public boolean hasError()
		{
			for(int i = 0; i < labels.length; ++i) {
				if (wasSet[i]) {
					return true;
				}
			}
			return false;
		}
	}
	
	DialogErrorBuilder getErrorBuilder()
	{
		return new DialogErrorBuilder(this.lblRaErr, this.lblDecErr, this.lblNomError);
	}
	
	int updateDisabled = 0;
	
	/** Copie vers workingCopy et remonte les erreurs au fur et à mesure */
	void updateWorkingFromForm()
	{
		if (updateDisabled > 0) return;
		
		DialogErrorBuilder error = getErrorBuilder();
		
		String wName = (String)this.nameCombo.getSelectedItem();
		if (wName == null || "".equals(wName)) {
			error.set(this.lblNomError, "Valeur obligatoire");
		} else {
			workingCopy.setName(wName);
		}
		
		String ra = (String)this.raTextField.getText();
		if (ra.equals("")) {
			workingCopy.setRa(null);
		} else {
			try {
				workingCopy.setRa(Utils.getDegFromHourMinSec(ra));
			} catch(NumberFormatException e) {
				error.set(this.lblRaErr, "Valeur invalide: " + e.getLocalizedMessage());
				workingCopy.setRa(Double.NaN);
			}
		}
		
		String dec = (String)this.decTextField.getText();
		if (dec.equals("")) {
			workingCopy.setDec(null);
		} else {
			try {
				workingCopy.setDec(Utils.getDegFromDegMinSec(dec));
			} catch(NumberFormatException e) {
				error.set(this.lblDecErr, "Valeur invalide: " + e.getLocalizedMessage());
				workingCopy.setDec(Double.NaN);
			}
		}

		if ((workingCopy.getRa() != null) != (workingCopy.getDec() != null)) {
			if (workingCopy.getRa() == null) {
				error.set(this.lblRaErr, "Obligatoire");
			} else {
				error.set(this.lblDecErr, "Obligatoire");
			}
		}
		error.done();
		hasError = error.hasError();
	}
	


	double degDistForHint = 1.0;
	private void setDegDistForHint(double degDistForHint) {
		this.degDistForHint = degDistForHint;
	}
	
	Double lastPropRa, lastPropDec;
	
	void updateProposals()
	{
		if (Objects.equals(this.lastPropRa, this.workingCopy.getRa())
				&& Objects.equals(this.lastPropDec, this.workingCopy.getDec()))
		{
			return;
		}
		lastPropRa = this.workingCopy.getRa();
		lastPropDec = this.workingCopy.getDec();
		
		Object previous = this.nameCombo.getSelectedItem();
		
		this.nameCombo.removeAllItems();
		
		for(String item : workingCopy.findPossibleNames(degDistForHint)) {
			this.nameCombo.addItem(item);
		}
		if (previous != null && !"".equals(previous)) {
			this.nameCombo.setSelectedItem(previous);
		}
	}
	
	boolean save()
	{
		updateWorkingFromForm();
		if (hasError) return false;
		target.setName(workingCopy.getName());
		target.setRa(workingCopy.getRa());
		target.setDec(workingCopy.getDec());
		return true;
		
	}
	
	void load(Target target)
	{
		this.target = target;
		workingCopy = new Target(target);
		
		updateDisabled++;
		try {
			
			this.nameCombo.removeAllItems();
			this.nameCombo.setSelectedItem(this.target.getName());
			
			if (workingCopy.getRa() != null) {
				this.raTextField.setText(Utils.formatHourMinSec(workingCopy.getRa()));
			} else {
				this.raTextField.setText("");
			}
			
			if (workingCopy.getDec() != null) {
				this.decTextField.setText(Utils.formatDegMinSec(workingCopy.getDec()));
			} else {
				this.decTextField.setText("");
			}
		} finally {
			updateDisabled --;
		}
		updateProposals();
		getErrorBuilder().done();
		hasError = false;		
	}

	public static void createNewDialog(final FocusUi focusUi, final Target model, double degDistForHint)
	{
		final JDialog jd = new JDialog(focusUi.getFrmFocus());
		jd.getContentPane().setLayout(new BorderLayout());
		jd.setTitle("Nouvelle cible");
		final TargetPanel tp = new TargetPanel();
		tp.setDegDistForHint(degDistForHint);
		jd.add(tp);
		Utils.addDialogButton(jd, new Runnable() {
			@Override
			public void run() {
				if (tp.save()) {
					jd.setVisible(false);
					
					focusUi.database.getRoot().getTargets().add(model);
					focusUi.database.getRoot().setCurrentTarget(model);

					focusUi.database.asyncSave();
				}
			}
		});
		jd.setResizable(false);
		jd.pack();
		jd.setLocationRelativeTo(jd.getParent());
		Utils.autoDisposeDialog(jd);
		
		tp.load(model);
		jd.setVisible(true);
		
	}
	

	public static void createModifyDialog(final FocusUi focusUi, final Target model, double degDistForHint)
	{
		final JDialog jd = new JDialog(focusUi.getFrmFocus());
		jd.getContentPane().setLayout(new BorderLayout());
		jd.setTitle("Détails de la cible: " + model.getName());
		final TargetPanel tp = new TargetPanel();
		tp.setDegDistForHint(degDistForHint);
		jd.add(tp);
		Utils.addDialogButton(jd, new Runnable() {
			@Override
			public void run() {
				if (tp.save()) {
					jd.setVisible(false);
					focusUi.database.asyncSave();
				}
			}
		});
		jd.setResizable(false);
		jd.pack();
		jd.setLocationRelativeTo(jd.getParent());
		Utils.autoDisposeDialog(jd);
		
		tp.load(model);
		jd.setVisible(true);
		
	}
	
}
