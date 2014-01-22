package net.ivoa.fits.test;

import net.ivoa.fits.*;
import net.ivoa.fits.hdu.BasicHDU;

public class FitsReader {

	public static void main(String[] args) throws Exception {

		String file = args[0];

		Fits f = new Fits(file);
		int i = 0;
		BasicHDU h;

		f.skipHDU(12);
		i = 12;

		do {
			h = f.readHDU();
			if (h != null) {
				if (i == 0) {
					System.out.println("\n\nPrimary header:\n");
				} else {
					System.out.println("\n\nExtension " + i + ":\n");
				}
				i += 1;
				System.out.println(h.toString());
			}
		} while (h != null);

	}
}