package fr.pludov.cadrage;

import java.awt.Dimension;
import java.awt.Panel;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.plaf.multi.MultiSplitPaneUI;

import fr.pludov.cadrage.async.AsyncOperation;
import fr.pludov.cadrage.ui.CorrelationImageDisplay;
import fr.pludov.cadrage.ui.ImageList;

public class Cadrage {

	public static StarDetection defaultParameter;
	public static JFrame mainFrame;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			
			// BufferedImage image;
			
			// image = ImageIO.read(new File("c:/astro/EOS 350D DIGITAL/IMG_0231.JPG"));
			
			double aduSeuil = 0.25;
			
			defaultParameter = new StarDetection();
			defaultParameter.setAbsoluteAdu(aduSeuil);
			defaultParameter.setBinFactor(2);
			
			// List<ImageStar> star1 = detection.proceed(image, 18 * 1600);
			
			
			
			
			Correlation correlation = new Correlation();
			
			mainFrame = new JFrame("Display image");
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			 
			ImageList table = new ImageList(correlation);
	        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
	        table.setFillsViewportHeight(true);

	        //Create the scroll pane and add the table to it.
	        JScrollPane scrollPane = new JScrollPane(table);
			
	        CorrelationImageDisplay display = new CorrelationImageDisplay(correlation, table);
	        display.setVisible(true);
	        
	        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
	        		display, scrollPane);
	        splitPane.setResizeWeight(1.0);
	        Dimension minimumSize = new Dimension(100, 50);
	        display.setMinimumSize(minimumSize);
	        scrollPane.setMinimumSize(minimumSize);
	        
	        
			// ShowImage panel = new ShowImage();
			// panel.setImage(image);
			mainFrame.getContentPane().add(splitPane);
			mainFrame.setSize(500, 500);
			mainFrame.setVisible(true);

			// detection = new ImageDetection();
			// detection.setAbsoluteAdu(aduSeuil);
			// List<ImageStar> stars2 = detection.proceed(image, 18 * 1600 );
			
			// Faire des triangles avec les étoiles qui sont sensiblement de même luminosité 
			// 
			// Pour appairer des triangles, choisir ceux qui ont des tailles proches
			// Pour chaque etoile de l'ensemble a, repérer celles qui sont à distance 
			
			Image image = correlation.addImage(new File("c:/astro/EOS 350D DIGITAL/IMG_0231.JPG"));
			
			AsyncOperation a1 = correlation.detectStars(image);
			a1.queue(correlation.place(image));
			
			image = correlation.addImage(new File("C:/astro/EOS 350D DIGITAL/america-300-2706/IMG_0153.JPG"));
			a1.queue(correlation.detectStars(image));
			a1.queue(correlation.place(image));
			
			image = correlation.addImage(new File("c:/astro/EOS 350D DIGITAL/america300/IMG_0221.JPG"));
			a1.queue(correlation.detectStars(image));
			a1.queue(correlation.place(image));
			
			
			
			a1.start();
			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
