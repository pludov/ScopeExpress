package fr.pludov.cadrage.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.focus.Application;
import fr.pludov.cadrage.ui.focus.StarCorrelationTest;
import fr.pludov.cadrage.ui.utils.BackgroundTaskQueueListener;
import fr.pludov.cadrage.utils.WeakListenerOwner;

public class TestRunner {
	private static final Logger logger = Logger.getLogger(TestRunner.class);

	final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	final Application application;
	int runningId;
	final List<TestInstance> testList;
	Thread waitForTests;
	boolean done;

	public TestRunner() {
		application = new Application();
		testList = new ArrayList<TestInstance>();
		runningId = -1;
		
		application.getBackgroundTaskQueue().listeners.addListener(listenerOwner, new BackgroundTaskQueueListener() {
			@Override
			public void stateChanged() {
				if (runningId >= 0 && runningId < testList.size()) {
					TestInstance runningInstance = testList.get(runningId);
					if (runningInstance.isDone()) {
						logger.info("Test " + runningInstance + " done => " + runningInstance.getResult());
						advance();
					}
				}
			}
		});

	}

	private TestInstance getRunningInstance()
	{
		if (runningId >= 0 && runningId < testList.size()) {
			TestInstance runningInstance = testList.get(runningId);
			return runningInstance;
		}
		return null;
	}
	
	
	public Application getApplication() {
		return application;
	}

	public void addTest(TestInstance ti)
	{
		this.testList.add(ti);
	}
	
	private void advance()
	{
		this.runningId++;
		TestInstance ti = getRunningInstance();
		if (ti != null) {
			ti.start();
		} else {
			synchronized(this) {
				done = true;
				notifyAll();
			}
		}
	}
	
	public void start()
	{
		done = false;
		advance();
		waitForTests = new Thread(new Runnable(){
			@Override
			public void run() {
				synchronized(TestRunner.this)
				{
					while(!done) {
						try {
							TestRunner.this.wait();
						} catch(InterruptedException e) {
							logger.warn("Exception", e);
						}
					}
				}
				
				for(TestInstance ti : testList)
				{
					logger.info("Test " + ti + " => " + ti.getResult());
				}
			}
		});
		
		waitForTests.setDaemon(false);
		waitForTests.start();
		
//		try {
//			waitForTests.join();
//		} catch(InterruptedException e) {
//			logger.warn("Exception", e);
//		}
	}
}
