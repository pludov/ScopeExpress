package fr.pludov.cadrage.ui.dialogs;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.pludov.cadrage.ui.focus.ActionOpen;
import fr.pludov.cadrage.ui.preferences.BooleanConfigItem;
import fr.pludov.cadrage.ui.preferences.StringConfigItem;
import fr.pludov.cadrage.ui.utils.Utils;

public class MosaicStarter extends MosaicStarterDesign {

	public final StringConfigItem lastOpenRa;
	public final StringConfigItem lastOpenDec;
	public final StringConfigItem lastOpenRadius;
	public final StringConfigItem lastOpenMag;
	public final BooleanConfigItem lastDoWithStar;
	boolean validated;
	
	public MosaicStarter(Window owner, String prefix) {
		super(owner);
		
		lastOpenRa = new StringConfigItem(MosaicStarter.class, prefix + "_lastOpenRa", "0h0m0s");
		lastOpenDec = new StringConfigItem(MosaicStarter.class, prefix + "_lastOpenDec", "+90°");
		lastOpenRadius = new StringConfigItem(MosaicStarter.class, prefix + "_lastOpenRadius", "6°");
		lastOpenMag = new StringConfigItem(MosaicStarter.class, prefix + "_lastOpenMag", "10.5");
		lastDoWithStar = new BooleanConfigItem(MosaicStarter.class, prefix + "_lastDoWithStar", true);
		
		this.getOkButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				validated = true;
				
				lastDoWithStar.set(getDoStarCheckBox().isSelected());
				if (getDoStarCheckBox().isSelected())
				{
					lastOpenRa.set(getRaTextField().getText());
					lastOpenDec.set(getDecTextField().getText());
					lastOpenMag.set(getMagTextField().getText());
					lastOpenRadius.set(getRadiusTextField().getText());
				}
				setVisible(false);
			}
		});
		
		this.getCancelButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				validated = false;
				setVisible(false);
			}
		});
		
		this.getDoStarCheckBox().addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				validateInput();				
			}
		});
	
		getDoStarCheckBox().setSelected(lastDoWithStar.get());
		getRaTextField().setText(lastOpenRa.get());
		getDecTextField().setText(lastOpenDec.get());
		getMagTextField().setText(lastOpenMag.get());
		getRadiusTextField().setText(lastOpenRadius.get());
		
		Runnable listener = new Runnable()
		{
			@Override
			public void run() {
				validateInput();
			}
			
		};
		Utils.addTextFieldChangeListener(getRaTextField(), listener);
		Utils.addTextFieldChangeListener(getDecTextField(), listener);
		Utils.addTextFieldChangeListener(getMagTextField(), listener);
		Utils.addTextFieldChangeListener(getRadiusTextField(), listener);
		
		validateInput();
	}
	
	@Override
	public void setVisible(boolean b) {
		if (b) validated = false;
		
		super.setVisible(b);
	}
	
	private void validateInput()
	{
		boolean projectionPanelActivated = this.getDoStarCheckBox().isSelected();
		getRaTextField().setEnabled(projectionPanelActivated);
		getDecTextField().setEnabled(projectionPanelActivated);
		getRadiusTextField().setEnabled(projectionPanelActivated);
		getMagTextField().setEnabled(projectionPanelActivated);

		validateRa();
		validateDec();
		validateRadius();
		validateMag();
		
		boolean hasError = false;
		for(JLabel label : new JLabel[] {this.raErrorLbl, this.decErrorLbl, this.magErrorLbl, this.radiusErrorLbl })
		{
			if (label.isVisible()) {
				hasError = true;
			}
		}
		this.getOkButton().setEnabled(!hasError);
	}

	private void validateRa()
	{
		Exception error = null;
		if (this.getDoStarCheckBox().isSelected()) {
			try {
				Double e = getRa();
				if (e == null) throw new Exception("champ obligatoire");
			} catch(Exception e) {
				error = e;
			}
		}
		raErrorLbl.setToolTipText(error != null ? error.getMessage() : "");
		raErrorLbl.setVisible(error != null);
	}

	private void validateDec()
	{
		Exception error = null;
		if (this.getDoStarCheckBox().isSelected()) {
			try {
				Double e = getDec();
				if (e == null) throw new Exception("champ obligatoire");
			} catch(Exception e) {
				error = e;
			}
		}
		decErrorLbl.setToolTipText(error != null ? error.getMessage() : "");
		decErrorLbl.setVisible(error != null);
	}

	private void validateMag()
	{
		Exception error = null;
		if (this.getDoStarCheckBox().isSelected()) {
			try {
				Double e = getMag();
				if (e == null) throw new Exception("champ obligatoire");
			} catch(Exception e) {
				error = e;
			}
		}
		magErrorLbl.setToolTipText(error != null ? error.getMessage() : "");
		magErrorLbl.setVisible(error != null);
	}

	private void validateRadius()
	{
		Exception error = null;
		if (this.getDoStarCheckBox().isSelected()) {
			try {
				Double e = getRadius();
				if (e == null) throw new Exception("champ obligatoire");
			} catch(Exception e) {
				error = e;
			}
		}
		radiusErrorLbl.setToolTipText(error != null ? error.getMessage() : "");
		radiusErrorLbl.setVisible(error != null);
	}

	public boolean includeStar()
	{
		return this.getDoStarCheckBox().isSelected();
	}
	
	public void setRa(double ra)
	{
		String raStr = Utils.formatHourMinSec(ra);
		this.raTextField.setText(raStr);
		validateInput();
	}
	
	public void setDec(double dec)
	{
		String decStr = Utils.formatDegMinSec(dec);
		this.decTextField.setText(decStr);
		validateInput();
	}
	
	
	
	public Double getRa() throws NumberFormatException
	{
		return Utils.getDegFromInput(getRaTextField().getText());
	}
	
	public Double getDec() throws NumberFormatException
	{
		return Utils.getDegFromInput(getDecTextField().getText());
	}
	
	public Double getRadius() throws NumberFormatException
	{
		return Utils.getDegFromInput(getRadiusTextField().getText());
	}
	
	public Double getMag() throws NumberFormatException
	{
		String mag = getMagTextField().getText();
		mag = mag.trim();
		if (mag.isEmpty()) return null;
		return Double.parseDouble(mag);
	}
	
	public boolean isValidated() {
		return validated;
	}

}
