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

package org.corpus_tools.hexatomic.formats.exporter;

import org.corpus_tools.hexatomic.formats.CorpusFormatSelectionPage;
import org.corpus_tools.hexatomic.formats.CorpusPathSelectionPage;
import org.corpus_tools.hexatomic.formats.CorpusPathSelectionPage.Type;
import org.eclipse.jface.wizard.Wizard;

public class ExportWizard extends Wizard {

  private final CorpusPathSelectionPage corpusPathPage = new CorpusPathSelectionPage(Type.EXPORT);
  private final CorpusFormatSelectionPage exporterPage = new ExporterSelectionPage();

  @Override
  public String getWindowTitle() {
    return "EXPORT a corpus project to a different file format";
  }

  @Override
  public void addPages() {
    addPage(corpusPathPage);
    addPage(exporterPage);
  }

  @Override
  public boolean performFinish() {
    // TODO Auto-generated method stub
    return false;
  }

}
