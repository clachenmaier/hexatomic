
package org.corpus_tools.hexatomic.core.handlers;

import javax.inject.Named;

import org.corpus_tools.hexatomic.core.ProjectManager;
import org.corpus_tools.salt.common.SDocument;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;

public class OpenSaltDocumentHandler {

	public static final String DOCUMENT_ID = "org.corpus_tools.hexatomic.document-id";

	@Execute
	public static void execute(ProjectManager projectManager, EModelService modelService,
			EPartService partService, ESelectionService selectionService,
			@Named("org.corpus_tools.hexatomic.core.commandparameter.editor-id") String editorID) {
		
		// get currently selected document
		Object selection = selectionService.getSelection();
		if(selection instanceof SDocument) {
			
			SDocument document = (SDocument) selection;
	
			if(document.getDocumentGraph() == null) {
				if(document.getDocumentGraphLocation() == null) {
					// create a new document graph, because no one exists yet
					document.createDocumentGraph();
				} else {
					// TODO: show progress indicator
					document.loadDocumentGraph();
				}
			}

			// Create a new part from an editor part descriptor
			MPart editorPart = partService.createPart(editorID);
			editorPart.setLabel(document.getName());
			editorPart.getPersistedState().put(OpenSaltDocumentHandler.DOCUMENT_ID, document.getId());

			partService.showPart(editorPart, PartState.ACTIVATE);

		}
	}
	
	@CanExecute
	public static boolean canExecute(ESelectionService selectionService) {
		return selectionService.getSelection() instanceof SDocument;
	}

}