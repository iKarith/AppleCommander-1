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

import java.util.List;

/**
 * Represents a file entry on disk - not the data.
 * <p>
 * Date created: Oct 4, 2002 4:46:42 PM
 * @author: Rob Greene
 */
public interface FileEntry {
	/**
	 * Return the name of this file.
	 */
	public String getFilename();
	
	/**
	 * Return the filetype of this file.
	 * This will be OS specific.
	 */
	public String getFiletype();
	
	/**
	 * Identify if this file is locked.
	 */
	public boolean isLocked();
	
	/**
	 * Compute the size of this file (in bytes).
	 */
	public int getSize();
	
	/**
	 * Identify if this is a directory file.
	 */
	public boolean isDirectory();
	
	/**
	 * Retrieve the list of files in this directory.
	 * Note that if this is not a directory, the return
	 * value should be null.  If this a directory, the
	 * return value should always be a list - a directory
	 * with 0 entries returns an empty list.
	 */
	public List getFiles();
	
	/**
	 * Identify if this file has been deleted.
	 */
	public boolean isDeleted();

	/**
	 * Get the standard file column header information.
	 * This default implementation is intended only for standard mode.
	 * displayMode is specified in FormattedDisk.
	 */
	public List getFileColumnData(int displayMode);
	
	/**
	 * Get file data.  This handles any operating-system specific issues.
	 * Specifically, DOS 3.3 places address and length into binary files
	 * and length into Applesoft files.
	 */
	public byte[] getFileData();
	
	/**
	 * Get the suggested FileFilter.  This appears to be operating system
	 * specific, so each operating system needs to implement some manner
	 * of guessing the appropriate filter.
	 */
	public FileFilter getSuggestedFilter();
}