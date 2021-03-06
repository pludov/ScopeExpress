//package net.ivoa.fits.test;
//
//import java.io.File;
//
//import junit.framework.TestCase;
//import net.ivoa.fits.*;
//import net.ivoa.fits.hdu.ImageHDU;
//import net.ivoa.util.*;
//
//public class TestHeader extends TestCase {
//
//	/**
//	 * Check out header manipulation.
//	 */
//	public void testHeader() throws Exception {
//		float[][] img = new float[300][300];
//
//		Fits f = new Fits();
//
//		ImageHDU hdu = (ImageHDU) Fits.makeHDU(img);
//		BufferedFile bf = new BufferedFile("ht1.fits", "rw");
//		f.addHDU(hdu);
//		f.write(bf);
//		bf.close();
//
//		f = new Fits("ht1.fits");
//		hdu = (ImageHDU) f.getHDU(0);
//		Header hdr = hdu.getHeader();
//
//		Cursor c = hdr.iterator();
//
//		c.setKey("XXX");
//		c.add("CTYPE1", new HeaderCard("CTYPE1", "GLON-CAR",
//				"Galactic Longitude"));
//		c.add("CTYPE2", new HeaderCard("CTYPE2", "GLAT-CAR",
//				"Galactic Latitude"));
//		c.setKey("CTYPE1"); // Move before CTYPE1
//		c.add("CRVAL1", new HeaderCard("CRVAL1", 0., "Longitude at reference"));
//		c.setKey("CTYPE2"); // Move before CTYPE2
//		c
//				.add("CRVAL2", new HeaderCard("CRVAL2", -90.,
//						"Latitude at reference"));
//		c.setKey("CTYPE1"); // Just practicing moving around!!
//		c.add("CRPIX1", new HeaderCard("CRPIX1", 150.0, "Reference Pixel X"));
//		c.setKey("CTYPE2");
//		c.add("CRPIX2", new HeaderCard("CRPIX2", 0., "Reference pixel Y"));
//		c.add("INV2", new HeaderCard("INV2", true, "Invertible axis"));
//		c.add("SYM2", new HeaderCard("SYM2", "YZ SYMMETRIC", "Symmetries..."));
//
//		c.setKey("CTYPE2");
//		c.add(new HeaderCard("COMMENT", null, "This should come after CTYPE2"));
//		c.add(new HeaderCard("COMMENT", null,
//				"This should come second after CTYPE2"));
//
//		hdr.findCard("CRPIX1");
//		hdr.addValue("INTVAL1", 1, "An integer value");
//		hdr.addValue("LOG1", true, "A true value");
//		hdr.addValue("LOGB1", false, "A false value");
//		hdr.addValue("FLT1", 1.34, "A float value");
//		hdr.addValue("FLT2", -1.234567890e-134, "A very long float");
//		hdr.insertComment("Comment after flt2");
//
//		hdr.rewrite();
//
//		c = hdr.iterator();
//		c.next();
//		c.next();
//		c.remove();
//		try {
//			hdr.rewrite();
//			fail("Rewrite without BITPIX succesed - that's bad");
//		} catch (Exception e) {
//			// as expected failed
//		}
//		c.add("BITPIX", new HeaderCard("BITPIX", 8, ""));
//
//		f = new Fits("ht1.fits");
//		hdr = f.getHDU(0).getHeader();
//
//		assertEquals("GLON-CAR", hdr.getStringValue("CTYPE1"));
//		assertEquals("GLAT-CAR", hdr.getStringValue("CTYPE2"));
//		assertEquals(150.0, hdr.getDoubleValue("CRPIX1"), 0);
//		assertEquals(0, hdr.getDoubleValue("CRPIX2"), 0);
//		assertEquals("YZ SYMMETRIC", hdr.getStringValue("SYM2"));
//		assertEquals(1, hdr.getIntValue("INTVAL1"));
//		assertEquals(true, hdr.getBooleanValue("LOG1"));
//		assertEquals(false, hdr.getBooleanValue("LOGB1"));
//
//		assertEquals(0, hdr.getFloatValue("CRVAL1"), 0);
//		assertEquals(-90, hdr.getFloatValue("CRVAL2"), 0);
//		assertEquals(true, hdr.getBooleanValue("INV2"));
//		assertEquals(2, hdr.getIntValue("NAXIS"));
//		assertEquals(1.34, hdr.getDoubleValue("FLT1"), 0);
//		assertEquals(-1.234567890e-134, hdr.getDoubleValue("FLT2"), 0);
//
//		c = hdr.iterator();
//		while (c.hasNext()) {
//			HeaderCard card = (HeaderCard) c.next();
//			String key = card.getKey();
//			String val = card.getValue();
//			String comment = card.getComment();
//			// default keys, generated by JavaFITS
//			if (key.equals("BITPIX")) {
//				assertEquals("-32", val);
//			} else if (key.equals("SIMPLE")) {
//				assertTrue(comment.startsWith("Java FITS"));
//				assertEquals("T", val);
//			} else if (key.equals("NAXIS")) {
//				assertEquals("2", val);
//			} else if (key.equals("NAXIS1")) {
//				assertEquals("300", val);
//			} else if (key.equals("NAXIS2")) {
//				assertEquals("300", val);
//			} else if (key.equals("EXTEND")) {
//				assertEquals("T", val);
//			}
//			// here comes "our" headers
//			else if (key.equals("XXX")) {
//				assertEquals(null, val);
//				assertEquals(null, comment);
//			} else if (key.equals("CTYPE1")) {
//				assertEquals("GLON-CAR", val);
//				assertEquals("Galactic Longitude", comment.trim());
//			} else if (key.equals("CTYPE2")) {
//				assertEquals("GLAT-CAR", val);
//				assertEquals("Galactic Latitude", comment.trim());
//			} else if (key.equals("CRVAL1")) {
//				assertEquals(0, Double.parseDouble(val), 0);
//				assertEquals("Longitude at reference", comment);
//			} else if (key.equals("CRVAL2")) {
//				assertEquals(-90, Double.parseDouble(val), 0);
//				assertEquals("Latitude at reference", comment);
//			} else if (key.equals("CRPIX1")) {
//				assertEquals(150.0, Double.parseDouble(val), 0);
//				assertEquals("Reference Pixel X", comment);
//			} else if (key.equals("CRPIX2")) {
//				assertEquals(0, Double.parseDouble(val), 0);
//				assertEquals("Reference pixel Y", comment);
//			} else if (key.equals("INV2")) {
//				assertEquals("T", val);
//				assertEquals("Invertible axis", comment);
//			} else if (key.equals("SYM2")) {
//				assertEquals("YZ SYMMETRIC", val);
//				assertEquals("Symmetries...", comment.trim());
//			} else if (key.equals("INTVAL1")) {
//				assertEquals("1", val);
//				assertEquals("An integer value", comment);
//			} else if (key.equals("LOG1")) {
//				assertEquals("T", val);
//				assertEquals("A true value", comment);
//			} else if (key.equals("LOGB1")) {
//				assertEquals("F", val);
//				assertEquals("A false value", comment);
//			} else if (key.equals("FLT1")) {
//				assertEquals(1.34, Double.parseDouble(val), 0);
//				assertEquals("A float value", comment);
//			} else if (key.equals("FLT2")) {
//				assertEquals(-1.234567890e-134, Double.parseDouble(val), 0);
//				assertEquals("A very long float", comment);
//			} else if (key.equals("COMMENT")) {
//				// ignore comment fields
//			} else {
//				fail("Unknow key:'" + key + "' value:'" + val + "' comment:'"
//						+ comment + "'");
//			}
//		}
//
//		while (hdr.rewriteable()) {
//			c.add(new HeaderCard("DUMMY", null, null));
//		}
//
//		File fi = new File("ht1.fits");
//		fi.delete();
//	}
//}
//
