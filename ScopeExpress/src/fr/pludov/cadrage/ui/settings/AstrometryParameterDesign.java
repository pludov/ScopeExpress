package fr.pludov.cadrage.ui.settings;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.JButton;
import java.awt.Font;
import java.awt.Color;

public class AstrometryParameterDesign extends JPanel {
	protected JComboBox modeSelector;
	protected JPanel panel;
	protected JPanel panel_1;
	protected JLabel lblMin;
	protected JLabel lblMax;
	protected JTextField fieldMinText;
	protected JTextField fieldMaxText;
	protected JLabel lblAd;
	protected JLabel lblDec;
	protected JLabel lblRayon;
	protected JTextField raText;
	protected JTextField decText;
	protected JTextField rayonText;
	protected JButton autoResetButton;
	protected JLabel fieldMinErrorLbl;
	protected JLabel fieldMaxErrorLbl;
	protected JLabel raErrorLbl;
	protected JLabel decErrorLbl;
	protected JLabel rayonErrorLbl;

	/**
	 * Create the panel.
	 */
	public AstrometryParameterDesign() {
		setBorder(new TitledBorder(null, "Astrom\u00E9trie", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		setLayout(new MigLayout("insets 1", "[grow][]", "[][][]"));
		
		this.modeSelector = new JComboBox();
		this.modeSelector.setToolTipText("En mode automatique, la taille du champ et la zone de recherche sont adapt\u00E9es au fil des corr\u00E9lation. Si le t\u00E9l\u00E9scope est connect\u00E9, des valeurs optimales de la position seront choisies en fonction du mouvement du t\u00E9l\u00E9scope.");
		add(this.modeSelector, "cell 0 0,growx");
		
		this.autoResetButton = new JButton("Reset");
		add(this.autoResetButton, "cell 1 0");
		
		this.panel = new JPanel();
		this.panel.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Champ de l'image", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		add(this.panel, "cell 0 1 2 1,growx,aligny top");
		this.panel.setLayout(new MigLayout("insets 1", "[][grow][][][grow][]", "[]"));
		
		this.lblMin = new JLabel("Min:");
		this.panel.add(this.lblMin, "cell 0 0,alignx trailing");
		
		this.fieldMinText = new JTextField();
		this.panel.add(this.fieldMinText, "cell 1 0,growx");
		this.fieldMinText.setColumns(10);
		
		this.fieldMinErrorLbl = new JLabel("(!)");
		this.fieldMinErrorLbl.setForeground(Color.RED);
		this.fieldMinErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 10));
		this.panel.add(this.fieldMinErrorLbl, "flowx,cell 2 0");
		
		this.lblMax = new JLabel("Max:");
		this.panel.add(this.lblMax, "cell 3 0,alignx trailing");
		
		this.fieldMaxText = new JTextField();
		this.panel.add(this.fieldMaxText, "cell 4 0,growx");
		this.fieldMaxText.setColumns(10);
		
		this.fieldMaxErrorLbl = new JLabel("(!)");
		this.fieldMaxErrorLbl.setForeground(Color.RED);
		this.fieldMaxErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 10));
		this.panel.add(this.fieldMaxErrorLbl, "cell 5 0");
		
		this.panel_1 = new JPanel();
		this.panel_1.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Zone de recherche", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		add(this.panel_1, "cell 0 2 2 1,growx,aligny top");
		this.panel_1.setLayout(new MigLayout("insets 1", "[][grow][][][grow][]", "[][]"));
		
		this.lblAd = new JLabel("AD:");
		this.panel_1.add(this.lblAd, "cell 0 0,alignx trailing");
		
		this.raText = new JTextField();
		this.panel_1.add(this.raText, "cell 1 0,growx");
		this.raText.setColumns(10);
		
		this.raErrorLbl = new JLabel("(!)");
		this.raErrorLbl.setForeground(Color.RED);
		this.raErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 10));
		this.panel_1.add(this.raErrorLbl, "cell 2 0");
		
		this.lblDec = new JLabel("Dec:");
		this.panel_1.add(this.lblDec, "cell 3 0,alignx trailing");
		
		this.decText = new JTextField();
		this.panel_1.add(this.decText, "cell 4 0,growx");
		this.decText.setColumns(10);
		
		this.decErrorLbl = new JLabel("(!)");
		this.decErrorLbl.setForeground(Color.RED);
		this.decErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 10));
		this.panel_1.add(this.decErrorLbl, "cell 5 0");
		
		this.lblRayon = new JLabel("Rayon:");
		this.panel_1.add(this.lblRayon, "cell 0 1,alignx trailing");
		
		this.rayonText = new JTextField();
		this.panel_1.add(this.rayonText, "cell 1 1 4 1,growx");
		this.rayonText.setColumns(10);
		
		this.rayonErrorLbl = new JLabel("(!)");
		this.rayonErrorLbl.setForeground(Color.RED);
		this.rayonErrorLbl.setFont(new Font("Tahoma", Font.BOLD, 10));
		this.panel_1.add(this.rayonErrorLbl, "cell 5 1");

	}

}
