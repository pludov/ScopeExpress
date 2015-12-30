package fr.pludov.scopeexpress.ui;

import javax.swing.*;

import net.miginfocom.swing.*;

public class CameraTemperatureAdjustmentDesign extends JPanel {
	protected JLabel lblTemprature;
	protected JTextField textFieldTemperature;
	protected JLabel lblNewLabel;
	protected JTextField textFieldTemperatureStep;
	protected JLabel lblNewLabel_1;
	protected JTextField textFieldTimeout;

	/**
	 * Create the panel.
	 */
	public CameraTemperatureAdjustmentDesign() {
		setLayout(new MigLayout("", "[][grow]", "[][][]"));
		
		this.lblTemprature = new JLabel("Temp\u00E9rature cible:");
		add(this.lblTemprature, "cell 0 0,alignx trailing");
		
		this.textFieldTemperature = new JTextField();
		add(this.textFieldTemperature, "cell 1 0,growx");
		this.textFieldTemperature.setColumns(10);
		
		this.lblNewLabel = new JLabel("Pas en \u00B0C:");
		add(this.lblNewLabel, "cell 0 1,alignx trailing");
		
		this.textFieldTemperatureStep = new JTextField();
		add(this.textFieldTemperatureStep, "cell 1 1,growx");
		this.textFieldTemperatureStep.setColumns(10);
		
		this.lblNewLabel_1 = new JLabel("D\u00E9lai maxi (s):");
		add(this.lblNewLabel_1, "cell 0 2,alignx trailing");
		
		this.textFieldTimeout = new JTextField();
		add(this.textFieldTimeout, "cell 1 2,growx");
		this.textFieldTimeout.setColumns(10);

	}

}
