package fr.pludov.scopeexpress.script;

import static org.junit.Assert.assertEquals;

import org.junit.*;

import fr.pludov.scopeexpress.script.Task.*;

public class Includes {

	@Test
	public void test() {
		TaskGroup tg = new TaskGroup();
		RootJsTask example = new RootJsTask(new Modules(tg, Utils.getClassFilePath(Includes.class)), "Includes.js");

		while(example.getStatus() != Status.Done) {
			tg.advance();
		}
		assertEquals(example.getResult(), (double)1);
	}

}
