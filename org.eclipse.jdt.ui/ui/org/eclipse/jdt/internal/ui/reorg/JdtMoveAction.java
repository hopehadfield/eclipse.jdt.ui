/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.actions.MoveProjectAction;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementContentProvider;

import org.eclipse.jdt.internal.corext.refactoring.reorg.MoveRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class JdtMoveAction extends ReorgDestinationAction {

	private boolean fShowPreview= false;

	public JdtMoveAction(String name, ISelectionProvider provider) {
		super(name, provider);
		setDescription(ReorgMessages.getString("moveAction.description")); //$NON-NLS-1$
	}
	
	public JdtMoveAction(StructuredSelectionProvider provider) {
		super(ReorgMessages.getString("moveAction.label"), provider); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * see @ReorgDestinationAction#isOkToProceed
	 */
	String getActionName() {
		return ReorgMessages.getString("moveAction.name"); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * see @ReorgDestinationAction#getDestinationDialogMessage
	 */
	String getDestinationDialogMessage() {
		return ReorgMessages.getString("moveAction.destination.label"); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * see @ReorgDestinationAction#createRefactoring
	 */
	ReorgRefactoring createRefactoring(List elements){
		return new MoveRefactoring(elements, JavaPreferencesSettings.getCodeGenerationSettings());
	}
	
	ElementTreeSelectionDialog createDestinationSelectionDialog(Shell parent, ILabelProvider labelProvider, JavaElementContentProvider cp, ReorgRefactoring refactoring){
		return new MoveDestinationDialog(parent, labelProvider, cp, (MoveRefactoring)refactoring);
	}	
	
	/* non java-doc
	 * see @ReorgDestinationAction#isOkToProceed
	 */
	protected boolean isOkToProceed(ReorgRefactoring refactoring) throws JavaModelException{
		return (isOkToMoveReadOnly(refactoring));
	}
	
	/*
	 * @see ReorgDestinationAction#getExcluded(ReorgRefactoring)
	 */ 
	Set getExcluded(ReorgRefactoring refactoring) throws JavaModelException{
		Set elements= refactoring.getElementsThatExistInTarget();
		Set result= new HashSet();
		for (Iterator iter= elements.iterator(); iter.hasNext(); ){
			Object o= iter.next();
			int action= askIfOverwrite(ReorgUtils.getName(o));
			if (action == IDialogConstants.CANCEL_ID)
				return null;
			if (action == IDialogConstants.YES_TO_ALL_ID)	
				return new HashSet(0); //nothing excluded
			if (action == IDialogConstants.NO_ID)		
				result.add(o);	
		}
		return result;
	}
	
	private static int askIfOverwrite(String elementName){
		Shell shell= JavaPlugin.getActiveWorkbenchShell().getShell();
		String title= ReorgMessages.getString("JdtMoveAction.move"); //$NON-NLS-1$
		String question= ReorgMessages.getFormattedString("JdtMoveAction.overwrite", elementName);//$NON-NLS-1$
		
		String[] labels= new String[] {IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL,
															 IDialogConstants.NO_LABEL,  IDialogConstants.CANCEL_LABEL };
		final MessageDialog dialog = new MessageDialog(shell,	title, null, question, MessageDialog.QUESTION,	labels,  0);
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				dialog.open();
			}
		});
		int result = dialog.getReturnCode();
		if (result == 0)
			return IDialogConstants.YES_ID;
		if (result == 1)
			return IDialogConstants.YES_TO_ALL_ID;
		if (result == 2)
			return IDialogConstants.NO_ID;
		return IDialogConstants.CANCEL_ID;
	}
	
	
	protected void setShowPreview(boolean showPreview) {
		fShowPreview = showPreview;
	}
	
	private static boolean isOkToMoveReadOnly(ReorgRefactoring refactoring){
		if (! hasReadOnlyElements(refactoring))
			return true;
		
		return  MessageDialog.openQuestion(
				JavaPlugin.getActiveWorkbenchShell(),
				ReorgMessages.getString("moveAction.checkMove"),  //$NON-NLS-1$
				ReorgMessages.getString("moveAction.error.readOnly")); //$NON-NLS-1$
	}
	
	private static boolean hasReadOnlyElements(ReorgRefactoring refactoring){
		for (Iterator iter= refactoring.getElementsToReorg().iterator(); iter.hasNext(); ){
			if (ReorgUtils.shouldConfirmReadOnly(iter.next())) 
				return true;
		}
		return false;
	}
	
	/* non java-doc
	 * @see IRefactoringAction#canOperateOn(IStructuredSelection)
	 */
	public boolean canOperateOn(IStructuredSelection selection) {
		if (hasOnlyProjects())
			return selection.size() == 1;
		else
			return super.canOperateOn(selection);
	}
	
	/*
	 * @see Action#run()
	 */
	public void run() {
		if (hasOnlyProjects()){
			moveProject();
		}	else {
			super.run();
		}
	}
	
	/* non java-doc
	 * @see ReorgDestinationAction#doReorg(ReorgRefactoring) 
	 */
	void doReorg(ReorgRefactoring refactoring) throws JavaModelException{
		if (!fShowPreview){
			super.doReorg(refactoring);
			return;
		}	
		//XX incorrect help
		RefactoringWizard wizard= new RefactoringWizard(refactoring, ReorgMessages.getString("JdtMoveAction.move"), IJavaHelpContextIds.MOVE_CU_ERROR_WIZARD_PAGE); //$NON-NLS-1$
		new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), wizard).open();	
	}
	
	private void moveProject(){
		MoveProjectAction action= new MoveProjectAction(JavaPlugin.getActiveWorkbenchShell());
		action.selectionChanged(getStructuredSelection());
		action.run();
	}
	
	//--- static inner classes
		
	private class MoveDestinationDialog extends ElementTreeSelectionDialog {
		private static final int PREVIEW_ID= IDialogConstants.CLIENT_ID + 1;
		private MoveRefactoring fRefactoring;
		private Button fCheckbox;
		private Button fPreview;
		
		MoveDestinationDialog(Shell parent, ILabelProvider labelProvider, 	ITreeContentProvider contentProvider, MoveRefactoring refactoring){
			super(parent, labelProvider, contentProvider);
			fRefactoring= refactoring;
			fShowPreview= false;//from outter instance
			setDoubleClickSelects(false);
		}
		
		protected Control createDialogArea(Composite parent) {
			Composite result= (Composite)super.createDialogArea(parent);
			fCheckbox= new Button(result, SWT.CHECK);
			fCheckbox.setText(ReorgMessages.getString("JdtMoveAction.update_references")); //$NON-NLS-1$
			fCheckbox.setEnabled(canUpdateReferences());
			fCheckbox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					updatePreviewButton();
					fRefactoring.setUpdateReferences(fCheckbox.getEnabled() && fCheckbox.getSelection());
				}
			});
			fCheckbox.setSelection(canUpdateReferences());
			return result;
		}
		
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			fPreview= createButton(parent, PREVIEW_ID, ReorgMessages.getString("JdtMoveAction.preview"), false); //$NON-NLS-1$
		}
		
		protected void updateOKStatus() {
			super.updateOKStatus();
			try{
				fRefactoring.setDestination(getFirstResult());
				fCheckbox.setEnabled(getOkButton().getEnabled() &&  canUpdateReferences());
				updatePreviewButton();
			} catch (JavaModelException e){
				ExceptionHandler.handle(e, ReorgMessages.getString("JdtMoveAction.move"), ReorgMessages.getString("JdtMoveAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			}		
		}
		
		protected void buttonPressed(int buttonId) {
			fShowPreview= (buttonId == PREVIEW_ID);
			super.buttonPressed(buttonId);
			if (buttonId == PREVIEW_ID)
				close();
		}
		
		private void updatePreviewButton(){
			fPreview.setEnabled(fCheckbox.getEnabled() && fCheckbox.getSelection());
		}
		
		private boolean canUpdateReferences(){
			try{
				return fRefactoring.canUpdateReferences();
			} catch (JavaModelException e){
				return false;
			}
		}
	}
}
