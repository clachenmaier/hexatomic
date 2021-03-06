/*-
 * #%L
 * org.corpus_tools.hexatomic.grid
 * %%
 * Copyright (C) 2018 - 2020 Stephan Druskat,
 *                                     Thomas Krause
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

package org.corpus_tools.hexatomic.grid.internal.data;

import org.corpus_tools.hexatomic.core.errors.HexatomicRuntimeException;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;

/**
 * Provides the data to be displayed in the row header, i.e., row count starting from 1.
 * 
 * @author Stephan Druskat (mail@sdruskat.net)
 *
 */
public class RowHeaderDataProvider implements IDataProvider {

  private final GraphDataProvider provider;

  /**
   * Constructor setting the body data provider. Throws a {@link HexatomicRuntimeException} if the
   * passed argument is <code>null</code>.
   * 
   * @param bodyDataProvider The body data provider
   */
  public RowHeaderDataProvider(GraphDataProvider bodyDataProvider) {
    if (bodyDataProvider == null) {
      throw new HexatomicRuntimeException(
          "Body data provider in " + this.getClass().getSimpleName() + " must not be null.");
    }
    this.provider = bodyDataProvider;
  }

  @Override
  public Object getDataValue(int columnIndex, int rowIndex) {
    return rowIndex + 1;
  }

  @Override
  public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
    // Left unimplemented, as header data shouldn't be settable
  }

  @Override
  public int getColumnCount() {
    if (provider.getColumns().isEmpty()) {
      return 0;
    }
    return 1;
  }

  @Override
  public int getRowCount() {
    return provider.getRowCount();
  }

}
