package fr.pludov.cadrage;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import fr.pludov.cadrage.ImageDisplayParameter.ImageDisplayMetaDataInfo;
import fr.pludov.cadrage.correlation.Correlation;
import fr.pludov.cadrage.focus.Focus;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.scope.ascom.AscomScope;
import fr.pludov.cadrage.scope.dummy.DummyScope;
import fr.pludov.cadrage.ui.CorrelationImageDisplay;
import fr.pludov.cadrage.ui.CorrelationUi;
import fr.pludov.cadrage.ui.FrameDisplay;
import fr.pludov.cadrage.ui.ImageList;
import fr.pludov.cadrage.ui.ImageListEntry;
import fr.pludov.cadrage.ui.ViewPortList;
import fr.pludov.cadrage.ui.focus.FocusImageListView;
import fr.pludov.io.CameraFrame;
import fr.pludov.io.ImageProvider;

public class MAP {

	private MAP() {
		// TODO Auto-generated constructor stub
	}

	static JFrame mainFrame;
	
	/**
	 * @param args
	 */
//	public static void main(String[] args) {
//
//		try {
//			
//			// defaultStarDetectionParameters = new StarDetectionParameters();
//			
//			// preferedStartingPoint = new File("C:\\Documents and Settings\\utilisateur\\Mes documents\\Mes images\\BackyardEOS");
//			
//			// List<ImageStar> star1 = detection.proceed(image, 18 * 1600);
//			
//			
//			final Focus focus = new Focus();
//						
//			mainFrame = new JFrame("Focus");
//			
//			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//			
//			// mainFrame.add(correlationUi.getToolBar(), BorderLayout.NORTH);
//			FocusImageListView fd = new FocusImageListView(focus);
//
//			fd.setOnClick(new FocusImageListView.ClicEvent() {
//				
//				@Override
//				public void clicked(FrameDisplay fd, int scx, int scy, double imgx, double imgy) {
////					Star star = new Star((int)imgx, (int)imgy);
////					focus.addStar(star);
//				}
//			});
//			
//			
//			mainFrame.getContentPane().add(fd);
//			mainFrame.setSize(1024, 768);
//			mainFrame.setVisible(true);
//
//			
//			// DisplayImageP;
////			ImageDisplayParameter imgDisplayParameter = new ImageDisplayParameter();
////			ImageDisplayMetaDataInfo metadataInfo = new ImageDisplayMetaDataInfo();
////			metadataInfo.expositionDuration = 1.0;
////			metadataInfo.iso = 1600;
////			
////			CameraFrame frame = ImageProvider.readImage(new File("C:\\APT_Images\\Camera_1\\2012-12-08\\l_2012-12-08_23-44-19_m31toto0007_iso1600_4s.cr2"));
////			fd.setFrame(frame.asImage(imgDisplayParameter, metadataInfo));
//////			
////			File dir = new File("C:\\APT_Images\\Camera_1\\2012-12-08\\");
////			for(String child : dir.list())
////			{
////				File file = new File(dir, child);
////				
////				fr.pludov.cadrage.focus.Image image = new fr.pludov.cadrage.focus.Image(file);
////				focus.addImage(image);
////			}
////			
//			// detection = new ImageDetection();
//			// detection.setAbsoluteAdu(aduSeuil);
//			// List<ImageStar> stars2 = detection.proceed(image, 18 * 1600 );
//			
//			// Faire des triangles avec les étoiles qui sont sensiblement de même luminosité 
//			// 
//			// Pour appairer des triangles, choisir ceux qui ont des tailles proches
//			// Pour chaque etoile de l'ensemble a, repérer celles qui sont à distance 
//			
////			Image image = correlation.addImage(new File("c:/astro/EOS 350D DIGITAL/IMG_0231.JPG"));
////			
////			AsyncOperation a1 = correlation.detectStars(image);
////			a1.queue(correlation.place(image));
////			
////			image = correlation.addImage(new File("C:/astro/EOS 350D DIGITAL/america-300-2706/IMG_0153.JPG"));
////			a1.queue(correlation.detectStars(image));
////			a1.queue(correlation.place(image));
////			
////			image = correlation.addImage(new File("c:/astro/EOS 350D DIGITAL/america300/IMG_0221.JPG"));
////			a1.queue(correlation.detectStars(image));
////			a1.queue(correlation.place(image));
////	
////			
////			
////			a1.start();
//			
//			
//			
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//		
//		
//	}

}
