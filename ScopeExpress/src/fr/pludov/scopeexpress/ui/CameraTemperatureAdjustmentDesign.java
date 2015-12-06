package fr.pludov.scopeexpress.ui;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import javax.swing.JButton;

public class CameraTemperatureAdjustmentDesign extends JPanel {
	protected JLabel lblTemprature;
	protected JTextField textFieldTemperature;
	protected JLabel lblNewLabel;
	protected JTextField textFieldTemperatureStep;
	protected JLabel lblNewLabel_1;
	protected JTextField textFieldTimeout;
	protected JPanel btonPanel;
	protected JButton btnOk;
	protected JButton btnAnnuler;

	/**
	 * Create the panel.
	 */
	public CameraTemperatureAdjustmentDesign() {
		setLayout(new MigLayout("", "[][grow]", "[][][][]"));
		
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
		
		this.btonPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) this.btonPanel.getLayout();
		flowLayout.setAlignment(FlowLayout.RIGHT);
		add(this.btonPanel, "cell 0 3 2 1,growx,aligny top");
		
		this.btnOk = new JButton("OK");
		this.btonPanel.add(this.btnOk);
		
		this.btnAnnuler = new JButton("Annuler");
		this.btonPanel.add(this.btnAnnuler);

	}

}
