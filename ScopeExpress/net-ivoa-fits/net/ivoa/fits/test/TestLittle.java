package net.ivoa.fits.test;

import junit.framework.TestCase;
import net.ivoa.fits.*;

public class TestLittle extends TestCase {

	public void test1() throws Exception {

		Header h = new Header();
		h.addValue("XXX", 1, "Comment1");
		h.addValue("XXX", 2, "Comment2");
		h.addValue("YYY", 1, "Comment1");
		h.addValue("YYY", 2, "Comment2");
		h.addValue("ZZZ", 1, "Comment1");
		h.addValue("ZZZ", 2, "Comment2");
		h.findCard("XXX");
		h.addValue("YYY", 3, "Comment3");

		java.util.Iterator it = h.iterator();
		int i = 0;
		while (it.hasNext()) {
			HeaderCard hc = (HeaderCard) it.next();
			switch (i) {
				case 0 :
					assertEquals("XXX", hc.getKey());
					assertEquals("2", hc.getValue());
					assertEquals("Comment2", hc.getComment());
					break;
				case 1 :
					assertEquals("YYY", hc.getKey());
					assertEquals("3", hc.getValue());
					assertEquals("Comment3", hc.getComment());
					break;
				case 2 :
					assertEquals("ZZZ", hc.getKey());
					assertEquals("2", hc.getValue());
					assertEquals("Comment2", hc.getComment());
					break;
				default :
					fail("Too much keys in header");
			}
			i++;
		}
	}
}

