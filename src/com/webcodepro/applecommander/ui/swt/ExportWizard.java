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
package com.webcodepro.applecommander.ui.swt;

import java.util.Stack;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;

/**
 * File export wizard.
 * <p>
 * Date created: Nov 7, 2002 9:22:35 PM
 * @author: Rob Greene
 */
public class ExportWizard {
	private FormattedDisk disk;
	private Shell parent;
	private Shell dialog;
	private Image logo;	// managed by SwtAppleCommander
	private Stack wizardPanes = new Stack();
	private FileFilter fileFilter;
	private String directory;
	private boolean wizardCompleted;
	private Button backButton;
	private Button nextButton;
	private Button finishButton;
	private Composite contentPane;
	/**
	 * Constructor for ExportWizard.
	 */
	public ExportWizard(Shell parent, Image logo, FormattedDisk disk) {
		super();
		this.parent = parent;
		this.logo = logo;
		this.disk = disk;
	}
	/**
	 * Create the dialog.
	 */
	private void createDialog() {
		dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText("Export Wizard");
		RowLayout layout = new RowLayout(SWT.VERTICAL);
		layout.justify = true;
		layout.marginBottom = 5;
		layout.marginLeft = 5;
		layout.marginRight = 5;
		layout.marginTop = 5;
		layout.spacing = 3;
		dialog.setLayout(layout);

		// Wizard logo		
		RowData rowData = new RowData();
		rowData.width = logo.getImageData().width;
		rowData.height = logo.getImageData().height;
		ImageCanvas imageCanvas = new ImageCanvas(dialog, SWT.BORDER, logo, rowData);

		// Starting pane
		rowData = new RowData();
		rowData.width = logo.getImageData().width;
		contentPane = new Composite(dialog, SWT.BORDER);
		contentPane.setLayoutData(rowData);
		contentPane.setLayout(new FillLayout());

		// Bottom row of buttons
		Composite composite = new Composite(dialog, SWT.NONE);
		composite.setLayoutData(rowData);
		composite.setLayout(new FillLayout(SWT.HORIZONTAL));
		Button button = new Button(composite, SWT.PUSH);
		button.setText("Cancel");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				wizardCompleted = false;
				dialog.close();
			}
		});
		backButton = new Button(composite, SWT.PUSH);
		backButton.setEnabled(false);
		backButton.setText("< Back");
		backButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				WizardPane current = (WizardPane) wizardPanes.pop();
				WizardPane previous = (WizardPane) wizardPanes.peek();
				backButton.setEnabled(wizardPanes.size() > 1);
				current.dispose();
				previous.open();
				dialog.pack();
			}
		});
		nextButton = new Button(composite, SWT.PUSH);
		nextButton.setText("Next >");
		nextButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				WizardPane current = (WizardPane) wizardPanes.peek();
				WizardPane next = current.getNextPane();
				wizardPanes.add(next);
				backButton.setEnabled(wizardPanes.size() > 1);
				current.dispose();
				next.open();
				dialog.pack();
			}
		});
		finishButton = new Button(composite, SWT.PUSH);
		finishButton.setEnabled(false);
		finishButton.setText("Finish");
		finishButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				wizardCompleted = true;
				dialog.close();
			}
		});
		
		WizardPane wizardPane = new ExportFileStartPane(contentPane, this, null);
		wizardPanes.add(wizardPane);
		wizardPane.open();

		dialog.pack();
	}
	/**
	 * Open and display the dialog.
	 */
	public void open() {
		createDialog();
		dialog.open();
		Display display = dialog.getDisplay();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep ();
		}
	}
	/**
	 * Dispose of all panels and resources.
	 */
	public void dispose() {
		while (!wizardPanes.empty()) {
			WizardPane pane = (WizardPane) wizardPanes.pop();
			pane.dispose();
			pane = null;
		}
		dialog.dispose();
		backButton.dispose();
		nextButton.dispose();
		finishButton.dispose();
		contentPane.dispose();
	}
	/**
	 * Get the FileFilter.
	 */
	public FileFilter getFileFilter() {
		return fileFilter;
	}
	/**
	 * Set the FileFilter.
	 */
	public void setFileFilter(FileFilter fileFilter) {
		this.fileFilter = fileFilter;
	}
	/**
	 * Indicates if the wizard was completed.
	 */
	public boolean isWizardCompleted() {
		return wizardCompleted;
	}
	/**
	 * Enable/disable the next button.
	 */
	public void enableNextButton(boolean state) {
		nextButton.setEnabled(state);
	}
	/**
	 * Enable/disable the finish button.
	 */
	public void enableFinishButton(boolean state) {
		finishButton.setEnabled(state);
	}
	/**
	 * Get the disk that is being worked on.
	 */
	public FormattedDisk getDisk() {
		return disk;
	}
	/**
	 * Returns the directory.
	 * @return String
	 */
	public String getDirectory() {
		return directory;
	}
	/**
	 * Sets the directory.
	 * @param directory The directory to set
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}
}