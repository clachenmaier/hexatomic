/*-
 * #%L
 * org.corpus_tools.hexatomic.formats
 * %%
 * Copyright (C) 2018 - 2020 Stephan Druskat, Thomas Krause
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package org.corpus_tools.hexatomic.formats.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Optional;
import org.corpus_tools.hexatomic.formats.Activator;
import org.corpus_tools.hexatomic.formats.CorpusFormatSelectionPage;
import org.corpus_tools.pepper.common.Pepper;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.wizard.IWizardPage;

public class ImporterSelectionPage extends CorpusFormatSelectionPage<ImportFormat>
    implements IWizardPage {


  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(ImporterSelectionPage.class);


  protected ImporterSelectionPage() {
    super("Select import format");
    setTitle("Select import format");
    setDescription(
        "Corpora are stored in specific formats and you need to select the correct one.");
  }

  protected void updateRecommendFormats(File corpusPath) {

    // Initialize selection to default
    btnExb.setSelection(false);
    btnPaulaXml.setSelection(false);

    Optional<Pepper> pepper = Activator.getPepper();
    File[] roots = File.listRoots();
    boolean pathIsRoot = false;
    for (File r : roots) {
      if (r.equals(corpusPath)) {
        pathIsRoot = true;
        break;
      }
    }
    if (!pathIsRoot && pepper.isPresent()) {
      try {
        for (String importerName : pepper.get()
            .findAppropriateImporters(URI.createFileURI(corpusPath.getAbsolutePath()))) {
          Optional<ImportFormat> format = ImportFormat.getFormatByName(importerName);
          if (format.isPresent()) {
            if (format.get() == ImportFormat.EXB) {
              btnExb.setSelection(true);
              setPageComplete(true);
            } else if (format.get() == ImportFormat.PAULA) {
              btnPaulaXml.setSelection(true);
              setPageComplete(true);
            }
          }
        }
      } catch (FileNotFoundException ex) {
        log.error("Corpus path not a valid URI, can't get recommened importers", ex);
      }
    }
  }

  @Override
  public Optional<ImportFormat> getSelectedFormat() {
    if (btnExb.getSelection()) {
      return Optional.of(ImportFormat.EXB);
    } else if (btnPaulaXml.getSelection()) {
      return Optional.of(ImportFormat.PAULA);
    }
    return Optional.empty();
  }
}
