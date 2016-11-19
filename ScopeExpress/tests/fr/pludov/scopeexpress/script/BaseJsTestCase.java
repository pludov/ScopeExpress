package fr.pludov.scopeexpress.script;

import static org.junit.Assert.assertEquals;

import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.script.Task.*;

/**
 * Base for test that start a script named by the class
 */
public class BaseJsTestCase {

	public BaseJsTestCase() {
	}
	
	public void test()
	{
		TaskGroup tg = new TaskGroup();
		String script = getClass().getSimpleName() + ".js";
		RootJsTask example = new RootJsTask(new Modules(tg, ContextFactory.getGlobal(), Utils.getClassFilePath(getClass())), script);

		while(example.getStatus() != Status.Done) {
			tg.advance();
		}
		assertEquals(example.getResult(), (double)1);
	}

}
