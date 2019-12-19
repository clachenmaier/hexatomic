/*-
 * #%L
 * org.corpus_tools.hexatomic.graph
 * %%
 * Copyright (C) 2018 - 2019 Stephan Druskat, Thomas Krause
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

package org.corpus_tools.hexatomic.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.corpus_tools.hexatomic.console.AtomicalConsole;
import org.corpus_tools.hexatomic.core.ProjectManager;
import org.corpus_tools.hexatomic.core.errors.ErrorService;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SPointingRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SSpanningRelation;
import org.corpus_tools.salt.common.SStructuredNode;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.extensions.notification.Listener;
import org.corpus_tools.salt.graph.GRAPH_ATTRIBUTES;
import org.corpus_tools.salt.graph.IdentifiableElement;
import org.corpus_tools.salt.util.DataSourceSequence;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.internal.ZoomManager;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.LayoutItem;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.CompositeLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;

@SuppressWarnings("restriction")
public class GraphEditor {

  @Inject
  private ProjectManager projectManager;

  @Inject
  private MPart thisPart;

  @Inject
  public GraphEditor() {

  }

  private Button btnIncludeSpans;
  private Table textRangeTable;
  private Text txtSegmentFilter;
  private Button btnIncludePointingRelations;

  private GraphViewer viewer;

  private ZoomManager zoomManager;

  @Inject
  UISynchronize sync;

  @Inject
  ErrorService errors;

  private ListenerImplementation projectChangeListener;

  private SDocumentGraph getGraph() {
    String documentID = thisPart.getPersistedState().get("org.corpus_tools.hexatomic.document-id");
    Optional<SDocument> doc = projectManager.getDocument(documentID);
    if (doc.isPresent()) {
      return doc.get().getDocumentGraph();
    }
    return null;
  }

  /**
   * Create a new graph viewer.
   * 
   * @param parent The parent SWT composite.
   * @param part The part this viewer belongs to.
   */
  @PostConstruct
  public void postConstruct(Composite parent, MPart part) {

    projectChangeListener = new ListenerImplementation();
    projectManager.addListener(projectChangeListener);

    parent.setLayout(new FillLayout(SWT.VERTICAL));

    SashForm mainSash = new SashForm(parent, SWT.VERTICAL);

    SashForm graphSash = new SashForm(mainSash, SWT.HORIZONTAL);

    viewer = new GraphViewer(graphSash, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
    viewer.getGraphControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    viewer.setContentProvider(new SaltGraphContentProvider());
    viewer.setLabelProvider(new SaltLabelProvider());
    viewer.setLayoutAlgorithm(createLayout());
    viewer.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED);

    Composite filterComposite = new Composite(graphSash, SWT.NONE);
    GridLayout gridLayoutFilterComposite = new GridLayout(1, false);
    gridLayoutFilterComposite.marginWidth = 0;
    filterComposite.setLayout(gridLayoutFilterComposite);

    Label lblFilterByAnnotation = new Label(filterComposite, SWT.NONE);
    lblFilterByAnnotation.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
    lblFilterByAnnotation.setText("Filter by annotation type");

    btnIncludeSpans = new Button(filterComposite, SWT.CHECK);
    btnIncludeSpans.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
    btnIncludeSpans.setSelection(false);
    btnIncludeSpans.setText("Include Spans");

    btnIncludePointingRelations = new Button(filterComposite, SWT.CHECK);
    btnIncludePointingRelations.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1));
    btnIncludePointingRelations.setSelection(true);
    btnIncludePointingRelations.setText("Include pointing relations");

    txtSegmentFilter = new Text(filterComposite, SWT.BORDER);
    txtSegmentFilter.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1));

    textRangeTable =
        new Table(filterComposite, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION | SWT.MULTI);
    textRangeTable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
    textRangeTable.setHeaderVisible(true);
    textRangeTable.setLinesVisible(true);
    textRangeTable.getHorizontalBar().setEnabled(true);
    textRangeTable.getVerticalBar().setEnabled(true);

    TableColumn tblclmnFilterBySegment = new TableColumn(textRangeTable, SWT.NONE);
    tblclmnFilterBySegment.setWidth(100);
    tblclmnFilterBySegment.setText("Filter by segment");
    graphSash.setWeights(new int[] {300, 100});

    textRangeTable.addSelectionListener(new SelectionListener() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        updateView(false);

      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        updateView(false);

      }
    });
    txtSegmentFilter.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        updateView(true);
      }
    });
    btnIncludePointingRelations.addSelectionListener(new SelectionListener() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        updateView(true);

      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {

      }
    });
    btnIncludeSpans.addSelectionListener(new SelectionListener() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        updateView(true);
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {

      }
    });

    zoomManager = new ZoomManager(viewer.getGraphControl().getRootLayer(),
        viewer.getGraphControl().getViewport());

    viewer.getGraphControl().addMouseWheelListener(new MouseWheelListener() {

      @Override
      public void mouseScrolled(MouseEvent e) {
        if (e.count < 0) {
          zoomManager.zoomOut();
        } else {
          zoomManager.zoomIn();
        }

      }
    });
    viewer.getControl().forceFocus();

    Document consoleDocument = new Document();
    SourceViewer consoleViewer = new SourceViewer(mainSash, null, SWT.V_SCROLL | SWT.H_SCROLL);
    consoleViewer.setDocument(consoleDocument);
    new AtomicalConsole(consoleViewer, sync, getGraph());
    mainSash.setWeights(new int[] {200, 100});

    updateView(true);

  }

  @PreDestroy
  void preDestroy() {
    projectManager.removeListener(projectChangeListener);
  }

  private List<Integer> getSegmentIdxSortedByLength() {
    List<Integer> result =
        IntStream.range(0, textRangeTable.getItemCount()).boxed().collect(Collectors.toList());

    result.sort(new Comparator<Integer>() {
      @SuppressWarnings("unchecked")
      @Override
      public int compare(Integer o1, Integer o2) {

        // get both ranges from the container
        Object r1Raw = textRangeTable.getItem(o1).getData("range");
        Object r2Raw = textRangeTable.getItem(o2).getData("range");

        Range<Long> r1 = (Range<Long>) r1Raw;
        Range<Long> r2 = (Range<Long>) r2Raw;
        return ComparisonChain.start().compare(r1.upperEndpoint() - r1.lowerEndpoint(),
            r2.upperEndpoint() - r2.lowerEndpoint()).result();

      }
    });

    return result;
  }

  @SuppressWarnings("unchecked")
  private void updateView(boolean recalculateSegments) {

    SDocumentGraph graph = getGraph();

    if (recalculateSegments) {
      // store the old segment selection
      List<Range<Long>> oldSelectedRanges = new LinkedList<>();
      for (TableItem item : textRangeTable.getSelection()) {
        oldSelectedRanges.add((Range<Long>) item.getData("range"));
      }

      updateSegments(graph);

      textRangeTable.deselectAll();

      // sort the segments by their length
      List<Integer> sortedIdx = getSegmentIdxSortedByLength();

      // for each old segment select the first (and thus smallest) segment in the new
      // list
      boolean selectedSomeOld = false;
      for (Range<Long> oldRange : oldSelectedRanges) {
        for (int idx : sortedIdx) {
          Range<Long> itemRange = (Range<Long>) textRangeTable.getItem(idx).getData("range");
          if (itemRange.encloses(oldRange)) {
            textRangeTable.select(idx);
            selectedSomeOld = true;
            // only select the first one
            break;
          }
        }
      }
      if (!selectedSomeOld && textRangeTable.getItemCount() > 0) {
        textRangeTable.setSelection(0);
      }
    }

    // update the status check for each item
    for (int idx = 0; idx < textRangeTable.getItemCount(); idx++) {
      textRangeTable.getItem(idx).setChecked(textRangeTable.isSelected(idx));
    }

    viewer.setFilters(new Filter());

    if (viewer.getInput() != graph) {
      viewer.setInput(graph);
    }

    viewer.applyLayout();
  }

  private static Multimap<STextualDS, Range<Long>> calculateSegments(SDocumentGraph graph,
      ViewerFilter filter) {
    List<SNode> roots = graph.getRoots();
    List<STextualDS> allTexts = new ArrayList<>(graph.getTextualDSs());
    allTexts.sort(new STextualDataSourceComparator());

    LinkedHashMultimap<STextualDS, Range<Long>> result = LinkedHashMultimap.create();

    for (STextualDS ds : graph.getTextualDSs()) {
      boolean hasNonTokenRoot = false;
      TreeSet<Range<Long>> sortedRangesForDS = new TreeSet<>(new RangeStartComparator<>());

      for (SNode r : roots) {

        if (filter.select(null, null, r)) {

          if (!(r instanceof SToken)) {
            hasNonTokenRoot = true;
          }

          @SuppressWarnings("rawtypes")
          List<DataSourceSequence> overlappedDS =
              graph.getOverlappedDataSourceSequence(r, SALT_TYPE.STEXT_OVERLAPPING_RELATION);
          if (overlappedDS != null) {

            for (DataSourceSequence<?> seq : overlappedDS) {
              if (seq.getDataSource() instanceof STextualDS && seq.getDataSource() == ds) {

                long start = 0;
                long end = 0;
                if (seq.getStart() != null) {
                  start = seq.getStart().longValue();
                }
                if (seq.getEnd() != null) {
                  end = seq.getEnd().longValue();
                }
                sortedRangesForDS.add(Range.closedOpen(start, end));
              }
            }
          }
        }
      }

      if (hasNonTokenRoot) {

        result.putAll(ds, sortedRangesForDS);
      } else {
        // add the whole text as a sequence
        long textLength = 0L;
        if (ds.getText() != null) {
          textLength = ds.getText().length();
        }
        result.put(ds, Range.closed(0L, textLength));
      }

    }

    return result;
  }

  private void updateSegments(SDocumentGraph graph) {
    textRangeTable.removeAll();

    ViewerFilter currentFilter = new RootFilter();

    Multimap<STextualDS, Range<Long>> segments = calculateSegments(graph, currentFilter);

    for (Map.Entry<STextualDS, Range<Long>> e : segments.entries()) {
      TableItem item = new TableItem(textRangeTable, SWT.NONE);

      item.setText(e.getValue().lowerEndpoint() + ".." + e.getValue().upperEndpoint() + " ("
          + (e.getKey().getName() + ")"));
      item.setData("range", e.getValue());
      item.setData("text", e.getKey());
    }

  }

  private LayoutAlgorithm createLayout() {

    TokenLayoutAlgorithm tokenLayout = new TokenLayoutAlgorithm(LayoutStyles.NONE);
    TreeLayoutAlgorithm otherLayout = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);

    org.eclipse.zest.layouts.Filter hierarchyFilter = new org.eclipse.zest.layouts.Filter() {

      @Override
      public boolean isObjectFiltered(LayoutItem object) {
        IdentifiableElement data = SaltGraphContentProvider.getData(object);
        if (data instanceof SStructuredNode || data instanceof SToken) {
          return false;
        } else if (data instanceof SDominanceRelation || data instanceof SSpanningRelation) {
          SRelation<?, ?> rel = (SRelation<?, ?>) data;
          if (rel.getTarget() instanceof SStructuredNode) {
            return false;
          }
        }
        return true;
      }
    };

    tokenLayout.setFilter(hierarchyFilter);
    otherLayout.setFilter(hierarchyFilter);

    LayoutAlgorithm[] layouts = new LayoutAlgorithm[] {otherLayout, tokenLayout};
    return new CompositeLayoutAlgorithm(layouts);
  }

  private final class ListenerImplementation implements Listener {
    @Override
    public void notify(NOTIFICATION_TYPE type, GRAPH_ATTRIBUTES attribute, Object oldValue,
        Object newValue, Object container) {
      sync.syncExec(() -> updateView(true));
    }
  }

  private class RootFilter extends ViewerFilter {

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {

      if (element instanceof SNode) {

        boolean include = false;

        SNode node = (SNode) element;

        if (txtSegmentFilter.getText().isEmpty() || (node instanceof SToken)) {
          include = true;
        } else {
          if (node.getAnnotations() != null) {
            for (SAnnotation anno : node.getAnnotations()) {
              if (anno.getName().contains(txtSegmentFilter.getText())) {
                include = true;
                break;
              }
            }
          }
        }

        if (node instanceof SSpan) {
          include = include && btnIncludeSpans.getSelection();
        }
        return include;

      } else {
        return true;
      }
    }

  }

  private class Filter extends ViewerFilter {

    private final Set<String> coveredTokenIDs;

    public Filter() {

      // Collect all tokens which are selected by the current ranges
      Set<SToken> coveredTokens = new HashSet<>();
      for (TableItem item : textRangeTable.getSelection()) {

        @SuppressWarnings("unchecked")
        Range<Long> itemRange = (Range<Long>) item.getData("range");
        STextualDS itemText = (STextualDS) item.getData("text");

        DataSourceSequence<Number> seq = new DataSourceSequence<>();
        seq.setStart(itemRange.lowerEndpoint());
        seq.setEnd(itemRange.upperEndpoint());
        seq.setDataSource(itemText);
        List<SToken> t = itemText.getGraph().getTokensBySequence(seq);
        if (t != null) {
          coveredTokens.addAll(t);
        }
      }
      coveredTokenIDs = new HashSet<>();
      for (SToken t : coveredTokens) {
        coveredTokenIDs.add(t.getId());
      }
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {

      if (element instanceof SNode) {
        SNode node = (SNode) element;
        boolean include = false;

        // check if the node covers a currently selected range
        List<SToken> overlappedTokens = getGraph().getOverlappedTokens(node);
        for (SToken t : overlappedTokens) {
          if (coveredTokenIDs.contains(t.getId())) {
            include = true;
            break;
          }
        }

        if (node instanceof SSpan) {
          include = include && btnIncludeSpans.getSelection();
        }

        // additionally check for valid annotation
        if (include && !txtSegmentFilter.getText().isEmpty() && !(node instanceof SToken)) {
          if (node.getAnnotations() != null) {
            boolean annoFound = false;
            for (SAnnotation anno : node.getAnnotations()) {
              if (anno.getName().contains(txtSegmentFilter.getText())) {
                annoFound = true;
                break;
              }
            }
            include = annoFound;
          }
        }

        return include;
      } else if (element instanceof SRelation<?, ?>) {
        SRelation<?, ?> rel = (SRelation<?, ?>) element;
        boolean include = true;
        if (rel instanceof SPointingRelation) {
          include = btnIncludePointingRelations.getSelection();
        }
        return include;
      } else {
        return true;
      }
    }

  }

  private static class STextualDataSourceComparator implements Comparator<STextualDS> {

    @Override
    public int compare(STextualDS o1, STextualDS o2) {
      return ComparisonChain.start().compare(o1.getName(), o2.getName()).result();
    }
  }

  private static class RangeStartComparator<C extends Comparable<?>>
      implements Comparator<Range<C>> {

    @Override
    public int compare(Range<C> o1, Range<C> o2) {

      return ComparisonChain.start().compare(o1.lowerEndpoint(), o2.lowerEndpoint())
          .compare(o1.upperBoundType(), o2.upperBoundType()).result();
    }
  }

}
