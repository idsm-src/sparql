package cz.iocb.chemweb.client;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RowCountChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import cz.iocb.chemweb.client.resources.datagrid.DataGridBundle;
import cz.iocb.chemweb.client.resources.pager.PagerResources;
import cz.iocb.chemweb.client.services.downloader.FileDownloader;
import cz.iocb.chemweb.client.services.query.QueryServiceStub;
import cz.iocb.chemweb.client.widgets.codemirror.CodeMirror;
import cz.iocb.chemweb.client.widgets.dialog.MessageDialog;
import cz.iocb.chemweb.client.widgets.dialog.SearchQueryWizardDialog;
import cz.iocb.chemweb.client.widgets.dialog.SelectExampleDialog;
import cz.iocb.chemweb.shared.services.DatabaseException;
import cz.iocb.chemweb.shared.services.SessionException;
import cz.iocb.chemweb.shared.services.check.CheckResult;
import cz.iocb.chemweb.shared.services.check.CheckService;
import cz.iocb.chemweb.shared.services.check.CheckServiceAsync;
import cz.iocb.chemweb.shared.services.check.CheckerWarning;
import cz.iocb.chemweb.shared.services.details.DetailsPageService;
import cz.iocb.chemweb.shared.services.details.DetailsPageServiceAsync;
import cz.iocb.chemweb.shared.services.query.DataGridNode;
import cz.iocb.chemweb.shared.services.query.QueryResult;
import cz.iocb.chemweb.shared.utils.Encode;



public class MainPage extends Composite
{
    interface MainPageUiBinder extends UiBinder<Widget, MainPage>
    {
    }


    public static enum ErrorContext
    {
        QUERY, CANCEL, DETAILS, PROPERTIES
    }


    private static class JsPrefixDefinition extends JavaScriptObject
    {
        protected JsPrefixDefinition()
        {
        }

        public final native String getName()
        /*-{
            return this.name;
        }-*/;

        public final native String getIri()
        /*-{
            return this.iri;
        }-*/;
    }


    private static class HasDataFilter implements HasData<DataGridNode[]>
    {
        private static interface IFilter
        {
            boolean isValid(DataGridNode[] value);
        }


        DataGrid<DataGridNode[]> dataGrid;
        IFilter filter = null;
        List<? extends DataGridNode[]> values = new ArrayList<DataGridNode[]>();

        private final HandlerManager handlerManager = new HandlerManager(this);
        private boolean exact = false;
        private Range range = new Range(0, 0);
        private int count = -1;


        public HasDataFilter(DataGrid<DataGridNode[]> dataGrid)
        {
            this.dataGrid = dataGrid;
        }


        @SuppressWarnings("unchecked")
        private void show()
        {
            List<DataGridNode[]> results = null;

            if(filter != null)
            {
                results = new ArrayList<DataGridNode[]>(values.size());

                for(DataGridNode[] item : values)
                    if(filter.isValid(item))
                        results.add(item);
            }
            else
            {
                results = (List<DataGridNode[]>) values;
            }

            dataGrid.setPageSize(results.size());
            dataGrid.setRowCount(results.size());
            dataGrid.setRowData(0, results);
        }


        public void setFilter(IFilter filter)
        {
            this.filter = filter;
            show();
        }


        @Override
        public HandlerRegistration addRangeChangeHandler(RangeChangeEvent.Handler handler)
        {
            return handlerManager.addHandler(RangeChangeEvent.getType(), handler);
        }

        @Override
        public HandlerRegistration addRowCountChangeHandler(RowCountChangeEvent.Handler handler)
        {
            return handlerManager.addHandler(RowCountChangeEvent.getType(), handler);
        }

        @Override
        public int getRowCount()
        {
            return count;
        }

        @Override
        public Range getVisibleRange()
        {
            return range;
        }

        @Override
        public boolean isRowCountExact()
        {
            return exact;
        }

        @Override
        public void setRowCount(int count)
        {
            setRowCount(count, true);
        }

        @Override
        public void setRowCount(int count, boolean exact)
        {
            //Window.alert("C: " + count + " - " + exact);

            this.count = count;
            this.exact = exact;
            fireEvent(new RowCountChangeEvent(count, exact)
            {
            });
        }

        @Override
        public void setVisibleRange(int start, int length)
        {
            setVisibleRange(new Range(start, length));
        }

        @Override
        public void setVisibleRange(Range range)
        {
            //Window.alert("R: " + range.getStart() + " - " + range.getLength());

            this.range = range;
            fireEvent(new RangeChangeEvent(range)
            {
            });
        }

        @Override
        public void fireEvent(GwtEvent<?> event)
        {
            handlerManager.fireEvent(event);
        }

        @Override
        public HandlerRegistration addCellPreviewHandler(CellPreviewEvent.Handler<DataGridNode[]> handler)
        {
            return handlerManager.addHandler(CellPreviewEvent.getType(), handler);
        }

        @Override
        public SelectionModel<? super DataGridNode[]> getSelectionModel()
        {
            Window.alert("getSelectionModel");
            return null;
        }

        @Override
        public DataGridNode[] getVisibleItem(int indexOnPage)
        {
            Window.alert("getVisibleItem");
            return null;
        }

        @Override
        public int getVisibleItemCount()
        {
            Window.alert("getVisibleItemCount");
            return 0;
        }

        @Override
        public Iterable<DataGridNode[]> getVisibleItems()
        {
            Window.alert("getVisibleItems");
            return null;
        }

        @Override
        public void setRowData(int start, List<? extends DataGridNode[]> values)
        {
            this.values = values;
            show();
        }

        @Override
        public void setSelectionModel(SelectionModel<? super DataGridNode[]> selectionModel)
        {
            Window.alert("setSelectionModel");

        }

        @Override
        public void setVisibleRangeAndClearData(Range range, boolean forceRangeChangeEvent)
        {
            Window.alert("setVisibleRangeAndClearData");
        }
    }


    private class AsyncResultsProvider extends AsyncDataProvider<DataGridNode[]>
    {
        private int asyncResultsCounter = 0;
        private String queryString = null;
        private boolean resetColumns = false;
        private int maxRows = 0;
        private boolean truncated = true;
        private QueryServiceStub.Query runningQuery = null;
        List<DataGridNode[]> emptyData = new LinkedList<DataGridNode[]>();


        void setQuery(String query)
        {
            // TODO: sám se začne vyhodnocovat

            queryString = query;
            resetColumns = true;
        }


        void cancelQuery()
        {
            asyncResultsCounter++;

            if(runningQuery != null)
                QueryServiceStub.cancel(runningQuery);
        }


        @Override
        protected void onRangeChanged(HasData<DataGridNode[]> display)
        {
            if(queryString == null)
                return;

            asyncResultsCounter++;

            final Range range = display.getVisibleRange();
            final int start = range.getStart();
            final int length = range.getLength();


            if(length == 0)
            {
                updateRowData(start, emptyData);
                return;
            }


            runButton.setVisible(false);
            cancelButton.setVisible(true);


            if(runningQuery != null)
                QueryServiceStub.cancel(runningQuery);


            runningQuery = QueryServiceStub.query(queryString, start, length, new AsyncCallback<QueryResult>()
            {
                int request = asyncResultsCounter;

                @Override
                public void onSuccess(final QueryResult result)
                {
                    if(request != asyncResultsCounter)
                        return; // request is not up to date


                    if(resetColumns)
                    {
                        while(resultsDataGrid.getColumnCount() > 0)
                            resultsDataGrid.removeColumn(0);

                        removeUnusedDataGridColumns(resultsDataGrid);


                        for(int i = 0; i < result.getHeads().size(); i++)
                        {
                            addColumn(resultsDataGrid, i, result.getHeads().get(i));
                            searchListBox.addItem(result.getHeads().get(i), Integer.toString(i));
                        }

                        resultsDataGrid.setMinimumTableWidth(minimalColumnWidth * result.getHeads().size(), Unit.PX);

                        resetColumns = false;
                        truncated = true;
                        maxRows = 0;
                    }

                    maxRows = Math.max(maxRows, start + result.getItems().size());
                    truncated = truncated && result.isTruncated();

                    //resultsDataGrid.setRowCount(maxRows, !truncated);
                    hasDataFilter.setRowCount(maxRows, !truncated);

                    updateRowData(start, result.getItems());

                    cancelButton.setVisible(false);
                    runButton.setVisible(true);
                }

                @Override
                public void onFailure(Throwable caught)
                {
                    if(request != asyncResultsCounter)
                        return; // request is not up to date

                    clearResults();
                    cancelButton.setVisible(false);
                    runButton.setVisible(true);

                    showErrorMessage(ErrorContext.QUERY, caught);
                }
            });
        }
    };


    private class AsyncPropertiesProvider extends AsyncDataProvider<DataGridNode[]>
    {
        private int asyncPropertiesCounter = 0;
        private String propertiesIri = null;
        private QueryServiceStub.Query runningQuery = null;
        List<DataGridNode[]> emptyData = new LinkedList<DataGridNode[]>();


        void setPropertiesIri(String iri)
        {
            propertiesIri = iri;
        }


        @Override
        protected void onRangeChanged(HasData<DataGridNode[]> display)
        {
            if(propertiesIri == null)
                return;

            asyncPropertiesCounter++;

            final Range range = display.getVisibleRange();
            final int start = range.getStart();
            final int length = range.getLength();


            if(length == 0)
            {
                updateRowData(start, emptyData);
                return;
            }


            propertiesStopButton.setVisible(true);
            propertiesReloadButton.setVisible(false);


            if(runningQuery != null)
                QueryServiceStub.cancel(runningQuery);


            String query = "SELECT distinct ?Property ?Value WHERE { " + "<" + propertiesIri + "> ?Property ?Value. "
                    + "OPTIONAL { ?Property rdfs:label ?Label. }} ORDER BY ?Label";

            runningQuery = QueryServiceStub.query(query, start, length, new AsyncCallback<QueryResult>()
            {
                int request = asyncPropertiesCounter;

                @Override
                public void onSuccess(final QueryResult result)
                {
                    if(request != asyncPropertiesCounter)
                        return; // request is not up to date

                    updateRowData(start, result.getItems());

                    propertiesStopButton.setVisible(false);
                    propertiesReloadButton.setVisible(true);
                }

                @Override
                public void onFailure(Throwable caught)
                {
                    if(request != asyncPropertiesCounter)
                        return; // request is not up to date

                    //clearProperties();
                    //showedProperties = null;
                    propertiesStopButton.setVisible(false);
                    propertiesReloadButton.setVisible(true);

                    showErrorMessage(ErrorContext.PROPERTIES, caught);
                }
            });
        }
    };


    private static MainPageUiBinder uiBinder = GWT.create(MainPageUiBinder.class);
    //private static QueryServiceAsync queryService = (QueryServiceAsync) GWT.create(QueryService.class);
    private static DetailsPageServiceAsync detailsService = (DetailsPageServiceAsync) GWT
            .create(DetailsPageService.class);
    private static CheckServiceAsync checkService = (CheckServiceAsync) GWT.create(CheckService.class);
    private static FileDownloader fileDownloader = GWT.create(FileDownloader.class);
    private static PagerResources pagerIcons = GWT.create(PagerResources.class);


    @UiField TabLayoutPanel queryTabPanel;
    @UiField PushButton openButton;
    @UiField PushButton examplesButton;
    @UiField PushButton wizardButton;
    @UiField PushButton saveButton;
    @UiField PushButton runButton;
    @UiField PushButton cancelButton;
    @UiField TextArea queryTextArea;
    @UiField(provided = true) SplitLayoutPanel mainSplitLayoutPanel;
    @UiField TabLayoutPanel resultTabPanel;
    @UiField PushButton detailsPrevButton;
    @UiField PushButton detailsNextButton;
    @UiField PushButton detailsReloadButton;
    @UiField PushButton detailsStopButton;
    @UiField TextBox detailsIriTextBox;
    @UiField PushButton propertiesPrevButton;
    @UiField PushButton propertiesNextButton;
    @UiField PushButton propertiesReloadButton;
    @UiField PushButton propertiesStopButton;
    @UiField TextBox propertiesIriTextBox;
    @UiField TextBox searchTextBox;
    @UiField ListBox searchListBox;
    @UiField TabLayoutPanel infoTabPanel;
    @UiField DockLayoutPanel detailsTab;
    @UiField DockLayoutPanel propertiesTab;
    @UiField(provided = true) DataGrid<DataGridNode[]> resultsDataGrid;
    @UiField(provided = true) DataGrid<DataGridNode[]> propertiesDataGrid;
    @UiField(provided = true) SimplePager propertiesPager;
    @UiField(provided = true) SimplePager resultsPager;
    @UiField ScrollPanel pageScrollPanel;
    @UiField HTML pageHTML;
    @UiField HTML aboutHTML;

    private final int minimalColumnWidth = 250;
    private final int minCentralWidth = 300;

    //private Long queryID = null;
    //private boolean cancel = false;
    //private final Long propertiesQueryID = null;
    private CodeMirror codemirror;

    //private final FilteredListDataProvider<DataGridNode[]> dataProvider = new FilteredListDataProvider<DataGridNode[]>();
    private final HasDataFilter hasDataFilter;
    private final AsyncResultsProvider resultsAsyncProvider = new AsyncResultsProvider();
    private final AsyncPropertiesProvider propertiesAsyncProvider = new AsyncPropertiesProvider();

    private final Stack<String> prevStack = new Stack<String>();
    private final Stack<String> nextStack = new Stack<String>();
    private String currentIri;
    private String showedProperties;
    private String showedDetails;

    private int requestedDetailsCounter = 0;
    private int requestedPropertiesCounter = 0;

    private final SelectExampleDialog exampleDialog;
    private final SearchQueryWizardDialog wizardDialog;

    private final Element styleOfSelected;

    RegExp URLreqexp = null;
    RegExp BNreqexp = null;



    public MainPage()
    {
        mainSplitLayoutPanel = new SplitLayoutPanel()
        {
            @Override
            public void onResize()
            {
                super.onResize();

                if(codemirror != null)
                    codemirror.refresh();
            }
        };



        resultsDataGrid = new DataGrid<DataGridNode[]>(1, DataGridBundle.INSTANCE)
        {
            // https://code.google.com/p/google-web-toolkit/issues/detail?id=6865
            @Override
            public void setRowData(int start, List<? extends DataGridNode[]> values)
            {
                final HeaderPanel headerPanel = (HeaderPanel) getWidget();
                final ScrollPanel scrollPanel = (ScrollPanel) headerPanel.getContentWidget();
                scrollPanel.scrollToTop();
                super.setRowData(start, values);
            }
        };

        //resultsAsyncProvider.addDataDisplay(resultsDataGrid);



        propertiesDataGrid = new DataGrid<DataGridNode[]>(1, DataGridBundle.INSTANCE)
        {
            // https://code.google.com/p/google-web-toolkit/issues/detail?id=6865
            @Override
            public void setRowData(int start, List<? extends DataGridNode[]> values)
            {
                final HeaderPanel headerPanel = (HeaderPanel) getWidget();
                final ScrollPanel scrollPanel = (ScrollPanel) headerPanel.getContentWidget();
                scrollPanel.scrollToTop();
                super.setRowData(start, values);
            }
        };

        propertiesDataGrid.setMinimumTableWidth(2 * minimalColumnWidth, Unit.PX);

        addColumn(propertiesDataGrid, 0, "Property");
        addColumn(propertiesDataGrid, 1, "Value");


        propertiesPager = new SimplePager(SimplePager.TextLocation.RIGHT, pagerIcons, false, 0, true)
        {
            @Override
            public void setPageStart(int index)
            {
                if(getDisplay() != null)
                {
                    Range range = getDisplay().getVisibleRange();
                    int pageSize = range.getLength();

                    if(isRangeLimited() && getDisplay().isRowCountExact())
                        index = Math.min(index, getDisplay().getRowCount() - 1);

                    index = Math.max(0, index);

                    if(index != range.getStart())
                        getDisplay().setVisibleRange(index, pageSize);
                }
            }
        };


        resultsPager = new SimplePager(SimplePager.TextLocation.RIGHT, pagerIcons, false, 0, true)
        {
            @Override
            public void setPageStart(int index)
            {
                if(getDisplay() != null)
                {
                    Range range = getDisplay().getVisibleRange();
                    int pageSize = range.getLength();

                    if(isRangeLimited() && getDisplay().isRowCountExact())
                        index = Math.min(index, getDisplay().getRowCount() - 1);

                    index = Math.max(0, index);

                    if(index != range.getStart())
                        getDisplay().setVisibleRange(index, pageSize);
                }
            }
        };


        hasDataFilter = new HasDataFilter(resultsDataGrid);


        initWidget(uiBinder.createAndBindUi(this));


        //resultsDataGrid.setPageSize(0);
        //resultsDataGrid.setRowCount(0, true);
        //hasDataFilter.setPageSize(0);
        hasDataFilter.setRowCount(0, true);
        hasDataFilter.setVisibleRange(0, 0);

        resultsAsyncProvider.addDataDisplay(hasDataFilter);
        resultsPager.setDisplay(hasDataFilter);


        propertiesDataGrid.setPageSize(0);
        propertiesDataGrid.setRowCount(0, true);
        propertiesAsyncProvider.addDataDisplay(propertiesDataGrid);
        propertiesPager.setDisplay(propertiesDataGrid);


        infoTabPanel.addSelectionHandler(new SelectionHandler<Integer>()
        {
            @Override
            public void onSelection(final SelectionEvent<Integer> event)
            {
                showInfo(currentIri, false);
            }
        });

        pageHTML.getElement().setId("pageHTML");

        styleOfSelected = DOM.createElement("style");
        Document.get().getHead().appendChild(styleOfSelected);


        Element styleOfNonSelected = DOM.createElement("style");
        styleOfNonSelected.setInnerText(".BOX-selection { border-left: 2px solid transparent;}");
        Document.get().getHead().appendChild(styleOfNonSelected);


        Window.addResizeHandler(new ResizeHandler()
        {
            @Override
            public void onResize(ResizeEvent event)
            {
                resetSplitLayoutSize();
            }
        });

        mainSplitLayoutPanel.setWidgetMinSize(resultTabPanel, minCentralWidth);
        detailsIriTextBox.getElement().setAttribute("spellCheck", "false");
        propertiesIriTextBox.getElement().setAttribute("spellCheck", "false");
        searchTextBox.getElement().setAttribute("spellCheck", "false");

        exampleDialog = new SelectExampleDialog();
        wizardDialog = new SearchQueryWizardDialog();
    }


    private native void initJS()
    /*-{
        var self = this;

        function handleFileSelect(evt) {
            var file = evt.target.files[0];

            //if (!file.type.match('text.*'))
            //  return;

            if (!$wnd.File || !$wnd.FileReader || !$wnd.FileList || !$wnd.Blob)
                $wnd
                        .alert("The File APIs are not fully supported by your browser.");

            var reader = new $wnd.FileReader();

            reader.onload = (function(theFile) {
                return function(e) {
                    self.@cz.iocb.chemweb.client.MainPage::openClick(Ljava/lang/String;)(e.target.result);
                };
            })(file);

            reader.readAsText(file);
        }

        function handlingMsg(e) {
            self.@cz.iocb.chemweb.client.MainPage::goToIri(Ljava/lang/String;)(e.data);
        }

        $wnd.document.getElementById("fileUpload").addEventListener("change",
                handleFileSelect, false);

        $wnd.addEventListener("message", handlingMsg, true);
    }-*/;


    @Override
    protected void onAttach()
    {
        super.onAttach();
        initJS();
    }


    @Override
    public void onLoad()
    {
        super.onLoad();


        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand()
        {
            @Override
            public void execute()
            {
                resetSplitLayoutSize();
                codemirror = new CodeMirror(queryTextArea);
                codemirror.setValue(getDefultQuery());


                goToIri(getStartIri());

                codemirror.setValidator(new CodeMirror.Validator()
                {
                    @Override
                    public void validate(final String code, final JavaScriptObject callbackFunction,
                            final JavaScriptObject options, final JavaScriptObject cm)
                    {
                        if(code.trim().equals(""))
                        {
                            JsArray<JavaScriptObject> warnings = JavaScriptObject.createArray().cast();
                            CodeMirror.callback(callbackFunction, warnings, cm);

                            runButton.setEnabled(false);
                            return;
                        }

                        checkService.check(code, new AsyncCallback<CheckResult>()
                        {
                            @Override
                            public void onSuccess(CheckResult result)
                            {
                                if(!code.equals(codemirror.getValue()))
                                    return;

                                JsArray<JavaScriptObject> warnings = JavaScriptObject.createArray().cast();

                                boolean error = false;

                                for(CheckerWarning warn : result.warnings)
                                {
                                    error = error || warn.type.equals("error");

                                    JavaScriptObject w = CodeMirror.createWarning(warn.beginLine, warn.beginColumnt,
                                            warn.endLine, warn.endColumnt, warn.type, warn.message);
                                    warnings.push(w);
                                }

                                runButton.setEnabled(!error);
                                CodeMirror.callback(callbackFunction, warnings, cm);
                            }

                            @Override
                            public void onFailure(Throwable caught)
                            {
                                if(!code.equals(codemirror.getValue()))
                                    return;

                                JsArray<JavaScriptObject> warnings = JavaScriptObject.createArray().cast();

                                warnings.push(CodeMirror.createWarning(0, 0, 0, 0, "warning",
                                        "The connection with the server has failed."));

                                runButton.setEnabled(true);
                                CodeMirror.callback(callbackFunction, warnings, cm);
                            }
                        });
                    }
                });
            }
        });
    }

    /**************************************************************************/


    public void openClick(String code)
    {
        codemirror.setValue(code);
    }


    @UiHandler("examplesButton")
    void examplesButtonClick(ClickEvent e)
    {
        exampleDialog.open(new SelectExampleDialog.Callback()
        {
            @Override
            public void onSelection(String code)
            {
                codemirror.setValue(code);
            }
        });

        ((PushButton) e.getSource()).setHovering(false);
    }


    @UiHandler("wizardButton")
    void wizardButtonClick(ClickEvent e)
    {
        wizardDialog.open(new SearchQueryWizardDialog.Callback()
        {
            @Override
            public void onSelection(String code)
            {
                codemirror.setValue(code);
            }
        });

        ((PushButton) e.getSource()).setHovering(false);
    }


    @UiHandler("saveButton")
    void saveButtonClick(ClickEvent e)
    {
        String data = Encode.base64(codemirror.getValue());
        fileDownloader.download("query.sparql", "application/x-sparql-query", data);
    }


    @UiHandler("runButton")
    void runButtonClick(ClickEvent e)
    {
        clearResults();

        resultsAsyncProvider.setQuery(codemirror.getValue());
        //resultsDataGrid.setPageSize(1000);
        //resultsDataGrid.setRowCount(1000);
        //hasDataFilter.setPageSize(1000);
        hasDataFilter.setRowCount(0);
        hasDataFilter.setVisibleRange(0, 1000);


        /*
        queryID = null;
        cancel = false;
        
        cancelButton.setVisible(true);
        runButton.setVisible(false);
        
        queryService.query(codemirror.getValue(), new AsyncCallback<Long>()
        {
            @Override
            public void onSuccess(Long result)
            {
                if(cancel)
                {
                    cancelButtonClick(null);
                    return;
                }
        
                queryID = result;
                final long savedQueryID = result;
        
                queryService.getResult(savedQueryID, new AsyncCallback<QueryResult>()
                {
                    @Override
                    public void onSuccess(QueryResult result)
                    {
                        if(queryID == null || queryID.longValue() != savedQueryID) // query has been canceled
                            return;
        
                        clearResults();
        
                        for(int i = 0; i < result.getHeads().size(); i++)
                        {
                            addColumn(resultsDataGrid, i, result.getHeads().get(i));
                            searchListBox.addItem(result.getHeads().get(i), Integer.toString(i));
                        }
        
                        resultsDataGrid.setMinimumTableWidth(minimalColumnWidth * result.getHeads().size(), Unit.PX);
        
                        resultsDataGrid.setPageSize(result.getItems().size());
                        resultsDataGrid.setRowCount(result.getItems().size(), true);
                        //resultsDataGrid.setRowData(0, result.items);
                        dataProvider.getList().addAll(result.getItems());
        
                        //runButton.setEnabled(true);
                        cancelButton.setVisible(false);
                        runButton.setVisible(true);
        
        
                        if(result.isTruncated())
                        {
                            int size = result.getItems().size();
        
                            String message = "The query has more than " + size + " solutions. Only the first " + size
                                    + " solutions will be displayed. Please, use a more specific query.";
        
                            final MessageDialog dialog = new MessageDialog("Warning Message", message);
        
                            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand()
                            {
                                @Override
                                public void execute()
                                {
                                    dialog.center();
                                    disablePushButtonHover();
                                }
                            });
                        }
                    }
        
                    @Override
                    public void onFailure(Throwable caught)
                    {
                        if(queryID == null || queryID.longValue() != savedQueryID) // query has been canceled
                            return;
        
                        clearResults();
        
                        //runButton.setEnabled(true);
                        cancelButton.setVisible(false);
                        runButton.setVisible(true);
        
                        showErrorMessage(ErrorContext.QUERY, caught);
                    }
                });
            }
        
            @Override
            public void onFailure(Throwable caught)
            {
                if(caught instanceof QueryException)
                    return; // The error should be reported in the editor.
        
                clearResults();
        
                cancelButton.setVisible(false);
                runButton.setVisible(true);
        
                showErrorMessage(ErrorContext.QUERY, caught);
            }
        });
        */
    }


    @UiHandler("cancelButton")
    void cancelButtonClick(ClickEvent e)
    {
        resultsAsyncProvider.cancelQuery();

        cancelButton.setVisible(false);
        runButton.setVisible(true);
    }


    /**************************************************************************/


    @UiHandler("searchTextBox")
    void complexesTextBoxKeyUp(KeyUpEvent e)
    {
        resetFilter();
    }


    @UiHandler("searchListBox")
    void searchListBoxChangeEvent(ChangeEvent e)
    {
        resetFilter();
    }


    private void resetFilter()
    {
        final String value = searchTextBox.getValue();
        final int index = Integer.parseInt(searchListBox.getSelectedValue());


        hasDataFilter.setFilter(new HasDataFilter.IFilter()
        {
            @Override
            public boolean isValid(DataGridNode[] row)
            {
                if(index != -1)
                    return row[index].html.replaceAll("<[^>]*>", " ").indexOf(value) >= 0;

                for(int i = 0; i < row.length; i++)
                    if(row[i].html.replaceAll("<[^>]*>", " ").indexOf(value) >= 0)
                        return true;

                return false;
            }
        });
    }


    private void addColumn(DataGrid<DataGridNode[]> grid, final int i, final String name)
    {
        LinkCell cell = new LinkCell()
        {
            @Override
            public void onBrowserEvent(Context context, Element parent, DataGridNode value, NativeEvent event,
                    ValueUpdater<DataGridNode> valueUpdater)
            {
                if(event.getType() == "click"
                        || event.getType() == "keypress" && event.getKeyCode() == KeyCodes.KEY_ENTER)
                {
                    if(value.ref != null)
                        goToIri(value.ref);
                }

                super.onBrowserEvent(context, parent, value, event, valueUpdater);
            }
        };

        Column<DataGridNode[], DataGridNode> column = new Column<DataGridNode[], DataGridNode>(cell)
        {
            @Override
            public DataGridNode getValue(DataGridNode[] object)
            {
                return object[i];
            }

        };

        grid.addColumn(column, name);
    }


    private static void removeUnusedDataGridColumns(DataGrid<?> dataGrid)
    {
        int columnCount = dataGrid.getColumnCount();
        NodeList<Element> colGroups = dataGrid.getElement().getElementsByTagName("colgroup");

        for(int i = 0; i < colGroups.getLength(); i++)
        {
            Element colGroupEle = colGroups.getItem(i);
            NodeList<Element> colList = colGroupEle.getElementsByTagName("col");

            for(int j = colList.getLength() - 1; j >= columnCount; j--)
            {
                colGroupEle.removeChild(colList.getItem(j));
            }
        }
    }


    /**************************************************************************/


    @UiHandler({ "detailsPrevButton", "propertiesPrevButton" })
    void prevButtonClick(ClickEvent e)
    {
        nextStack.push(currentIri);
        currentIri = prevStack.pop();
        showIri(currentIri);
    }

    @UiHandler({ "detailsNextButton", "propertiesNextButton" })
    void nextButtonClick(ClickEvent e)
    {
        prevStack.push(currentIri);
        currentIri = nextStack.pop();
        showIri(currentIri);
    }

    @UiHandler({ "detailsReloadButton", "propertiesReloadButton" })
    void reloadButtonClick(ClickEvent e)
    {
        showInfo(currentIri, true);
    }

    @UiHandler("detailsIriTextBox")
    void detailsIriTextBoxChange(ValueChangeEvent<String> e)
    {
        propertiesIriTextBox.setValue(detailsIriTextBox.getValue());
    }

    @UiHandler("propertiesIriTextBox")
    void propertiesIriTextBoxChange(ValueChangeEvent<String> e)
    {
        detailsIriTextBox.setValue(propertiesIriTextBox.getValue());
    }

    @UiHandler({ "detailsIriTextBox", "propertiesIriTextBox" })
    void iriTextBoxKeyUp(KeyUpEvent event)
    {
        if(KeyCodes.KEY_ENTER != event.getNativeEvent().getKeyCode())
        {
            detailsIriTextBox.removeStyleDependentName("error");
            propertiesIriTextBox.removeStyleDependentName("error");
        }
    }

    @UiHandler({ "detailsIriTextBox", "propertiesIriTextBox" })
    void iriTextBoxKeyPress(KeyPressEvent event)
    {
        if(KeyCodes.KEY_ENTER == event.getNativeEvent().getKeyCode())
        {
            if(URLreqexp == null)
                URLreqexp = RegExp.compile(
                        "^((ftp|http|https)://[\\w@.\\-\\_]+\\.[a-zA-Z]{2,}(:\\d{1,5})?(/[\\w#!:.?+=&%@!\\_\\-/]+)*){1}$");

            if(BNreqexp == null)
                BNreqexp = RegExp.compile("^(nodeID://[a-zA-Z0-90]+)$");

            String value = ((TextBox) event.getSource()).getValue();

            value = deprefixedIRI(value);
            //detailsIriTextBox.setValue(prefixedIRI(value));
            //propertiesIriTextBox.setValue(prefixedIRI(value));


            if(URLreqexp.exec(value) != null || BNreqexp.exec(value) != null)
            {
                detailsIriTextBox.removeStyleDependentName("error");
                propertiesIriTextBox.removeStyleDependentName("error");

                goToIri(value);
            }
            else
            {
                detailsIriTextBox.addStyleDependentName("error");
                propertiesIriTextBox.addStyleDependentName("error");
            }
        }
    }

    private void goToIri(String iri)
    {
        if(iri.equals(currentIri))
            return;

        if(currentIri != null)
            prevStack.push(currentIri);

        nextStack.clear();

        currentIri = iri;
        showIri(currentIri);
    }

    private void showIri(String iri)
    {
        detailsPrevButton.setEnabled(!prevStack.isEmpty());
        detailsNextButton.setEnabled(!nextStack.isEmpty());
        detailsReloadButton.setEnabled(true);
        detailsIriTextBox.setValue(prefixedIRI(iri));

        propertiesPrevButton.setEnabled(!prevStack.isEmpty());
        propertiesNextButton.setEnabled(!nextStack.isEmpty());
        propertiesReloadButton.setEnabled(true);
        propertiesIriTextBox.setValue(prefixedIRI(iri));

        detailsIriTextBox.removeStyleDependentName("error");
        propertiesIriTextBox.removeStyleDependentName("error");

        showInfo(iri, false);
    }

    private void showInfo(String iri, boolean forced)
    {
        styleOfSelected
                .setInnerText(".BOX-NODE_" + Encode.base32m(iri) + " { border-left: 2px solid #aaa !important; }");

        if(infoTabPanel.getWidget(infoTabPanel.getSelectedIndex()) == detailsTab)
            showDetails(iri, forced);
        else if(infoTabPanel.getWidget(infoTabPanel.getSelectedIndex()) == propertiesTab)
            showProperties(iri, forced);
    }


    private void showDetails(final String iri, boolean forced)
    {
        requestedDetailsCounter++;

        if(!forced && iri.equals(showedDetails))
        {
            detailsStopButton.setVisible(false);
            detailsReloadButton.setVisible(true);
            return;
        }

        detailsReloadButton.setVisible(false);
        detailsStopButton.setVisible(true);
        //clearDetails();

        detailsService.details(iri, new AsyncCallback<String>()
        {
            int request = requestedDetailsCounter;

            @Override
            public void onSuccess(String result)
            {
                if(request != requestedDetailsCounter)
                    return; // request is not up to date

                pageHTML.setHTML(result);
                pageScrollPanel.scrollToTop();

                String elementId = "SEL-NODE_" + Encode.base32m(iri);
                Element element = DOM.getElementById(elementId);

                if(element != null)
                    element.scrollIntoView();

                showedDetails = iri;
                detailsStopButton.setVisible(false);
                detailsReloadButton.setVisible(true);
            }

            @Override
            public void onFailure(Throwable caught)
            {
                if(request != requestedDetailsCounter)
                    return; // request is not up to date

                clearDetails();

                showedDetails = null;
                detailsStopButton.setVisible(false);
                detailsReloadButton.setVisible(true);

                showErrorMessage(ErrorContext.DETAILS, caught);
            }
        });
    }


    private void showProperties(final String iri, boolean forced)
    {
        propertiesDataGrid.redraw(); // DataGrig in TabLayoutPanel workaround

        requestedPropertiesCounter++;

        if(!forced && iri.equals(showedProperties))
        {
            propertiesStopButton.setVisible(false);
            propertiesReloadButton.setVisible(true);
            return;
        }

        propertiesReloadButton.setVisible(false);
        propertiesStopButton.setVisible(true);
        clearProperties();


        QueryServiceStub.countOfProperties(iri, new AsyncCallback<Integer>()
        {
            int request = requestedPropertiesCounter;

            @Override
            public void onSuccess(Integer result)
            {
                if(request != requestedPropertiesCounter)
                    return; // request is not up to date

                propertiesAsyncProvider.setPropertiesIri(iri);
                propertiesDataGrid.setRowCount(result, true);
                propertiesDataGrid.setPageSize(0);
                propertiesDataGrid.setPageStart(0);
                propertiesDataGrid.setPageSize(100);

                showedProperties = iri;
                propertiesStopButton.setVisible(false);
                propertiesReloadButton.setVisible(true);
            }

            @Override
            public void onFailure(Throwable caught)
            {
                if(request != requestedPropertiesCounter)
                    return; // request is not up to date

                clearProperties();

                showedProperties = null;
                propertiesStopButton.setVisible(false);
                propertiesReloadButton.setVisible(true);

                showErrorMessage(ErrorContext.PROPERTIES, caught);
            }
        });
    }


    private void resetSplitLayoutSize()
    {
        int border = 2;
        int margin = 20;

        int queryPanelWidth = Math.max(queryTabPanel.getOffsetWidth() - border, 0);
        int infoPanelWidth = Math.max(infoTabPanel.getOffsetWidth() - border, 0);

        int mainPanelWidth = mainSplitLayoutPanel.getOffsetWidth() - border - margin;
        int resultPanelWidth = mainPanelWidth - queryPanelWidth - infoPanelWidth;

        if(resultPanelWidth < minCentralWidth)
        {
            int diff = minCentralWidth - resultPanelWidth;

            // TODO: Make it better ...
            if(queryPanelWidth == 0)
            {
                mainSplitLayoutPanel.setWidgetSize(infoTabPanel, infoPanelWidth - diff);
            }
            else if(infoPanelWidth == 0)
            {
                mainSplitLayoutPanel.setWidgetSize(queryTabPanel, queryPanelWidth - diff);
            }
            else if(queryPanelWidth > infoPanelWidth)
            {
                double r = (double) queryPanelWidth / (queryPanelWidth + infoPanelWidth);
                int w1 = (int) Math.ceil(r * diff); // we take more from wider
                int w2 = diff - w1;

                mainSplitLayoutPanel.setWidgetSize(queryTabPanel, queryPanelWidth - w1);
                mainSplitLayoutPanel.setWidgetSize(infoTabPanel, infoPanelWidth - w2);
            }
            else
            {
                double r = (double) infoPanelWidth / (queryPanelWidth + infoPanelWidth);
                int w2 = (int) Math.ceil(r * diff); // we take more from wider
                int w1 = diff - w2;

                mainSplitLayoutPanel.setWidgetSize(queryTabPanel, queryPanelWidth - w1);
                mainSplitLayoutPanel.setWidgetSize(infoTabPanel, infoPanelWidth - w2);
            }
        }
    }



    private static String prefixedIRI(String url)
    {
        JsArray<JsPrefixDefinition> data = getPrefixes();

        for(int i = 0; i < data.length(); ++i)
        {
            JsPrefixDefinition prefix = data.get(i);

            if(url.startsWith(prefix.getIri()))
                return url.replaceFirst(prefix.getIri(), prefix.getName());
        }

        return url;
    }


    private static String deprefixedIRI(String url)
    {
        JsArray<JsPrefixDefinition> data = getPrefixes();

        for(int i = 0; i < data.length(); ++i)
        {
            JsPrefixDefinition prefix = data.get(i);

            if(url.startsWith(prefix.getName()))
                return url.replaceFirst(prefix.getName(), prefix.getIri());
        }

        return url;
    }


    private void clearResults()
    {
        //List<DataGridNode[]> empty = new Vector<DataGridNode[]>();
        //resultsDataGrid.setPageSize(0);
        //resultsDataGrid.setRowCount(0, true);
        //resultsDataGrid.setRowData(0, empty);
        //dataProvider.getList().clear();

        searchListBox.clear();
        searchListBox.addItem("any", "-1");
        searchTextBox.setValue("");
        resetFilter();


        while(resultsDataGrid.getColumnCount() > 0)
            resultsDataGrid.removeColumn(0);

        removeUnusedDataGridColumns(resultsDataGrid);
    }

    private void clearDetails()
    {
        pageHTML.setHTML("");
    }

    private void clearProperties()
    {
        propertiesDataGrid.setPageSize(0);
        propertiesDataGrid.setPageStart(0);
    }


    private void disablePushButtonHover()
    {
        openButton.setHovering(false);
        examplesButton.setHovering(false);
        wizardButton.setHovering(false);
        saveButton.setHovering(false);
        runButton.setHovering(false);
        cancelButton.setHovering(false);
        detailsPrevButton.setHovering(false);
        detailsNextButton.setHovering(false);
        detailsReloadButton.setHovering(false);
        detailsStopButton.setHovering(false);
        propertiesPrevButton.setHovering(false);
        propertiesNextButton.setHovering(false);
        propertiesReloadButton.setHovering(false);
        propertiesStopButton.setHovering(false);
    }


    private void showErrorMessage(ErrorContext context, Throwable error)
    {
        String message = null;

        if(error instanceof StatusCodeException)
        {
            message = "The connection with the server has failed.";
        }
        else if(error instanceof SessionException)
        {
            message = error.getMessage();
        }
        else if(error instanceof DatabaseException)
        {
            message = "The database server has returned the following error: " + error.getMessage();
        }
        else
        {
            message = "The unexpected exception \"" + error.getClass().getSimpleName() + "\" has been catched: "
                    + error.getMessage();
        }

        final MessageDialog dialog = new MessageDialog("Error Message", message);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand()
        {
            @Override
            public void execute()
            {
                dialog.center();
                disablePushButtonHover();
            }
        });
    }


    public final native static JsArray<JsPrefixDefinition> getPrefixes()
    /*-{
        return $wnd.prefixes;
    }-*/;


    public final native static String getStartIri()
    /*-{
        return $wnd.startIri;
    }-*/;


    public final native static String getDefultQuery()
    /*-{
        return $wnd.examples[0].code;
    }-*/;
}
