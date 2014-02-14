package fr.pludov.cadrage.ui.focus;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.focus.Application;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.MosaicImageParameter;
import fr.pludov.cadrage.focus.MosaicListener.ImageAddedCause;
import fr.pludov.cadrage.focus.SkyProjection;
import fr.pludov.cadrage.tests.TestInstance;
import fr.pludov.cadrage.tests.TestRunner;
import fr.pludov.cadrage.ui.utils.BackgroundTaskQueueListener;
import fr.pludov.cadrage.utils.WeakListenerOwner;

public class StarCorrelationTest implements TestInstance {
	private static final Logger logger = Logger.getLogger(StarCorrelationTest.class);

	final TestRunner runner;
	final File imageFile;
	
	Mosaic mosaic;
	Image image;
	int imgWidth, imgHeight;
	FindStarTask findStar;
	CorrelateTask correlateTask;
	
	final double [] expectedTop;
	final double [] expectedCenter;
	final double [] expectedRight;

	final double raShift, decShift;
	
	public StarCorrelationTest(TestRunner tr, File imageFile, double [] expectedTop, double [] expectedCenter, double [] expectedRight, double raShift, double decShift) {
		this.runner = tr;
		this.imageFile = imageFile;
		this.expectedTop = expectedTop;
		this.expectedCenter = expectedCenter;
		this.expectedRight = expectedRight;
		this.raShift = raShift;
		this.decShift = decShift;
	}
	
	@Override
	public void start()
	{
		mosaic = new Mosaic(runner.getApplication());
		
		image = runner.getApplication().getImage(imageFile);
		
		imgWidth = image.getCameraFrame().getWidth();
		imgHeight = image.getCameraFrame().getHeight();
		
		double raTarget;
		double decTarget;
		double radius;
		double maxMag;
		double pixelArcSec;
		
		raTarget = 210.92683333333332;
		decTarget = 54.28594444444444;
		radius = 5.0;
		maxMag = 11.0;
		pixelArcSec = 2 * 2.46;

		raTarget = expectedCenter[0] + raShift;
		decTarget = expectedCenter[1] + decShift;
		
		FocusUi.createStarProjection(mosaic, raTarget, decTarget, radius, maxMag, pixelArcSec);
		
		mosaic.addImage(image, ImageAddedCause.Loading);
		findStar = new FindStarTask(mosaic, image);
		runner.getApplication().getBackgroundTaskQueue().addTask(findStar);
		
		correlateTask = new CorrelateTask(mosaic, image);
		runner.getApplication().getBackgroundTaskQueue().addTask(correlateTask);
	}

	@Override
	public String toString() {
		return this.imageFile.toString() + (this.raShift != 0 || this.decShift != 0 ? " +[" + this.raShift+";" + this.decShift+"]" : "");
	}
	
	@Override
	public boolean isDone()
	{
		return correlateTask.getStatus().isFinal && findStar.getStatus().isFinal;
	}
	
	@Override
	public TestResult getResult() {
		List<String> messages = new ArrayList<String>();
		MosaicImageParameter mip = mosaic.getMosaicImageParameter(image);
		if (mip == null) {
			messages.add("Pas de MosaicImageParameter");
			return TestResult.Failed;
		}
		
		if (!mip.isCorrelated()) {
			messages.add("Pas de correlation");
			return TestResult.Failed;
		}
		
		double [] [] expected = {
				{ 0.5, 0.5},	expectedCenter,
				{ 0.5, 0},		expectedTop,
				{ 0, 0.5},		expectedRight
		};
		
		TestResult result = TestResult.OK;
		for(int i = 0; i < expected.length; i += 2)
		{
			double [] imgPos = expected[i];
			double [] raDecExpected = expected[i + 1];
		

			double [] mosaic3dPos = new double[3];
			double [] raDecPos = new double[2];
			
			
			mip.getProjection().image2dToSky3d(new double[]{0.5 * imgWidth * imgPos[0], 0.5 * imgHeight * imgPos[1]}, mosaic3dPos);
			mosaic.getMosaicToSky().convert(mosaic3dPos);
			SkyProjection.convert3DToRaDec(mosaic3dPos, raDecPos);
			
			double distance = SkyProjection.getDegreeDistance(raDecExpected, raDecPos);
			
			logger.info("RaDec : expected = [" + raDecExpected[0] + ";" + raDecExpected[1] + "]; found = [" + raDecPos[0] + ";" + raDecPos[1] + "] - distance is " + distance + "°");
			if (distance > 15 * 1.0 / 3600) {
				if (result != TestResult.Failed) {
					logger.error("Distance is > 15");
					result = TestResult.Failed;
				}
			} else if (distance > 60 * 1.0 / 3600) {
				if (result == TestResult.OK) {
					logger.warn("Distance is > 60");
					result = TestResult.Warning;
				}
			}
		}
		
		
		return result;
	}
	
	static double [] decodeRaDec(String value)
	{
		value = value.trim();
		Pattern re = Pattern.compile("\\[\\s*([0-9.-]+)\\s*;\\s*([0-9.-]+)\\s*\\]");
		Matcher matcher = re.matcher(value);
		if (!matcher.matches()) {
			throw new RuntimeException("invalid format :" + value);
		}
		return new double[]{Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2)) };
	}
	

	
	
	private static void doTest()
	{
		try {
			TestRunner tr = new TestRunner();
			logger.info("Running in " + new File(".").getAbsolutePath());
			File parent = new File("tests-data/images");
			Pattern re = Pattern.compile("(.*?)\\.(png|jpg|cr.)");
			String [] childs = parent.list();
			Arrays.sort(childs);
			for(String child : childs)
			{
				Matcher matcher = re.matcher(child);
				if (!matcher.matches()) {
					continue;
				}
				logger.info("found " + child);
				
				File image = new File(parent, child);
				File properties = new File(parent, matcher.group(1) + ".properties");
				
				try {
					Properties p = new Properties();
					p.load(new FileInputStream(properties));
					double[] top = decodeRaDec(p.getProperty("top"));
					double[] center = decodeRaDec(p.getProperty("center"));
					double[] right = decodeRaDec(p.getProperty("right"));
					

					tr.addTest(new StarCorrelationTest(tr, image, top, center, right, 0, 0));

					tr.addTest(new StarCorrelationTest(tr, image, top, center, right, 0.25, 0.45));

					tr.addTest(new StarCorrelationTest(tr, image, top, center, right, -1.2, 0.18));

					tr.addTest(new StarCorrelationTest(tr, image, top, center, right, -0.7, -2.2));
					
				} catch(Exception e) {
					throw new RuntimeException("failed to read " + properties, e);
				}
			}
			
			tr.start();
		} catch(Throwable t) {
			logger.error("fatal error", t);
		}
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				doTest();
			}
		});
	}
}
