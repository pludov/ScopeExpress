package fr.pludov.cadrage.ui.dialogs;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.pludov.cadrage.ui.focus.ActionOpen;
import fr.pludov.cadrage.ui.preferences.BooleanConfigItem;
import fr.pludov.cadrage.ui.preferences.StringConfigItem;

public class MosaicStarter extends MosaicStarterDesign {

	public final StringConfigItem lastOpenRa = new StringConfigItem(MosaicStarter.class, "lastOpenRa", "0h0m0s");
	public final StringConfigItem lastOpenDec = new StringConfigItem(MosaicStarter.class, "lastOpenDec", "+90°");
	public final StringConfigItem lastOpenRadius = new StringConfigItem(MosaicStarter.class, "lastOpenRadius", "6°");
	public final StringConfigItem lastOpenMag = new StringConfigItem(MosaicStarter.class, "lastOpenMag", "10.5");
	public final BooleanConfigItem lastDoWithStar = new BooleanConfigItem(MosaicStarter.class, "lastDoWithStar", true);
	boolean validated;
	
	public MosaicStarter(Window owner) {
		super(owner);
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

	private Double getDegFromInput(String input) throws NumberFormatException
	{
		// Pattern hms = Pattern.compile("\\s*(\\d+)\\s*h(?:|\\s*(\\d+)\\s*m(?:|\\s*(\\d+\\.\\d+|\\d+)\\s*s))\\s*");
		Pattern hms = Pattern.compile("\\s*(?:(\\d+|\\d+\\.\\d*)\\s*h|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*m|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*s|)\\s*");
		
		Matcher hmsMatcher = hms.matcher(input);
		if (hmsMatcher.matches() && (hmsMatcher.group(1) != null || hmsMatcher.group(2) != null || hmsMatcher.group(3) != null)) {
			double result = 0;
			if (hmsMatcher.group(1) != null) {
				double h = Double.parseDouble(hmsMatcher.group(1));
				result += 360 * h / 24;
			}
			if (hmsMatcher.group(2) != null) {
				double m = Double.parseDouble(hmsMatcher.group(2));
				result += 360 * m / (60 * 24);
			}

			if (hmsMatcher.group(3) != null) {
				double s = Double.parseDouble(hmsMatcher.group(3));
				result += 360 * s / (60 * 60 * 24);
			}
			
			return result;
		}

		Pattern deg = Pattern.compile("\\s*(\\+|\\-|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*°|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*'|)\\s*(?:(\\d+|\\d+\\.\\d*)\\s*(?:''|\")|)\\s*");
		Matcher degMatcher = deg.matcher(input);
		if (degMatcher.matches() && (degMatcher.group(2) != null || degMatcher.group(3) != null || degMatcher.group(4) != null))
		{
			double result = 0;
			if (degMatcher.group(2) != null) {
				double d = Double.parseDouble(degMatcher.group(2));
				result += d;
			}
			if (degMatcher.group(3) != null) {
				double m = Double.parseDouble(degMatcher.group(3));
				result += m / (60);
			}

			if (degMatcher.group(4) != null) {
				double s = Double.parseDouble(degMatcher.group(4));
				result += s / (60 * 60);
			}
			
			if (degMatcher.group(1) != null && degMatcher.group(1).equals("-")) {
				result = -result;
			}
			return result;
		}
		
		return null;
	}
	
	public boolean includeStar()
	{
		return this.getDoStarCheckBox().isSelected();
	}
	
	public Double getRa() throws NumberFormatException
	{
		return getDegFromInput(getRaTextField().getText());
	}
	
	public Double getDec() throws NumberFormatException
	{
		return getDegFromInput(getDecTextField().getText());
	}
	
	public Double getRadius() throws NumberFormatException
	{
		return getDegFromInput(getRadiusTextField().getText());
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
