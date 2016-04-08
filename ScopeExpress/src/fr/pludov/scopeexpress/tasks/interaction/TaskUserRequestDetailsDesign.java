package fr.pludov.scopeexpress.tasks.interaction;

import java.awt.*;

import javax.swing.*;

public class TaskUserRequestDetailsDesign extends JPanel {
	protected JLabel lblLabel;
	protected JPanel panel;
	protected JButton btnOk;
	protected JButton btnAnnuler;

	/**
	 * Create the panel.
	 */
	public TaskUserRequestDetailsDesign() {
		setLayout(new BorderLayout(0, 0));
		
		this.lblLabel = new JLabel("New label");
		add(this.lblLabel, BorderLayout.CENTER);
		
		this.panel = new JPanel();
		add(this.panel, BorderLayout.SOUTH);
		this.panel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		
		this.btnOk = new JButton("OK");
		this.panel.add(this.btnOk);
		
		this.btnAnnuler = new JButton("Annuler");
		this.panel.add(this.btnAnnuler);

	}

}
