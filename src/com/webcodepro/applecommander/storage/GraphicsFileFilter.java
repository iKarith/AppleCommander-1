/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002 by Robert Greene
 * robgreene at users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the 
 * Free Software Foundation; either version 2 of the License, or (at your 
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.webcodepro.applecommander.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.sun.image.codec.jpeg.JPEGCodec;

/**
 * Filter the given file as if it were a graphics image.
 * <p>
 * Address for Apple2 HGR/DHR address is calculated from an observation of a pattern:<br>
 * line number bits:  87654321<br>
 * 87 are multipled by 0x0028<br>
 * 65 are multipled by 0x0100<br>
 * 4 is multiplied by 0x0080<br>
 * 321 are multipled by 0x0400
 * <p>
 * HGR bit values ignore the high bit, as that switches the "palette", and for B&W mode,
 * the bit does nothing.  The other 7 bits simply toggle the pixel on or off.  Double hires
 * does not follow this - it uses a real 4 bit value, but the high bit is still ignored for
 * graphics (hence, the 560 instead of 640 resolution).
 * <p>
 * Date created: Nov 3, 2002 12:06:36 PM
 * @author: Rob Greene
 */
public class GraphicsFileFilter implements FileFilter {
	public static final int MODE_HGR_BLACK_AND_WHITE = 1;
	public static final int MODE_HGR_COLOR = 2;
	public static final int MODE_DHR_BLACK_AND_WHITE = 3;
	public static final int MODE_DHR_COLOR = 4;
	
	private String extension;
	private int mode = MODE_HGR_COLOR;
	
	private static final int CODEC_NONE = 0;			// disabled!
	private static final int CODEC_IMAGEIO = 1;		// JDK 1.4
	private static final int CODEC_JPEGCODEC = 2;	// SUN JDK's only
	private int imageCodec;
	
	/**
	 * Constructor for GraphicsFileFilter.
	 */
	public GraphicsFileFilter() {
		super();
		determineImageCodec();
	}
	
	/**
	 * Start guessing which codec is avilable for images.
	 */
	protected void determineImageCodec() {
		try {
			Class.forName("javax.imageio.ImageIO");
			imageCodec = CODEC_IMAGEIO;
			extension = "PNG";
			return;
		} catch (ClassNotFoundException ignored) {
			try {
				Class.forName("com.sun.image.codec.jpeg.JPEGCodec");
				imageCodec = CODEC_JPEGCODEC;
				extension = "JPEG";
			} catch (ClassNotFoundException ignored2) {
				imageCodec = CODEC_NONE;
			}
		}
	}
	
	/**
	 * Indicate if a codec is available (assist with interface requirements).
	 */
	public boolean isCodecAvailable() {
		return imageCodec != CODEC_NONE;
	}
	
	/**
	 * Indicate if the ImageIO codec is avilable.
	 */
	protected boolean isCodecImageIo() {
		return imageCodec == CODEC_IMAGEIO;
	}
	
	/**
	 * Indicate if the SUN JPEG Codec is avilable.
	 */
	protected boolean isCodecJpegCodec() {
		return imageCodec == CODEC_JPEGCODEC;
	}

	/**
	 * Filter the file data and produce an image.
	 * @see com.webcodepro.applecommander.storage.FileFilter#filter(FileEntry)
	 */
	public byte[] filter(FileEntry fileEntry) {
		byte[] fileData = fileEntry.getFileData();
		BufferedImage image = null;
		if (isHiresColorMode()) {
			image = new BufferedImage(280, 192, BufferedImage.TYPE_INT_RGB);
		} else if (isDoubleHiresMode()) {
			image = new BufferedImage(560, 192*2, BufferedImage.TYPE_INT_RGB);
		} else {
			return new byte[0];
		}
		for (int y=0; y<192; y++) {
			int base = (			// odd notation - bit value shifted right * hex value
				((y & 0x7) << 10)			// 00000111 * 0x0400
				| (y & 0x8) << 4			// 00001000 * 0x0080
				| (y & 0x30) << 4			// 00110000 * 0x0100
				| ((y & 0xc0) >> 6) * 0x028	// 11000000 * 0x0028
				) & 0x1fff;
			byte[] lineData = new byte[40];
			System.arraycopy(fileData, base, lineData, 0, 40);
			if (isHiresBlackAndWhiteMode()) {
				processHiresBlackAndWhiteLine(lineData, image, y);
			} else if (isHiresColorMode()) {
				processHiresColorLine(lineData, image, y);
			} else if (isDoubleHiresMode()) {
				byte[] lineData2 = new byte[40];
				System.arraycopy(fileData, base + 0x2000, lineData2, 0, 40);
				if (isDoubleHiresBlackAndWhiteMode()) {
					processDoubleHiresBlackAndWhiteLine(lineData, lineData2, image, y);
				} else if (isDoubleHiresColorMode()) {
					processDoubleHiresColorLine(lineData, lineData2, image, y);
				}
			} else {
				// oops...
			}
		}
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			if (isCodecImageIo()) {
				ImageIO.write(image, getExtension(), outputStream);
			} else if (isCodecJpegCodec()) {
				JPEGCodec.createJPEGEncoder(outputStream).encode(image);
			}
			return outputStream.toByteArray();
		} catch (IOException ex) {
			return null;
		}
	}

	/**
	 * Given a specific line in the image, process it in hires black and white
	 * mode.
	 */
	protected void processHiresBlackAndWhiteLine(byte[] lineData, BufferedImage image, int y) {
		for (int x=0; x<280; x++) {
			int offset = x / 7;	// byte across row
			int bit = x % 7;		// bit to test
			byte byt = lineData[offset];
			if (AppleUtil.isBitSet(byt, bit)) {
				image.setRGB(x, y, 0xffffff);
			} else {
				image.setRGB(x, y, 0x0);
			}
		}
	}

	/**
	 * Given a specific line in the image, process it in hires color mode.
	 * HGR color is two bits to determine color - essentially resolution is
	 * 140 horizontally, but it indicates the color for two pixels.
	 * <p>
	 * The names of pixles is a bit confusion - pixel0 is really the left-most
	 * pixel (not the low-value bit).
	 * To alleviate my bad naming, here is a color table to assist:<br>
	 * <pre>
	 * Color   Bits      RGB
	 * ======= ==== ========
	 * Black1   000 0x000000
	 * Green    001 0x00ff00
	 * Violet   010 0xff00ff
	 * White1   011 0xffffff
	 * Black2   100 0x000000
	 * Orange   101 0xff8000
	 * Blue     110 0x0000ff
	 * White2   111 0xffffff
	 * </pre>
	 * Remember: bits are listed as "highbit", "pixel0", "pixel1"!
	 */
	protected void processHiresColorLine(byte[] lineData, BufferedImage image, int y) {
		for (int x=0; x<140; x++) {
			int x0 = x*2;
			int x1 = x0+1;
			int offset0 = x0 / 7;	// byte across row
			int bit0 = x0 % 7;		// bit to test
			boolean pixel0 = AppleUtil.isBitSet(lineData[offset0], bit0);
			int offset1 = x1 / 7;	// byte across row
			int bit1 = x1 % 7;		// bit to test
			boolean pixel1 = AppleUtil.isBitSet(lineData[offset1], bit1);
			int color;
			if (pixel0 && pixel1) {
				color = 0xffffff;	// white
			} else if (!pixel0 && !pixel1) {
				color = 0;			// black
			} else {
				boolean highbit = pixel0 ? AppleUtil.isBitSet(lineData[offset0], 7) :
					AppleUtil.isBitSet(lineData[offset1], 7);
				if (pixel0 && highbit) {
					color = 0x0000ff;	// blue
				} else if (pixel0 && !highbit) {
					color = 0xff00ff;	// voilet
				} else if (pixel1 && !highbit) {
					color = 0x00ff00;	// green
				} else {	// pixel1 && highbit
					color = 0xff8000;	// orange
				}
			}
			if (pixel0) image.setRGB(x0, y, color);
			if (pixel1) image.setRGB(x1, y, color);
		}
	}

	/**
	 * Given a specific line in the image, process it in double hires black and white
	 * mode.
	 */
	protected void processDoubleHiresBlackAndWhiteLine(byte[] lineData1, byte[] lineData2, 
		BufferedImage image, int y) {
			
		for (int x=0; x<560; x++) {
				// alternate bytes - switching memory banks
			byte[] lineData = (x % 14 < 7) ? lineData1 : lineData2;
			int rowOffset = x / 14;	// byte across row
			int bit = x % 7;			// bit to test
			byte byt = lineData[rowOffset];
			if (AppleUtil.isBitSet(byt, bit)) {
				image.setRGB(x, y*2, 0xffffff);
				image.setRGB(x, y*2+1, 0xffffff);
			} else {
				image.setRGB(x, y*2, 0x0);
				image.setRGB(x, y*2+1, 0x0);
			}
		}
	}

	/**
	 * Given a specific line in the image, process it in double hires color
	 * mode.  Treat image as 140x192 mode.
	 * <p>
	 * From the <a href='http://web.pdx.edu/~heiss/technotes/aiie/tn.aiie.03.html'>Apple2 
	 * technical note:
	 * <pre>
	 *                                          Repeated<br>
     *                                          Binary<br>
     *    Color         aux1  main1 aux2  main2 Pattern<br>
     *    Black          00    00    00    00    0000<br>
     *    Magenta        08    11    22    44    0001<br>
     *    Brown          44    08    11    22    0010<br>
     *    Orange         4C    19    33    66    0011<br>
     *    Dark Green     22    44    08    11    0100<br>
     *    Grey1          2A    55    2A    55    0101<br>
     *    Green          66    4C    19    33    0110<br>
     *    Yellow         6E    5D    3B    77    0111<br>
     *    Dark Blue      11    22    44    08    1000<br>
     *    Violet         19    33    66    4C    1001<br>
     *    Grey2          55    2A    55    2A    1010<br>
     *    Pink           5D    3B    77    6E    1011<br>
     *    Medium Blue    33    66    4C    19    1100<br>
     *    Light Blue     3B    77    6E    5D    1101<br>
     *    Aqua           77    6E    5D    3B    1110<br>
     *    White          7F    7F    7F    7F    1111
     * </pre>
	 */
	protected void processDoubleHiresColorLine(byte[] lineData1, byte[] lineData2, 
		BufferedImage image, int y) {
		
		int[] bitValues = { 8,4,2,1 };
		int[] colorValues = {
				0x000000, 0xff0000, 0x800000, 0xff8000,	// black, magenta, brown, orange
				0x008000, 0x808080, 0x00ff00, 0xffff00,	// dark green, grey1, green, yellow
				0x000080, 0xff00ff, 0x808080, 0xff80c0,	// dark blue, voilet, grey2, pink
				0x0000a0, 0x0000ff, 0x00c080, 0xffffff	// medium blue, light blue, aqua, white
		};
		for (int x=0; x<560; x+=4) {
			int colorValue = 0;
			for (int b = 0; b < 4; b++) {
				int xb = x+b;
				// alternate bytes - switching memory banks
				byte[] lineData = (xb % 14 < 7) ? lineData1 : lineData2;
				int rowOffset = xb / 14;	// byte across row
				int bit = xb % 7;			// bit to test
				byte byt = lineData[rowOffset];
				if (AppleUtil.isBitSet(byt, bit)) {
					colorValue+= bitValues[b];
				}
			}
			for (int b = 0; b < 4; b++) {
				image.setRGB(x+b, y*2, colorValues[colorValue]);
				image.setRGB(x+b, y*2+1, colorValues[colorValue]);
			}
		}
	}

	/**
	 * Give file extensions.
	 */
	public String[] getFileExtensions() {
		if (isCodecImageIo()) {
			return new String[] { "PNG", "JPEG"  };
		} else if (isCodecJpegCodec()) {
			return new String[] { "JPEG" };
		} else {
			return new String[0];
		}
	}

	/**
	 * Give suggested file name.
	 */
	public String getSuggestedFileName(FileEntry fileEntry) {
		String fileName = fileEntry.getFilename().trim();
		if (!fileName.toLowerCase().endsWith("." + getExtension())) {
			fileName = fileName + "." + getExtension();
		}
		return fileName;
	}
	
	/**
	 * Set the format name.
	 */
	public void setExtension(String extension) {
		this.extension = extension;
	}
	
	/**
	 * Get the format name.
	 */
	public String getExtension() {
		return extension;
	}
	
	/**
	 * Set the color mode.
	 */
	public void setMode(int mode) {
		this.mode = mode;
	}

	/**
	 * Indicates if this is configured for hires black & white mode.
	 */
	public boolean isHiresBlackAndWhiteMode() {
		return mode == MODE_HGR_BLACK_AND_WHITE;
	}
	
	/**
	 * Indicates if this is configured for hires color mode.
	 */
	public boolean isHiresColorMode() {
		return mode == MODE_HGR_COLOR;
	}
	
	/**
	 * Indicates if this is configured for double hires black & white mode.
	 */
	public boolean isDoubleHiresBlackAndWhiteMode() {
		return mode == MODE_DHR_BLACK_AND_WHITE;
	}
	
	/**
	 * Indicates if this is configured for double hires color mode.
	 */
	public boolean isDoubleHiresColorMode() {
		return mode == MODE_DHR_COLOR;
	}
	
	/**
	 * Indicates if this is a hires mode.
	 */
	protected boolean isHiresMode() {
		return isHiresBlackAndWhiteMode() || isHiresColorMode();
	}
	
	/**
	 * Indicates if this is a double hires mode.
	 */
	protected boolean isDoubleHiresMode() {
		return isDoubleHiresBlackAndWhiteMode() || isDoubleHiresColorMode();
	}
}