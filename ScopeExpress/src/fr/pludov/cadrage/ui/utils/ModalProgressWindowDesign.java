package fr.pludov.cadrage.ui.utils;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.miginfocom.swing.MigLayout;
import javax.swing.JProgressBar;
import javax.swing.JLabel;

public class ModalProgressWindowDesign extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JButton okButton;
	private JButton cancelButton;
	private JProgressBar progressBar;
	private JLabel progressLabel;

	/**
	 * Create the dialog.
	 */
	public ModalProgressWindowDesign(Window owner) {
		super(owner);
		setBounds(100, 100, 450, 135);
		getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new MigLayout("", "[grow]", "[][grow]"));
		{
			progressBar = new JProgressBar();
			progressBar.setValue(50);
			contentPanel.add(progressBar, "cell 0 0,growx");
		}
		{
			progressLabel = new JLabel("none");
			contentPanel.add(progressLabel, "cell 0 1,alignx left,aligny center");
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

	public JButton getOkButton() {
		return this.okButton;
	}
	public JButton getCancelButton() {
		return this.cancelButton;
	}
	public JProgressBar getProgressBar() {
		return this.progressBar;
	}
	public JLabel getProgressLabel() {
		return this.progressLabel;
	}
}
