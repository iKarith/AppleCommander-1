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

import com.webcodepro.applecommander.util.AppleUtil;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a Pascal file entry on disk.
 * <p>
 * Date created: Oct 5, 2002 12:22:34 AM
 * @author Rob Greene
 */
public class PascalFileEntry implements FileEntry {
	private byte[] fileEntry;
	private PascalFormatDisk disk;

	/**
	 * Constructor for PascalFileEntry.
	 */
	public PascalFileEntry(byte[] fileEntry, PascalFormatDisk disk) {
		super();
		this.fileEntry = fileEntry;
		this.disk = disk;
	}
	
	/**
	 * Get the block number of the files 1st block.
	 */
	public int getFirstBlock() {
		return AppleUtil.getWordValue(fileEntry, 0);
	}
	
	/**
	 * Get the block number of the files last block +1.
	 */
	public int getLastBlock() {
		return AppleUtil.getWordValue(fileEntry, 2);
	}

	/**
	 * Return the name of this file.
	 */
	public String getFilename() {
		return AppleUtil.getPascalString(fileEntry, 6);
	}

	/**
	 * Set the name of this file.
	 */
	public void setFilename(String filename) {
		// FIXME: Need to implement!
	}
	
	/**
	 * Return the maximum filename length.
	 */
	public int getMaximumFilenameLength() {
		return 15;
	}

	/**
	 * Return the filetype of this file.
	 */
	public String getFiletype() {
		String[] filetypes = disk.getFiletypes();
		int filetype = fileEntry[4] & 0x0f;
		if (filetype == 0 || filetype > filetypes.length) {
			return "unknown (" + filetype + ")";
		} else {
			return filetypes[filetype-1];
		}
	}

	/**
	 * Set the filetype.
	 */
	public void setFiletype(String filetype) {
		// FIXME: Implement!
	}
	
	/**
	 * Identify if this file is locked - not applicable in Pascal?
	 */
	public boolean isLocked() {
		return false;
	}

	/**
	 * Set the lock indicator.
	 */
	public void setLocked(boolean lock) {
		// FIXME: Implement!
	}
	
	/**
	 * Get the number of bytes used in files last block.
	 */
	public int getBytesUsedInLastBlock() {
		return AppleUtil.getWordValue(fileEntry, 22);
	}
	
	/**
	 * Compute the size of this file (in bytes).
	 */
	public int getSize() {
		int blocks = getBlocksUsed() - 1;
		return blocks*Disk.BLOCK_SIZE + getBytesUsedInLastBlock();
	}
	
	/**
	 * Compute the blocks used.
	 */
	public int getBlocksUsed() {
		return AppleUtil.getWordValue(fileEntry, 2) - AppleUtil.getWordValue(fileEntry, 0);
	}
	
	/**
	 * Pascal does not support directories.
	 */
	public boolean isDirectory() {
		return false;
	}
	
	/**
	 * Retrieve the list of files in this directory.
	 * Always returns null, as Pascal does not support directories.
	 */
	public List getFiles() {
		return null;
	}
	
	/**
	 * Pascal file entries are removed upon deletion.
	 * Thus, a file entry cannot be marked as deleted.
	 */
	public boolean isDeleted() {
		return false;
	}

	/**
	 * Delete the file.
	 */
	public void delete() {
		// FIXME: Need to implement!
	}
	
	/**
	 * Get the file modification date.
	 */
	public Date getModificationDate() {
		return AppleUtil.getPascalDate(fileEntry, 24);
	}

	/**
	 * Get the standard file column header information.
	 * This default implementation is intended only for standard mode.
	 * displayMode is specified in FormattedDisk.
	 */
	public List getFileColumnData(int displayMode) {
		NumberFormat numberFormat = NumberFormat.getNumberInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy");

		List list = new ArrayList();
		switch (displayMode) {
			case FormattedDisk.FILE_DISPLAY_NATIVE:
				list.add(dateFormat.format(getModificationDate()));
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getBlocksUsed()));
				list.add(getFiletype());
				list.add(getFilename());
				break;
			case FormattedDisk.FILE_DISPLAY_DETAIL:
				list.add(dateFormat.format(getModificationDate()));
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getBlocksUsed()));
				numberFormat.setMinimumIntegerDigits(1);
				list.add(numberFormat.format(getBytesUsedInLastBlock()));
				list.add(numberFormat.format(getSize()));
				list.add(getFiletype());
				list.add(getFilename());
				numberFormat.setMinimumIntegerDigits(3);
				list.add(numberFormat.format(getFirstBlock()));
				list.add(numberFormat.format(getLastBlock()-1));
				break;
			default:	// FILE_DISPLAY_STANDARD
				list.add(getFilename());
				list.add(getFiletype());
				list.add(numberFormat.format(getSize()));
				list.add(isLocked() ? "Locked" : "");
				break;
		}
		return list;
	}

	/**
	 * Get file data.  This handles any operating-system specific issues.
	 * Currently, the disk itself handles this.
	 */
	public byte[] getFileData() {
		return disk.getFileData(this);
	}

	/**
	 * Set file data.  This, essentially, is saving data to disk using this
	 * file entry.
	 */
	public void setFileData(byte[] data) throws DiskFullException {
		// FIXME: Implement!
	}
	
	/**
	 * Get the suggested FileFilter.  This appears to be operating system
	 * specific, so each operating system needs to implement some manner
	 * of guessing the appropriate filter.
	 */
	public FileFilter getSuggestedFilter() {
		if ("textfile".equals(getFiletype())) {
			return new TextFileFilter();
		}
		return new BinaryFileFilter();
	}

	/**
	 * Get the FormattedDisk associated with this FileEntry.
	 * This is useful to interfaces that need to retrieve the associated
	 * disk.
	 */
	public FormattedDisk getFormattedDisk() {
		return disk;
	}

	/**
	 * Indicates if this filetype requires an address component.
	 * Note that the FormattedDisk also has this method - normally,
	 * this will defer to the method on FormattedDisk, as it will be
	 * more generic.
	 */
	public boolean needsAddress() {
		return false;
	}
	
	/**
	 * Set the address that this file loads at.
	 */
	public void setAddress(int address) {
		// Does not apply.
	}

	/**
	 * Indicates that this filetype can be compiled.
	 */
	public boolean canCompile() {
		return false;
	}
}
