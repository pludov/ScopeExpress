package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.miginfocom.swing.*;

public class CameraControlPanelDesign extends JPanel {
	protected JProgressBar progressBar;
	protected JLabel lblTemp;
	protected JLabel lblTempValue;
	protected JLabel lblGain;
	protected JLabel lblMode;
	protected JComboBox comboMode;
	protected JComboBox comboGain;
	protected JLabel lblExp;
	protected JComboBox comboExp;
	protected JPanel panel;
	protected JButton btnShoot;
	protected JButton btnInterrupt;
	protected JButton btnConnecter;
	protected JLabel lblCameraName;

	/**
	 * Create the panel.
	 */
	public CameraControlPanelDesign() {
		setLayout(new MigLayout("", "[][grow][]", "[][center][][][][][]"));
		
		this.progressBar = new JProgressBar();
		this.progressBar.setToolTipText("");
		add(this.progressBar, "cell 0 0 3 1,growx");
		
		this.panel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) this.panel.getLayout();
		flowLayout.setVgap(0);
		flowLayout.setHgap(4);
		add(this.panel, "cell 0 1 3 1,growx,aligny center");
		
		this.btnConnecter = new JButton("Connecter");
		this.panel.add(this.btnConnecter);
		
		this.btnShoot = new JButton("Shoot");
		this.btnShoot.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		this.panel.add(this.btnShoot);
		
		this.btnInterrupt = new JButton("Interrupt");
		this.panel.add(this.btnInterrupt);
		
		this.lblExp = new JLabel("Exp:");
		add(this.lblExp, "cell 0 2,alignx trailing");
		
		this.comboExp = new JComboBox();
		this.comboExp.setEditable(true);
		add(this.comboExp, "cell 1 2 2 1,growx");
		
		this.lblGain = new JLabel("Gain:");
		add(this.lblGain, "cell 0 3,alignx trailing");
		
		this.comboGain = new JComboBox();
		this.comboGain.setEditable(true);
		add(this.comboGain, "cell 1 3 2 1,growx");
		
		this.lblMode = new JLabel("Mode:");
		add(this.lblMode, "cell 0 4,alignx trailing");
		
		this.comboMode = new JComboBox();
		add(this.comboMode, "cell 1 4 2 1,growx");
		
		this.lblTemp = new JLabel("Temp:");
		add(this.lblTemp, "cell 0 5,alignx trailing");
		
		this.lblTempValue = new JLabel("25\u00B0 (target: -5\u00B0)");
		add(this.lblTempValue, "cell 1 5");
		
		this.lblCameraName = new JLabel("New label");
		add(this.lblCameraName, "cell 0 6 2 1");

	}

}
