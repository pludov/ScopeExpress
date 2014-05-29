package fr.pludov.cadrage.ui.joystick;

import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import net.miginfocom.swing.MigLayout;
import javax.swing.JComboBox;

public class JoystickBtonConfDesign extends JPanel {
	protected JPanel panel;
	protected JComboBox actionBox;

	/**
	 * Create the panel.
	 */
	public JoystickBtonConfDesign() {
		setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "AXIS_X", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		setLayout(new MigLayout("", "[70px:n:70px][grow]", "[]"));
		
		this.panel = new JPanel();
		add(this.panel, "cell 0 0,grow");
		
		this.actionBox = new JComboBox();
		add(this.actionBox, "cell 1 0,growx");

	}

}
