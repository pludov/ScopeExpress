package fr.pludov.cadrage.ui.focus;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JRadioButton;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.GridLayout;
import java.awt.Window;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import javax.swing.JTextPane;

import java.awt.Color;

import javax.swing.UIManager;

public class AstroNetIndexSelectorDesign extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JRadioButton level9;
	private JRadioButton level4;
	private JRadioButton level7;
	private JRadioButton level8;
	private JRadioButton level6;
	private JRadioButton level5;
	private JButton cancelButton;
	private JButton okButton;

	/**
	 * Create the dialog.
	 */
	public AstroNetIndexSelectorDesign(Window parent) {
		super(parent);
		setBounds(100, 100, 332, 304);
		getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new MigLayout("", "[left]", "[top][][grow,center][grow,center][grow,center][grow,center][grow,center]"));
		{
			JTextPane txtpnSlectionnezLaFinesse = new JTextPane();
			txtpnSlectionnezLaFinesse.setBackground(UIManager.getColor("Button.light"));
			txtpnSlectionnezLaFinesse.setText("S\u00E9lectionnez la finesse des indexes \u00E0 utiliser. Les derniers indexes (les plus gros) ne sont requis que pour les champs petits.");
			contentPanel.add(txtpnSlectionnezLaFinesse, "flowx,cell 0 0");
		}
		{
			level9 = new JRadioButton("Niveau 9 (75 Mo)");
			contentPanel.add(level9, "cell 0 1");
		}
		{
			level8 = new JRadioButton("Niveau 8 (150 Mo)");
			contentPanel.add(level8, "cell 0 2,grow");
		}
		{
			level7 = new JRadioButton("Niveau 7 (310 Mo)");
			contentPanel.add(level7, "cell 0 3,grow");
		}
		{
			level6 = new JRadioButton("Niveau 6 (623 Mo)");
			contentPanel.add(level6, "cell 0 4");
		}
		{
			level5 = new JRadioButton("Niveau 5 (1,25 Go)");
			contentPanel.add(level5, "cell 0 5");
		}
		{
			level4 = new JRadioButton("Niveau 4 (2,44 Go)");
			contentPanel.add(level4, "cell 0 6");
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	public JRadioButton getLevel9() {
		return this.level9;
	}
	public JRadioButton getLevel4() {
		return this.level4;
	}
	public JRadioButton getLevel7() {
		return this.level7;
	}
	public JRadioButton getLevel8() {
		return this.level8;
	}
	public JRadioButton getLevel6() {
		return this.level6;
	}
	public JRadioButton getLevel5() {
		return this.level5;
	}
	public JButton getCancelButton() {
		return this.cancelButton;
	}
	public JButton getOkButton() {
		return this.okButton;
	}
}
