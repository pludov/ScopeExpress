package fr.pludov.scopeexpress.tests;

public interface TestInstance {
	public static enum TestResult { OK, Warning, Failed };
	public void start();
	public boolean isDone();
	public TestResult getResult();
	
}
