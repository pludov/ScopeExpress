package fr.pludov.scopeexpress.ui.settings;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import javax.swing.JCheckBox;
import java.awt.Insets;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JComboBox;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JScrollBar;
import net.miginfocom.swing.MigLayout;

public class ImageDisplayParameterPanelDesign extends JPanel {
	protected JLabel lblMode;
	protected JComboBox channelModeCombo;
	protected JCheckBox autoHistogramCheckBox;
	protected JLabel lblAutomatique;
	protected JLabel lblHistogramme;
	protected JPanel levelPanel;
	protected JLabel lblDark;
	protected JTextField darkTextBox;
	protected JCheckBox darkEnabledCheckbox;
	protected JPanel panel;
	protected JScrollBar scrollBar;
	protected JPanel histoPanel;

	/**
	 * Create the panel.
	 */
	public ImageDisplayParameterPanelDesign() {
		setLayout(new MigLayout("insets 1", "[][][60px,grow 30][160px,grow 70]", "[grow,center][grow,center][grow,center][center]"));
		
		this.lblMode = new JLabel("Mode :");
		add(this.lblMode, "cell 0 0,alignx right,aligny center");
		
		this.channelModeCombo = new JComboBox();
		add(this.channelModeCombo, "cell 1 0 2 1,growx,aligny center");
		
		this.histoPanel = new JPanel();
		add(this.histoPanel, "cell 3 0 1 3,grow");
		this.histoPanel.setLayout(new BorderLayout(0, 0));
		
		this.lblDark = new JLabel("Dark :");
		add(this.lblDark, "cell 0 1,alignx right,aligny center");
		
		this.darkEnabledCheckbox = new JCheckBox("");
		add(this.darkEnabledCheckbox, "cell 1 1,alignx center,aligny center");
		
		this.darkTextBox = new JTextField();
		this.darkTextBox.setEditable(false);
		this.darkTextBox.setEnabled(false);
		add(this.darkTextBox, "cell 2 1,growx,aligny center");
		this.darkTextBox.setColumns(10);
		
		this.lblHistogramme = new JLabel("Histogramme :");
		add(this.lblHistogramme, "cell 0 2,alignx right,aligny center");
		
		this.autoHistogramCheckBox = new JCheckBox("");
		add(this.autoHistogramCheckBox, "cell 1 2,alignx center,aligny center");
		
		this.lblAutomatique = new JLabel("Automatique");
		add(this.lblAutomatique, "cell 2 2,alignx left,aligny center");
		
		this.panel = new JPanel();
		add(this.panel, "cell 0 3 4 1,grow");
		this.panel.setLayout(new MigLayout("insets 1", "[grow,fill][][][]", "[][]"));
		
		this.levelPanel = new JPanel();
		this.panel.add(this.levelPanel, "cell 0 0 4 1");
		GridBagLayout gbl_levelPanel = new GridBagLayout();
		gbl_levelPanel.columnWidths = new int[]{0};
		gbl_levelPanel.rowHeights = new int[]{0};
		gbl_levelPanel.columnWeights = new double[]{Double.MIN_VALUE};
		gbl_levelPanel.rowWeights = new double[]{Double.MIN_VALUE};
		this.levelPanel.setLayout(gbl_levelPanel);
		
		this.scrollBar = new JScrollBar();
		this.scrollBar.setUnitIncrement(50);
		this.scrollBar.setBlockIncrement(50);
		this.scrollBar.setOrientation(JScrollBar.HORIZONTAL);
		this.panel.add(this.scrollBar, "cell 0 1,alignx center");

	}

}
