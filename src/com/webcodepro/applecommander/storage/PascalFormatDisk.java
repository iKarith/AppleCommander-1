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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Manages a disk that is in the Pascal format.
 * <p>
 * Date created: Oct 4, 2002 11:56:50 PM
 * @author: Rob Greene
 */
public class PascalFormatDisk extends FormattedDisk {
	/**
	 * The size of the Pascal file entry.
	 */
	public static final int ENTRY_SIZE = 26;

	/**
	 * Use this inner interface for managing the disk usage data.
	 * This offloads format-specific implementation to the implementing class.
	 * A BitSet is used to track all blocks, as Pascal disks do not have a
	 * bitmap stored on the disk. This is safe since we know the number of blocks
	 * that exist. (BitSet length is of last set bit - unset bits at the end are
	 * "lost".)
	 */
	private class PascalDiskUsage implements DiskUsage {
		private int location = -1;
		private BitSet bitmap = null;
		public boolean hasNext() {
			return location == -1 || location < getBlocksOnDisk() - 1;
		}
		public void next() {
			if (bitmap == null) {
				bitmap = new BitSet(getBlocksOnDisk());
				// assume all blocks are unused
				for (int block=6; block<getBlocksOnDisk(); block++) {
					bitmap.set(block);
				}
				// process through all files and mark those blocks as used
				Iterator files = getFiles().iterator();
				while (files.hasNext()) {
					PascalFileEntry entry = (PascalFileEntry) files.next();
					for (int block=entry.getFirstBlock(); block<entry.getLastBlock(); block++) {
						bitmap.clear(block);
					}
				}
				location = 0;
			} else {
				location++;
			}
		}
		public boolean isFree() {
			return bitmap.get(location);	// true = free
		}
		public boolean isUsed() {
			return !bitmap.get(location);	// false = used
		}
	}

	/**
	 * Constructor for PascalFormatDisk.
	 * @param filename
	 * @param diskImage
	 * @param order
	 */
	public PascalFormatDisk(String filename, byte[] diskImage) {
		super(filename, diskImage);
	}

	/**
	 * Identify the operating system format of this disk.
	 * @see com.webcodepro.applecommander.storage.Disk#getFormat()
	 */
	public String getFormat() {
		return "Pascal";
	}

	/**
	 * Retrieve a list of files.
	 * @see com.webcodepro.applecommander.storage.Disk#getFiles()
	 */
	public List getFiles() {
		List list = new ArrayList();
		// read directory blocks (block 2-5):
		byte[] directory = new byte[4 * BLOCK_SIZE];
		for (int i=0; i<4; i++) {
			System.arraycopy(readBlock(2+i), 0, directory, i*BLOCK_SIZE, BLOCK_SIZE);
		}
		// process directory blocks:
		int entrySize = ENTRY_SIZE;
		int offset = entrySize;
		while (offset < directory.length) {
			byte[] entry = new byte[entrySize];
			System.arraycopy(directory, offset, entry, 0, entry.length);
			if (entry[6] == 0) break;	// at end
			list.add(new PascalFileEntry(entry, this));
			offset+= entrySize;
		}
		return list;
	}

	/**
	 * Identify if this disk format is capable of having directories.
	 * @see com.webcodepro.applecommander.storage.Disk#canHaveDirectories()
	 */
	public boolean canHaveDirectories() {
		return false;
	}

	/**
	 * Return the amount of free space in bytes.
	 * @see com.webcodepro.applecommander.storage.Disk#getFreeSpace()
	 */
	public int getFreeSpace() {
		return getFreeBlocks() * BLOCK_SIZE;
	}
	
	/**
	 * Return the number of free blocks.
	 */
	public int getFreeBlocks() {
		List files = getFiles();
		int blocksFree = getBlocksOnDisk() - 6;
		if (files != null) {
			for (int i=0; i<files.size(); i++) {
				PascalFileEntry entry = (PascalFileEntry) files.get(i);
				blocksFree-= entry.getBlocksUsed();
			}
		}
		return blocksFree;
	}
	
	/**
	 * Return the volume entry.
	 */
	protected byte[] getVolumeEntry() {
		byte[] block = readBlock(2);
		byte[] entry = new byte[ENTRY_SIZE];
		System.arraycopy(block, 0, entry, 0, entry.length);
		return entry;
	}
	
	/**
	 * Return the number of blocks on disk.
	 */
	public int getBlocksOnDisk() {
		return AppleUtil.getWordValue(getVolumeEntry(), 14);
	}

	/**
	 * Return the number of files on disk.
	 */
	public int getFilesOnDisk() {
		return AppleUtil.getWordValue(getVolumeEntry(), 16);
	}

	/**
	 * Return the last access date.
	 */
	public Date getLastAccessDate() {
		return AppleUtil.getPascalDate(getVolumeEntry(), 18);
	}

	/**
	 * Return the most recent date setting.  Huh?
	 */
	public Date getMostRecentDateSetting() {
		return AppleUtil.getPascalDate(getVolumeEntry(), 20);
	}

	/**
	 * Return the amount of used space in bytes.
	 * @see com.webcodepro.applecommander.storage.Disk#getUsedSpace()
	 */
	public int getUsedSpace() {
		return getUsedBlocks() * BLOCK_SIZE;
	}
	
	/**
	 * Return the number of used blocks.
	 */
	public int getUsedBlocks() {
		List files = getFiles();
		int blocksUsed = 6;
		if (files != null) {
			for (int i=0; i<files.size(); i++) {
				PascalFileEntry entry = (PascalFileEntry) files.get(i);
				blocksUsed+= entry.getBlocksUsed();
			}
		}
		return blocksUsed;
	}

	/**
	 * Return the name of the disk.  This is stored on block #2
	 * offset +6 (string[7]).
	 * @see com.webcodepro.applecommander.storage.Disk#getDiskName()
	 */
	public String getDiskName() {
		return AppleUtil.getPascalString(readBlock(2), 6);
	}

	/**
	 * Get suggested dimensions for display of bitmap.  Since Pascal disks are
	 * a block device, no suggestion is given.
	 */
	public int[] getBitmapDimensions() {
		return null;
	}

	/**
	 * Get the length of the bitmap.
	 */
	public int getBitmapLength() {
		return getBlocksOnDisk();
	}

	/**
	 * Get the disk usage iterator.
	 */
	public DiskUsage getDiskUsage() {
		return new PascalDiskUsage();
	}

	/**
	 * Get the labels to use in the bitmap.
	 */
	public String[] getBitmapLabels() {
		return new String[] { "Block" };
	}
	
	/**
	 * Get Pascal-specific disk information.
	 */
	public List getDiskInformation() {
		List list = super.getDiskInformation();
		list.add(new DiskInformation("Total Blocks", getBlocksOnDisk()));
		list.add(new DiskInformation("Free Blocks", getFreeBlocks()));
		list.add(new DiskInformation("Used Blocks", getUsedBlocks()));
		list.add(new DiskInformation("Files On Disk", getFilesOnDisk()));
		list.add(new DiskInformation("Last Access Date", getLastAccessDate()));
		list.add(new DiskInformation("Most Recent Date Setting", getMostRecentDateSetting()));
		return list;
	}

	/**
	 * Get the standard file column header information.
	 * This default implementation is intended only for standard mode.
	 */
	public List getFileColumnHeaders(int displayMode) {
		List list = new ArrayList();
		switch (displayMode) {
			case FILE_DISPLAY_NATIVE:
				list.add(new FileColumnHeader("Modified", 8, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Blocks", 3, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Filetype", 8, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Name", 15, FileColumnHeader.ALIGN_LEFT));
				break;
			case FILE_DISPLAY_DETAIL:
				list.add(new FileColumnHeader("Modified", 8, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Blocks", 3, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Bytes in last block", 3, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Size (bytes)", 6, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Filetype", 8, FileColumnHeader.ALIGN_CENTER));
				list.add(new FileColumnHeader("Name", 15, FileColumnHeader.ALIGN_LEFT));
				list.add(new FileColumnHeader("First Block", 3, FileColumnHeader.ALIGN_RIGHT));
				list.add(new FileColumnHeader("Last Block", 3, FileColumnHeader.ALIGN_RIGHT));
				break;
			default:	// FILE_DISPLAY_STANDARD
				list.addAll(super.getFileColumnHeaders(displayMode));
				break;
		}
		return list;
	}

	/**
	 * Indicates if this disk format supports "deleted" files.
	 */
	public boolean supportsDeletedFiles() {
		return false;
	}

	/**
	 * Indicates if this disk image can read data from a file.
	 */
	public boolean canReadFileData() {
		return true;
	}
	
	/**
	 * Indicates if this disk image can write data to a file.
	 */
	public boolean canWriteFileData() {
		return false;	// FIXME - not implemented
	}
	
	/**
	 * Indicates if this disk image can create a file.
	 */
	public boolean canCreateFile() {
		return false;	// FIXME - not implemented
	}
	
	/**
	 * Indicates if this disk image can delete a file.
	 */
	public boolean canDeleteFile() {
		return false;	// FIXME - not implemented
	}

	/**
	 * Get the data associated with the specified FileEntry.
	 */
	public byte[] getFileData(FileEntry fileEntry) {
		if ( !(fileEntry instanceof PascalFileEntry)) {
			throw new IllegalArgumentException("Most have a Pascal file entry!");
		}
		PascalFileEntry pascalEntry = (PascalFileEntry) fileEntry;
		int firstBlock = pascalEntry.getFirstBlock();
		int lastBlock = pascalEntry.getLastBlock();
		byte[] fileData = new byte[pascalEntry.getSize()];
		int offset = 0;
		for (int block = firstBlock; block < lastBlock; block++) {
			byte[] blockData = readBlock(block);
			if (block == lastBlock-1) {
				System.arraycopy(blockData, 0, fileData, offset, pascalEntry.getBytesUsedInLastBlock());
			} else {
				System.arraycopy(blockData, 0, fileData, offset, blockData.length);
			}
			offset+= blockData.length;
		}
		return fileData;
	}
}