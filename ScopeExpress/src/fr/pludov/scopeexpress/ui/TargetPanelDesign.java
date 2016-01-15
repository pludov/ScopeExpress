package fr.pludov.scopeexpress.ui;

import java.awt.*;

import javax.swing.*;

import net.miginfocom.swing.*;

public class TargetPanelDesign extends JPanel {
	protected JLabel lblNom;
	protected JComboBox nameCombo;
	protected JLabel lblRa;
	protected JLabel lblDec;
	protected JTextField raTextField;
	protected JTextField decTextField;
	protected JLabel lblRaErr;
	protected JLabel lblDecErr;
	protected JLabel lblNomError;

	/**
	 * Create the panel.
	 */
	public TargetPanelDesign() {
		setLayout(new MigLayout("", "[][grow]", "[][][][][]"));
		
		this.lblNom = new JLabel("Nom:");
		add(this.lblNom, "cell 0 0,alignx trailing");
		
		this.nameCombo = new JComboBox();
		this.nameCombo.setEditable(true);
		add(this.nameCombo, "flowx,cell 1 0,growx");
		
		this.lblRa = new JLabel("AD:");
		add(this.lblRa, "cell 0 1,alignx trailing");
		
		this.raTextField = new JTextField();
		add(this.raTextField, "flowx,cell 1 1,growx");
		this.raTextField.setColumns(10);
		
		this.lblDec = new JLabel("DEC:");
		add(this.lblDec, "cell 0 2,alignx trailing");
		
		this.decTextField = new JTextField();
		add(this.decTextField, "flowx,cell 1 2,growx");
		this.decTextField.setColumns(10);
		
		this.lblRaErr = new JLabel("!!!");
		this.lblRaErr.setForeground(Color.RED);
		this.lblRaErr.setFont(new Font("Tahoma", Font.BOLD, 11));
		add(this.lblRaErr, "cell 1 1");
		
		this.lblDecErr = new JLabel("!!!");
		this.lblDecErr.setForeground(Color.RED);
		this.lblDecErr.setFont(new Font("Tahoma", Font.BOLD, 11));
		add(this.lblDecErr, "cell 1 2");
		
		this.lblNomError = new JLabel("!!!");
		this.lblNomError.setForeground(Color.RED);
		this.lblNomError.setFont(new Font("Tahoma", Font.BOLD, 11));
		add(this.lblNomError, "cell 1 0");

	}

}
