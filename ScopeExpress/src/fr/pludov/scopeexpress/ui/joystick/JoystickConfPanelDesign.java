package fr.pludov.scopeexpress.ui.joystick;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import java.awt.BorderLayout;

public class JoystickConfPanelDesign extends JPanel {
	protected JScrollPane controllerDetailsScroll;
	protected JPanel controllerDetails;
	protected JButton btnRescan;

	/**
	 * Create the panel.
	 */
	public JoystickConfPanelDesign() {
		setLayout(new MigLayout("", "[grow,center]", "[][grow]"));
		
		this.btnRescan = new JButton("Rescan");
		add(this.btnRescan, "cell 0 0,alignx center,aligny center");
		
		this.controllerDetailsScroll = new JScrollPane();
		add(this.controllerDetailsScroll, "cell 0 1 2 1,grow");
		
		this.controllerDetails = new JPanel();
		this.controllerDetailsScroll.setViewportView(this.controllerDetails);
		this.controllerDetails.setLayout(new BorderLayout(0, 0));

	}
}
