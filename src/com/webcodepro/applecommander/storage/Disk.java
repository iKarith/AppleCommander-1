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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Abstract representation of an Apple2 disk (floppy, 800k, hard disk).
 * <p>
 * Date created: Oct 3, 2002 10:59:47 PM
 * @author: Rob Greene
 */
public class Disk {
	/**
	 * Specifies a filter to be used in determining filetypes which are supported.
	 * This works from a file extension, so it may or may not apply to the Macintosh.
	 */
	public class FilenameFilter {
		private String names;
		private String extensions;
		public FilenameFilter(String names, String extensions) {
			this.names = names;
			this.extensions = extensions;
		}
		public String getExtensions() {
			return extensions;
		}
		public String getNames() {
			return names;
		}
	}
	
	public static final int BLOCK_SIZE = 512;
	public static final int SECTOR_SIZE = 256;
	public static final int APPLE_140KB_DISK = 143360;
	public static final int APPLE_800KB_DISK = 819200;
	public static final int APPLE_800KB_2IMG_DISK = APPLE_800KB_DISK + 0x40;

	private static FilenameFilter[] filenameFilters;
	private byte[] diskImage;
	private String filename;
	
	/**
	 * Get the supported file filters supported by the Disk interface.
	 * This is due to the fact that FilenameFilter is an innerclass of Disk -
	 * without an instance of the class, the filters cannot be created.
	 */
	public static FilenameFilter[] getFilenameFilters() {
		if (filenameFilters == null) {
			new Disk();
		}
		return filenameFilters;
	}
	
	/**
	 * Constructor for a Disk - used only to generate FilenameFilter objects.
	 */
	private Disk() {
		filenameFilters = new FilenameFilter[] {
			new FilenameFilter("All Emulator Images", 
				"*.do; *.dsk; *.po; *.2mg; *.2img; *.hdv; *.do.gz; *.dsk.gz; *.po.gz; *.2mg.gz; *.2img.gz"),
			new FilenameFilter("140K DOS 3.3 Ordered Images (*.do, *.dsk)", 
				"*.do; *.dsk; *.do.gz; *.dsk.gz"),
			new FilenameFilter("140K ProDOS Ordered Images (*.po)", 
				"*.po; *.po.gz"),
			new FilenameFilter("800K ProDOS Ordered Images (*.2mg, *.2img)", 
				"*.2mg; *.2img; *.2mg.gz, *.2img.gz"),
			new FilenameFilter("ApplePC Hard Disk Images (*.hdv)", 
				"*.hdv"),
			new FilenameFilter("All Compressed Images", 
				"*.do.gz; *.dsk.gz; *.po.gz; *.2mg.gz; *.2img.gz"),
			new FilenameFilter("All Files", 
				"*.*")
		};
	}
	
	/**
	 * Construct a Disk with the given byte array.
	 */
	protected Disk(String filename, byte[] diskImage) {
		this.diskImage = diskImage;
		this.filename = filename;
	}
	
	/**
	 * Construct a Disk and load the specified file.
	 * Read in the entire contents of the file.
	 */
	public Disk(String filename) throws IOException {
		this.filename = filename;
		InputStream input = new FileInputStream(filename);
		if (isCompressed()) {
			input = new GZIPInputStream(input);
		}
		int diskSize = APPLE_140KB_DISK;
		if (is2ImgOrder()) {
			diskSize = APPLE_800KB_2IMG_DISK;
		}
		ByteArrayOutputStream diskImageByteArray = 
			new ByteArrayOutputStream(diskSize);
		byte[] data = new byte[1024];
		int bytes;
		while ((bytes = input.read(data)) > 0) {
			diskImageByteArray.write(data, 0, bytes);
		}
		input.close();
		this.diskImage = diskImageByteArray.toByteArray();
	}

	/**
	 * Determine type of disk, and return the appropriate
	 * FormattedDisk object.  Returns null if none are
	 * recognized.
	 */
	public FormattedDisk getFormattedDisk() {
		if (isProdosFormat()) {
			return new ProdosFormatDisk(filename, diskImage);
		} else if (isDosFormat()) {
			return new DosFormatDisk(filename, diskImage);
		} else if (isPascalFormat()) {
			return new PascalFormatDisk(filename, diskImage);
		} else if (isRdosFormat()) {
			return new RdosFormatDisk(filename, diskImage);
		}
		return null;
	}
	
	/**
	 * Returns the diskImage.
	 * @return byte[]
	 */
	public byte[] getDiskImage() {
		return diskImage;
	}
	
	/**
	 * Extract a portion of the disk image.
	 */
	public byte[] readBytes(int start, int length) {
		byte[] buffer = new byte[length];
		System.arraycopy(diskImage, start + (is2ImgOrder() ? 0x40 : 0), buffer, 0, length);
		return buffer;
	}

	/**
	 * Returns the filename.
	 * @return String
	 */
	public String getFilename() {
		return filename;
	}
	
	/**
	 * Indicate if this disk is GZIP compressed.
	 */
	public boolean isCompressed() {
		return filename.toLowerCase().endsWith(".gz");
	}
	
	/**
	 * Indicate if this disk is ProDOS ordered (beginning with block 0).
	 */
	public boolean isProdosOrder() {
		return filename.toLowerCase().endsWith(".po")
			|| filename.toLowerCase().endsWith(".po.gz")
			|| is2ImgOrder()
			|| filename.toLowerCase().endsWith(".hdv");
	}
	
	/**
	 * Indicate if this disk is DOS ordered (T0,S0 - T35,S15).
	 */
	public boolean isDosOrder() {
		return filename.toLowerCase().endsWith(".do")
			|| filename.toLowerCase().endsWith(".do.gz")
			|| filename.toLowerCase().endsWith(".dsk")
			|| filename.toLowerCase().endsWith(".dsk.gz");
	}
	
	/**
	 * Indicate if this disk is a 2IMG disk.
	 * This is ProDOS ordered, but with a header on the disk.
	 */
	public boolean is2ImgOrder() {
		return filename.toLowerCase().endsWith(".2img")
			|| filename.toLowerCase().endsWith(".2img.gz")
			|| filename.toLowerCase().endsWith(".2mg")
			|| filename.toLowerCase().endsWith(".2mg.gz");
	}
	
	/**
	 * Identify the size of this disk.
	 */
	public int getPhysicalSize() {
		return diskImage.length;
	}
	
	/**
	 * Get the block from the disk image.
	 */
	public byte[] readBlock(int block) {
		if (block * BLOCK_SIZE > getPhysicalSize()) {
			return null;
		} else {
			if (isProdosOrder()) {
				return readBytes(block*BLOCK_SIZE, BLOCK_SIZE);
			} else if (isDosOrder()) {
				int[] sectorMapping1 = { 0, 13, 11, 9, 7, 5, 3, 1 };
				int[] sectorMapping2 = { 14, 12, 10, 8, 6, 4, 2, 15 };
				int track = block / 8;
				int sectorOffset = block % 8;
				int sector1 = sectorMapping1[sectorOffset];
				int sector2 = sectorMapping2[sectorOffset];
				int physicalLocation1 = (track * 16 + sector1) * SECTOR_SIZE;
				int physicalLocation2 = (track * 16 + sector2) * SECTOR_SIZE;
				byte[] data = new byte[BLOCK_SIZE];
				System.arraycopy(readBytes(physicalLocation1, SECTOR_SIZE),
					0, data, 0, SECTOR_SIZE);
				System.arraycopy(readBytes(physicalLocation2, SECTOR_SIZE),
					0, data, SECTOR_SIZE, SECTOR_SIZE);
				return data;
			} else {
				return null;
			}
		}
	}

	/**
	 * Retrieve the specified sector.
	 */
	public byte[] readSector(int track, int sector) {
		if ((track * 16 + sector) * SECTOR_SIZE > getPhysicalSize()) {
			return null;
		} else if (isProdosOrder()) {
			// what block a sector belongs to:
			int[] blockInterleave = { 0, 7, 6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 0, 7 };
			// where in that block a sector resides:
			int[] blockOffsets = { 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1 };
			return readBytes(
				((track * 8) + blockInterleave[sector]) * BLOCK_SIZE 
				+ blockOffsets[sector] * SECTOR_SIZE, SECTOR_SIZE);
		} else if (isDosOrder()) {
			return readBytes((track * 16 + sector) * SECTOR_SIZE, SECTOR_SIZE);
		}
		return null;
	}
	
	/**
	 * Test the disk format to see if this is a ProDOS formatted
	 * disk.
	 */
	public boolean isProdosFormat() {
		byte[] prodosVolumeDirectory = readBlock(2);
		return prodosVolumeDirectory[0] == 0 &&
			prodosVolumeDirectory[1] == 0 &&
			(prodosVolumeDirectory[4]&0xf0) == 0xf0;
	}
	
	/**
	 * Test the disk format to see if this is a DOS 3.3 formatted
	 * disk.
	 */
	public boolean isDosFormat() {
		byte[] vtoc = readSector(17, 0);
		return vtoc[0x01] == 17	// expect catalog to start on track 17
			&& vtoc[0x02] == 15		// expect catalog to start on sector 15
			&& vtoc[0x03] == 3		// expect DOS release number of 3
			&& vtoc[0x27] == 122	// expect 122 tract/sector pairs per sector
			&& vtoc[0x34] == 35		// expect 35 tracks per disk (140KB disk only!)
			&& vtoc[0x35] == 16		// expect 16 sectors per disk (140KB disk only!)
			&& vtoc[0x36] == 0		// bytes per sector (low byte)
			&& vtoc[0x37] == 1;		// bytes per sector (high byte)
	}
	
	/**
	 * Test the disk format to see if this is a Pascal formatted
	 * disk.
	 */
	public boolean isPascalFormat() {
		byte[] directory = readBlock(2);
		return directory[0] == 0 && directory[1] == 0
			&& directory[2] == 6 && directory[3] == 0
			&& directory[4] == 0 && directory[5] == 0;
	}
	
	/**
	 * Test the disk format to see if this is a RDOS formatted
	 * disk.
	 */
	public boolean isRdosFormat() {
		byte[] block = readSector(0, 0x0d);
		String id = AppleUtil.getString(block, 0xe0, 4);
		return "RDOS".equals(id);
	}
}