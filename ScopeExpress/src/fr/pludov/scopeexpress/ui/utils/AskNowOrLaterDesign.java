package fr.pludov.scopeexpress.ui.utils;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;

import javax.swing.JCheckBox;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import java.awt.Font;

public class AskNowOrLaterDesign extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JCheckBox chckbxDontAsk;
	private JTextPane txtpn;
	private JButton yesButton;
	private JButton noButton;

	/**
	 * Create the dialog.
	 */
	public AskNowOrLaterDesign(Window parent) {
		super(parent);
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new MigLayout("", "[grow]", "[grow][]"));
		{
			txtpn = new JTextPane();
			txtpn.setFont(new Font("Tahoma", Font.PLAIN, 11));
			txtpn.setEditable(false);
			txtpn.setContentType("text/html");
			txtpn.setBackground(UIManager.getColor("Label.background"));
			txtpn.setText("Blah <b>blah</b>");
			contentPanel.add(txtpn, "cell 0 0,grow");
		}
		{
			chckbxDontAsk = new JCheckBox("Ne plus demander\u00E0 l'avenir");
			contentPanel.add(chckbxDontAsk, "cell 0 1");
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				yesButton = new JButton("Oui");
				yesButton.setActionCommand("OK");
				buttonPane.add(yesButton);
				getRootPane().setDefaultButton(yesButton);
			}
			{
				noButton = new JButton("Non");
				noButton.setActionCommand("No");
				buttonPane.add(noButton);
			}
		}
	}

	public JCheckBox getChckbxDontAsk() {
		return this.chckbxDontAsk;
	}
	public JTextPane getTxtpn() {
		return this.txtpn;
	}
	public JButton getYesButton() {
		return this.yesButton;
	}
	public JButton getNoButton() {
		return this.noButton;
	}
}
