package fr.pludov.scopeexpress.ui;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import java.awt.Font;

public class StarDetailDesign extends JPanel {
	protected JPanel graphPanel;
	protected JLabel lblTxtFwhm;
	protected JLabel lblTxtStddev;
	protected JLabel lblTxtAduMax;
	protected JLabel lblTxtAduTotal;
	protected JLabel lblFWHM;
	protected JLabel lblStdDev;
	protected JLabel lblValAduMaxGrey;
	protected JLabel lblValAduSumGrey;
	protected JLabel lblBlack;
	protected JLabel lblValBlackGrey;
	protected JLabel lblGrey;
	protected JLabel lblRed;
	protected JLabel lblGreen;
	protected JLabel lblBlue;
	protected JLabel lblValBlackRed;
	protected JLabel lblValBlackGreen;
	protected JLabel lblValBlackBlue;
	protected JLabel lblValAduMaxRed;
	protected JLabel lblValAduMaxGreen;
	protected JLabel lblValAduMaxBlue;
	protected JLabel lblValAduSumRed;
	protected JLabel lblValAduSumGreen;
	protected JLabel lblValAduSumBlue;
	protected JLabel lblPosimg;
	protected JLabel lblX;
	protected JLabel lblNewLabel;
	protected JLabel lblPosXImage;
	protected JLabel lblPosYImage;
	protected JLabel lblTxtIdent;
	protected JLabel lblIdent;

	/**
	 * Create the panel.
	 */
	public StarDetailDesign() {
		setLayout(new MigLayout("", "[right][grow][grow][grow][grow]", "[120px:280px,grow,fill][][][][][][][][]"));
		
		this.graphPanel = new JPanel();
		add(this.graphPanel, "cell 0 0 5 1,grow");
		this.graphPanel.setLayout(new BoxLayout(this.graphPanel, BoxLayout.X_AXIS));
		
		this.lblTxtFwhm = new JLabel("FWHM:");
		add(this.lblTxtFwhm, "cell 0 1");
		
		this.lblFWHM = new JLabel("New label");
		this.lblFWHM.setFont(new Font("Tahoma", Font.PLAIN, 10));
		add(this.lblFWHM, "cell 1 1 4 1");
		
		this.lblTxtStddev = new JLabel("STDDEV:");
		add(this.lblTxtStddev, "cell 0 2");
		
		this.lblStdDev = new JLabel("New label");
		this.lblStdDev.setFont(new Font("Tahoma", Font.PLAIN, 10));
		add(this.lblStdDev, "cell 1 2 4 1");
		
		this.lblTxtIdent = new JLabel("Ident:");
		add(this.lblTxtIdent, "cell 0 3");
		
		this.lblIdent = new JLabel("lblIdent");
		this.lblIdent.setFont(new Font("Tahoma", Font.PLAIN, 10));
		add(this.lblIdent, "cell 1 3 4 1");
		
		this.lblGrey = new JLabel("Grey");
		add(this.lblGrey, "cell 1 4");
		
		this.lblRed = new JLabel("Red");
		add(this.lblRed, "cell 2 4");
		
		this.lblGreen = new JLabel("Green");
		add(this.lblGreen, "cell 3 4");
		
		this.lblBlue = new JLabel("Blue");
		add(this.lblBlue, "cell 4 4");
		
		this.lblBlack = new JLabel("Black:");
		add(this.lblBlack, "cell 0 5");
		
		this.lblValBlackGrey = new JLabel("New label");
		this.lblValBlackGrey.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValBlackGrey, "cell 1 5");
		
		this.lblValBlackRed = new JLabel("New label");
		this.lblValBlackRed.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValBlackRed, "cell 2 5");
		
		this.lblValBlackGreen = new JLabel("New label");
		this.lblValBlackGreen.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValBlackGreen, "cell 3 5");
		
		this.lblValBlackBlue = new JLabel("New label");
		this.lblValBlackBlue.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValBlackBlue, "cell 4 5");
		
		this.lblTxtAduMax = new JLabel("ADU Max:");
		add(this.lblTxtAduMax, "cell 0 6");
		
		this.lblValAduMaxGrey = new JLabel("New label");
		this.lblValAduMaxGrey.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduMaxGrey, "cell 1 6");
		
		this.lblValAduMaxRed = new JLabel("New label");
		this.lblValAduMaxRed.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduMaxRed, "cell 2 6");
		
		this.lblValAduMaxGreen = new JLabel("New label");
		this.lblValAduMaxGreen.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduMaxGreen, "cell 3 6");
		
		this.lblValAduMaxBlue = new JLabel("New label");
		this.lblValAduMaxBlue.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduMaxBlue, "cell 4 6");
		
		this.lblTxtAduTotal = new JLabel("ADU Total:");
		add(this.lblTxtAduTotal, "cell 0 7");
		
		this.lblValAduSumGrey = new JLabel("New label");
		this.lblValAduSumGrey.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduSumGrey, "cell 1 7");
		
		this.lblValAduSumRed = new JLabel("New label");
		this.lblValAduSumRed.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduSumRed, "cell 2 7");
		
		this.lblValAduSumGreen = new JLabel("New label");
		this.lblValAduSumGreen.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduSumGreen, "cell 3 7");
		
		this.lblValAduSumBlue = new JLabel("New label");
		this.lblValAduSumBlue.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblValAduSumBlue, "cell 4 7");
		
		this.lblPosimg = new JLabel("Pos (img)");
		add(this.lblPosimg, "cell 0 8");
		
		this.lblX = new JLabel("x:");
		this.lblX.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblX, "cell 1 8,alignx right");
		
		this.lblPosXImage = new JLabel("New label");
		this.lblPosXImage.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblPosXImage, "cell 2 8");
		
		this.lblNewLabel = new JLabel("y:");
		this.lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblNewLabel, "cell 3 8,alignx right");
		
		this.lblPosYImage = new JLabel("New label");
		this.lblPosYImage.setFont(new Font("Tahoma", Font.PLAIN, 9));
		add(this.lblPosYImage, "cell 4 8");

	}
}
