package fr.pludov.cadrage.ui.focus;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JComboBox;

public class LocateStarParameterPanelDesign extends JPanel {
	protected JPanel panel;
	protected JPanel panel_1;
	protected JLabel label;
	protected JTextField txtBlackLevel;
	protected JLabel label_1;
	protected JTextField txtAduMin;
	protected JLabel label_2;
	protected JLabel label_3;
	protected JLabel label_4;
	protected JComboBox comboSearchMode;
	protected JComboBox comboImageRef;

	/**
	 * Create the panel.
	 */
	public LocateStarParameterPanelDesign() {
		setLayout(new MigLayout("", "[grow]", "[grow][grow]"));
		
		this.panel = new JPanel();
		this.panel.setToolTipText("");
		this.panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "D\u00E9tection", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		add(this.panel, "cell 0 0,grow");
		this.panel.setLayout(new MigLayout("", "[][grow]", "[][]"));
		
		this.label = new JLabel("Evaluation du noir:");
		this.label.setToolTipText("Pourcentage de pixels utilis\u00E9s dans le calcul du noir.");
		this.panel.add(this.label, "cell 0 0,alignx trailing");
		
		this.txtBlackLevel = new JTextField();
		this.txtBlackLevel.setColumns(10);
		this.panel.add(this.txtBlackLevel, "flowx,cell 1 0,growx");
		
		this.label_1 = new JLabel("Minimum pour la somme d'ADU:");
		this.label_1.setHorizontalAlignment(SwingConstants.TRAILING);
		this.panel.add(this.label_1, "cell 0 1,alignx trailing");
		
		this.txtAduMin = new JTextField();
		this.txtAduMin.setColumns(10);
		this.panel.add(this.txtAduMin, "cell 1 1,growx");
		
		this.label_2 = new JLabel("%");
		this.panel.add(this.label_2, "cell 1 0");
		
		this.panel_1 = new JPanel();
		this.panel_1.setToolTipText("Comment les \u00E9toiles trouv\u00E9es sont li\u00E9es aux \u00E9toiles d\u00E9j\u00E0 connues");
		this.panel_1.setBorder(new TitledBorder(null, "Corr\u00E9lation", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		add(this.panel_1, "cell 0 1,grow");
		this.panel_1.setLayout(new MigLayout("", "[][grow]", "[][]"));
		
		this.label_3 = new JLabel("Mode de recherche:");
		this.panel_1.add(this.label_3, "cell 0 0,alignx trailing");
		
		this.comboSearchMode = new JComboBox();
		this.panel_1.add(this.comboSearchMode, "cell 1 0,growx");
		
		this.label_4 = new JLabel("Image de r\u00E9f\u00E9rence:");
		this.panel_1.add(this.label_4, "cell 0 1,alignx trailing");
		
		this.comboImageRef = new JComboBox();
		this.panel_1.add(this.comboImageRef, "cell 1 1,growx");

	}

}
