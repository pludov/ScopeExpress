package fr.pludov.cadrage.ui.focus;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JSlider;

public class DistorsionViewDialogDesign extends JDialog {

	protected final JPanel contentPanel = new JPanel();
	private JSlider slider;
	private JButton okButton;
	private JButton cancelButton;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			DistorsionViewDialogDesign dialog = new DistorsionViewDialogDesign(null);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public DistorsionViewDialogDesign(Window owner) {
		super(owner);
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		{
			JPanel panel = new JPanel();
			getContentPane().add(panel, BorderLayout.CENTER);
			panel.setLayout(new BorderLayout(0, 0));
			{
				slider = new JSlider();
				panel.add(slider, BorderLayout.SOUTH);
			}
			panel.add(contentPanel, BorderLayout.CENTER);
			this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			contentPanel.setLayout(new BorderLayout(0, 0));
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				okButton = new JButton("Appliquer");
				okButton.setToolTipText("Utiliser comme correction pour les prochains calculs de correlation");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				cancelButton = new JButton("Fermer");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	public JSlider getSlider() {
		return this.slider;
	}
	public JButton getOkButton() {
		return this.okButton;
	}
	public JButton getCancelButton() {
		return this.cancelButton;
	}
}
