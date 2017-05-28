package cz.iocb.chemweb.client.widgets.dialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;




public class SearchQueryWizardDialog extends DialogBox
{
    private static SearchQueryWizardDialogUiBinder uiBinder = GWT.create(SearchQueryWizardDialogUiBinder.class);

    private Callback callback;

    interface SearchQueryWizardDialogUiBinder extends UiBinder<Widget, SearchQueryWizardDialog>
    {
    }

    public static abstract class Callback
    {
        abstract public void onSelection(String code);
    }

    private static enum Methods
    {
        SUBSEARCH, SIMSEARCH, EXACTSEARCH, SMARTSEARCH;
    }


    @UiField CheckBox searchCompoundsCheckBox;
    @UiField CheckBox searchBioassaysCheckBox;

    @UiField ListBox queryMethodListBox;
    @UiField ListBox queryTypeListBox;
    @UiField TextArea queryTextArea;
    @UiField FileUpload queryFileUpload;
    @UiField TextBox resultLimitTextBox;
    @UiField Label resultThresholdLabel;
    @UiField TextBox resultThresholdTextBox;
    @UiField Label useTautomersLabel;
    @UiField ListBox useTautomersListBox;

    @UiField TextArea keywordsTextArea;
    @UiField CheckBox activeStatusCheckBox;
    @UiField CheckBox inactiveStatusCheckBox;
    @UiField CheckBox inconclusiveStatusCheckBox;
    @UiField CheckBox probeStatusCheckBox;
    @UiField CheckBox unspecifiedStatusCheckBox;

    @UiField CheckBox compoundCheckBox;
    @UiField CheckBox scoreCheckBox;
    @UiField CheckBox bioassayCheckBox;
    @UiField CheckBox statusCheckBox;

    @UiField ListBox orderByListBox;

    @UiField Button finishButton;



    @UiHandler("searchCompoundsCheckBox")
    void searchCompoundsCheckBoxValueChange(ValueChangeEvent<Boolean> event)
    {
        queryMethodListBox.setEnabled(event.getValue());
        queryTypeListBox.setEnabled(event.getValue());
        queryTextArea.setEnabled(event.getValue());
        queryTextArea.setEnabled(event.getValue());
        resultLimitTextBox.setEnabled(event.getValue());
        resultThresholdTextBox.setEnabled(event.getValue());
        useTautomersListBox.setEnabled(event.getValue());

        compoundCheckBox.setEnabled(event.getValue());
        scoreCheckBox.setEnabled(
                event.getValue() && Methods.valueOf(queryMethodListBox.getSelectedValue()) == Methods.SIMSEARCH);
        statusCheckBox.setEnabled(event.getValue() && searchBioassaysCheckBox.getValue());

        if(searchBioassaysCheckBox.getValue())
        {
            activeStatusCheckBox.setEnabled(event.getValue());
            inactiveStatusCheckBox.setEnabled(event.getValue());
            inconclusiveStatusCheckBox.setEnabled(event.getValue());
            probeStatusCheckBox.setEnabled(event.getValue());
            unspecifiedStatusCheckBox.setEnabled(event.getValue());
        }

        finishButton.setEnabled(areValuesValid());
    }

    @UiHandler("searchBioassaysCheckBox")
    void searchBioassaysCheckBoxValueChange(ValueChangeEvent<Boolean> event)
    {
        keywordsTextArea.setEnabled(event.getValue());

        bioassayCheckBox.setEnabled(event.getValue());
        statusCheckBox.setEnabled(event.getValue() && searchCompoundsCheckBox.getValue());


        if(searchCompoundsCheckBox.getValue())
        {
            activeStatusCheckBox.setEnabled(event.getValue());
            inactiveStatusCheckBox.setEnabled(event.getValue());
            inconclusiveStatusCheckBox.setEnabled(event.getValue());
            probeStatusCheckBox.setEnabled(event.getValue());
            unspecifiedStatusCheckBox.setEnabled(event.getValue());
        }

        finishButton.setEnabled(areValuesValid());
    }

    @UiHandler("queryMethodListBox")
    void queryMethodListBoxChange(ChangeEvent e)
    {
        switch(Methods.valueOf(queryMethodListBox.getSelectedValue()))
        {
            case SUBSEARCH:
            case EXACTSEARCH:
                resultThresholdLabel.setVisible(false);
                resultThresholdTextBox.setVisible(false);
                useTautomersLabel.setVisible(true);
                useTautomersListBox.setVisible(true);
                resultThresholdTextBox.getElement().getStyle().setZIndex(1);
                useTautomersListBox.getElement().getStyle().setZIndex(2);
                scoreCheckBox.setEnabled(false);
                break;

            case SIMSEARCH:
                resultThresholdLabel.setVisible(true);
                resultThresholdTextBox.setVisible(true);
                useTautomersLabel.setVisible(false);
                useTautomersListBox.setVisible(false);
                resultThresholdTextBox.getElement().getStyle().setZIndex(2);
                useTautomersListBox.getElement().getStyle().setZIndex(1);
                scoreCheckBox.setEnabled(true);
                break;

            case SMARTSEARCH:
                resultThresholdLabel.setVisible(false);
                resultThresholdTextBox.setVisible(false);
                useTautomersLabel.setVisible(false);
                useTautomersListBox.setVisible(false);
                scoreCheckBox.setEnabled(false);
                break;
        }

        finishButton.setEnabled(areValuesValid());
    }


    @UiHandler("resultLimitTextBox")
    void resultLimitTextBoxKeyUp(KeyUpEvent e)
    {
        String color = isResultLimitTextBoxValid() ? null : "red";
        resultLimitTextBox.getElement().getStyle().setColor(color);
        resultLimitTextBox.getElement().getStyle().setBorderColor(color);
        finishButton.setEnabled(areValuesValid());
    }

    @UiHandler("resultLimitTextBox")
    void resultLimitTextBoxValueChange(ValueChangeEvent<String> e)
    {
        String color = isResultLimitTextBoxValid() ? null : "red";
        resultLimitTextBox.getElement().getStyle().setColor(color);
        resultLimitTextBox.getElement().getStyle().setBorderColor(color);
        finishButton.setEnabled(areValuesValid());
    }

    @UiHandler("resultThresholdTextBox")
    void resultThresholdTextBoxKeyUp(KeyUpEvent e)
    {
        String color = isResultThresholdTextBoxValid() ? null : "red";
        resultThresholdTextBox.getElement().getStyle().setColor(color);
        resultThresholdTextBox.getElement().getStyle().setBorderColor(color);
        finishButton.setEnabled(areValuesValid());
    }

    @UiHandler("resultThresholdTextBox")
    void resultThresholdTextBoxValueChange(ValueChangeEvent<String> e)
    {
        String color = isResultThresholdTextBoxValid() ? null : "red";
        resultThresholdTextBox.getElement().getStyle().setColor(color);
        resultThresholdTextBox.getElement().getStyle().setBorderColor(color);
        finishButton.setEnabled(areValuesValid());
    }

    @UiHandler("queryTextArea")
    void queryTextAreaKeyUp(KeyUpEvent e)
    {
        finishButton.setEnabled(areValuesValid());
    }

    @UiHandler("queryTextArea")
    void queryTextAreaValueChange(ValueChangeEvent<String> e)
    {
        finishButton.setEnabled(areValuesValid());
    }

    @UiHandler("keywordsTextArea")
    void keywordsTextAreaKeyUp(KeyUpEvent e)
    {
        finishButton.setEnabled(areValuesValid());
    }

    @UiHandler("keywordsTextArea")
    void keywordsTextAreaValueChange(ValueChangeEvent<String> e)
    {
        finishButton.setEnabled(areValuesValid());
    }

    @UiHandler("compoundCheckBox")
    void compoundCheckBoxValueChange(ValueChangeEvent<Boolean> event)
    {
        finishButton.setEnabled(areValuesValid());
    }

    @UiHandler("scoreCheckBox")
    void scoreCheckBoxValueChange(ValueChangeEvent<Boolean> event)
    {
        finishButton.setEnabled(areValuesValid());
    }

    @UiHandler("bioassayCheckBox")
    void bioassayCheckBoxValueChange(ValueChangeEvent<Boolean> event)
    {
        finishButton.setEnabled(areValuesValid());
    }

    @UiHandler("statusCheckBox")
    void statusCheckBoxValueChange(ValueChangeEvent<Boolean> event)
    {
        finishButton.setEnabled(areValuesValid());
    }



    @UiHandler("finishButton")
    void finishButtonClick(ClickEvent e)
    {
        if(!areValuesValid())
            return;

        hide();
        callback.onSelection(generateQuery());
    }

    @UiHandler("cancelButton")
    void cancelButtonClick(ClickEvent e)
    {
        hide();
    }

    public void open(Callback callback)
    {
        this.callback = callback;
        center();
    }

    private boolean areValuesValid()
    {
        if(!searchCompoundsCheckBox.getValue() && !searchBioassaysCheckBox.getValue())
            return false;

        if(searchCompoundsCheckBox.getValue() && queryTextArea.getValue().trim().isEmpty()
                && !searchBioassaysCheckBox.getValue())
            return false;

        if(searchBioassaysCheckBox.getValue() && keywordsTextArea.getValue().trim().isEmpty()
                && !searchCompoundsCheckBox.getValue())
            return false;

        if(searchCompoundsCheckBox.getValue())
        {
            if(!isResultLimitTextBoxValid())
                return false;

            if(resultThresholdTextBox.isVisible() && !isResultThresholdTextBoxValid())
                return false;
        }


        int selections = 0;

        if(compoundCheckBox.getValue() && searchCompoundsCheckBox.getValue())
            selections++;

        if(scoreCheckBox.getValue() && searchCompoundsCheckBox.getValue()
                && Methods.valueOf(queryMethodListBox.getSelectedValue()) == Methods.SIMSEARCH)
            selections++;

        if(bioassayCheckBox.getValue() && searchBioassaysCheckBox.getValue())
            selections++;

        if(statusCheckBox.getValue() && searchCompoundsCheckBox.getValue() && searchBioassaysCheckBox.getValue())
            selections++;

        if(selections == 0)
            return false;


        return true;
    }


    private boolean isResultThresholdTextBoxValid()
    {
        try
        {
            String text = resultThresholdTextBox.getValue().trim();

            if(text.isEmpty())
                return true;

            double val = Double.valueOf(text);

            if(val >= 0 && val <= 1)
                return true;
        }
        catch (NumberFormatException exp)
        {

        }

        return false;
    }

    private boolean isResultLimitTextBoxValid()
    {
        try
        {
            String text = resultLimitTextBox.getValue().trim();

            if(text.isEmpty())
                return true;

            double val = Integer.valueOf(text);

            if(val > 0)
                return true;
        }
        catch (NumberFormatException exp)
        {

        }

        return false;
    }

    private String generateQuery()
    {
        Methods method = Methods.valueOf(queryMethodListBox.getSelectedValue());
        boolean searchCompounds = searchCompoundsCheckBox.getValue();
        boolean searchBioassays = searchBioassaysCheckBox.getValue();

        StringBuilder query = new StringBuilder();

        query.append("SELECT DISTINCT ");


        if(compoundCheckBox.getValue() && searchCompounds)
            query.append("?COMPOUND ");

        if(scoreCheckBox.getValue() && searchCompounds && method == Methods.SIMSEARCH)
            query.append("?SCORE ");

        if(bioassayCheckBox.getValue() && searchBioassays)
            query.append("?BIOASSAY ");

        if(statusCheckBox.getValue() && searchCompounds && searchBioassays)
            query.append("?STATUS ");


        query.append("WHERE \n{");


        String mol = queryTextArea.getValue();

        if(searchCompounds && !mol.trim().isEmpty())
        {
            if(method == Methods.SIMSEARCH)
                query.append("\n  [ orchem:compound ?COMPOUND; orchem:score ?SCORE ]\n");
            else
                query.append("\n  ?COMPOUND ");

            if(method == Methods.EXACTSEARCH || method == Methods.SUBSEARCH)
                query.append("orchem:substructureSearch");
            else if(method == Methods.SIMSEARCH)
                query.append("    orchem:similaritySearch");
            else /*if(method == Methods.SMARTSEARCH)*/
                query.append("orchem:smartsSearch");

            query.append(" [\n      orchem:query '''");
            query.append(queryTextArea.getValue());
            query.append("'''");

            if(method != Methods.SMARTSEARCH)
            {
                query.append(";\n      orchem:queryType \"");
                query.append(queryTypeListBox.getSelectedValue());
                query.append("\"");
            }


            String limit = resultLimitTextBox.getValue().trim();

            if(!limit.isEmpty())
            {
                query.append(";\n      orchem:topn ");
                query.append(limit);
            }

            if(method == Methods.EXACTSEARCH)
            {
                query.append(";\n      orchem:exact true");
            }

            if(method == Methods.SUBSEARCH || method == Methods.EXACTSEARCH)
            {
                query.append(";\n      orchem:tautomers ");
                query.append(useTautomersListBox.getSelectedValue());
            }

            String threshold = resultThresholdTextBox.getValue().trim();

            if(method == Methods.SIMSEARCH && !threshold.isEmpty())
            {
                query.append(";\n      orchem:cutoff ");
                query.append(threshold);
            }

            query.append(" ].\n");
        }

        if(searchCompounds && searchBioassays)
        {
            query.append("\n  ?SUBSTANCE sio:CHEMINF_000477 ?COMPOUND.");
            query.append("\n  ?ENDPOINT obo:IAO_0000136 ?SUBSTANCE.");
            query.append("\n  ?MEASUREGROUP obo:OBI_0000299 ?ENDPOINT.");
            query.append("\n  ?BIOASSAY bao:BAO_0000209 ?MEASUREGROUP.");


            int statusCount = 0;

            if(activeStatusCheckBox.getValue())
                statusCount++;

            if(inactiveStatusCheckBox.getValue())
                statusCount++;

            if(inconclusiveStatusCheckBox.getValue())
                statusCount++;

            if(probeStatusCheckBox.getValue())
                statusCount++;

            if(unspecifiedStatusCheckBox.getValue())
                statusCount++;


            if(statusCount == 1 && !statusCheckBox.getValue())
            {
                query.append("\n  ?ENDPOINT vocab:PubChemAssayOutcome ");

                if(activeStatusCheckBox.getValue())
                    query.append("vocab:active");

                if(inactiveStatusCheckBox.getValue())
                    query.append("vocab:inactive");

                if(inconclusiveStatusCheckBox.getValue())
                    query.append("vocab:inconclusive");

                if(probeStatusCheckBox.getValue())
                    query.append("vocab:probe");

                if(unspecifiedStatusCheckBox.getValue())
                    query.append("vocab:unspecified");

                query.append(".");
            }
            else if(statusCount > 0 || statusCheckBox.getValue())
            {
                query.append("\n  ?ENDPOINT vocab:PubChemAssayOutcome ?STATUS");

                if(statusCount > 0)
                {
                    query.append("\n  FILTER(?STATUS IN (");

                    if(activeStatusCheckBox.getValue())
                    {
                        query.append("vocab:active");

                        if(--statusCount > 0)
                            query.append(" ,");
                    }

                    if(inactiveStatusCheckBox.getValue())
                    {
                        query.append("vocab:inactive");

                        if(--statusCount > 0)
                            query.append(" ,");
                    }

                    if(inconclusiveStatusCheckBox.getValue())
                    {
                        query.append("vocab:inconclusive");

                        if(--statusCount > 0)
                            query.append(" ,");
                    }

                    if(probeStatusCheckBox.getValue())
                    {
                        query.append("vocab:probe");

                        if(--statusCount > 0)
                            query.append(", ");
                    }

                    if(unspecifiedStatusCheckBox.getValue())
                    {
                        query.append("vocab:unspecified");
                    }

                    query.append("))");
                }

                query.append("\n");
            }
        }

        String keywords = keywordsTextArea.getValue().trim();

        if(searchBioassays && !keywords.isEmpty())
        {
            query.append("\n\n  ?BIOASSAY fulltext:bioassaySearch [fulltext:query '''");
            query.append(keywords.replaceAll("'", "\\\\'"));
            query.append("'''].\n");
        }

        query.append("}\n");


        switch(orderByListBox.getSelectedValue())
        {
            case "COMPOUND":
                query.append("ORDER BY xsd:integer(substr(str(str(?COMPOUND)), 49))\n");
                break;

            case "BIOASSAY":
                query.append("ORDER BY xsd:integer(substr(str(str(?BIOASSAY)), 49))\n");
                break;

            case "SCORE":
                query.append("ORDER BY ?SCORE\n");
                break;

            case "STATUS":
                query.append("ORDER BY ?STATUS\n");
                break;
        }


        return query.toString();
    }




    public SearchQueryWizardDialog()
    {
        uiBinder.createAndBindUi(this);

        queryMethodListBox.addItem("Substructure search", "SUBSEARCH");
        queryMethodListBox.addItem("Exact search", "EXACTSEARCH");
        queryMethodListBox.addItem("SMARTS search", "SMARTSEARCH");
        queryMethodListBox.addItem("Similarity search", "SIMSEARCH");

        queryTypeListBox.addItem("MOL/SDF file", "MOL");
        queryTypeListBox.addItem("SMILES", "SMILES");
        useTautomersListBox.addItem("yes", "true");
        useTautomersListBox.addItem("no", "false");

        orderByListBox.addItem("(none)", "NONE");
        orderByListBox.addItem("Compound ID", "COMPOUND");
        orderByListBox.addItem("Bioassay ID", "BIOASSAY");
        orderByListBox.addItem("Score", "SCORE");
        orderByListBox.addItem("Status", "STATUS");

        queryTextArea.setValue(
                "\n  Marvin  12270700542D          \n  \n 13 13  0  0  0  0            999 V2000\n   -0.7145   -0.4125    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    0.0000   -0.8250    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -0.7145    0.4125    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    0.7145    0.4125    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    0.0000    0.8250    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    0.7145   -0.4125    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    1.4289    0.8250    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    2.1433    0.4126    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n    1.4289    1.6501    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   -0.0002    1.6500    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   -0.7145    2.0625    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -1.4290    1.6500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -0.7145    2.8875    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n  2  1  1  0  0  0  0\n  3  1  2  0  0  0  0\n  4  7  1  0  0  0  0\n  5  3  1  0  0  0  0\n  4  5  2  0  0  0  0\n 10  5  1  0  0  0  0\n  6  4  1  0  0  0  0\n  2  6  2  0  0  0  0\n  8  7  1  0  0  0  0\n  9  7  2  0  0  0  0\n 11 10  1  0  0  0  0\n 12 11  1  0  0  0  0\n 13 11  2  0  0  0  0\nM  END\n");


        queryFileUpload.addChangeHandler(new ChangeHandler()
        {
            @Override
            public void onChange(ChangeEvent event)
            {
                handleFileSelect(queryFileUpload.getElement());
            }
        });
    }

    private native void handleFileSelect(@SuppressWarnings("deprecation") com.google.gwt.user.client.Element evt)
    /*-{
        var file = evt.files[0];

        //if (!file.type.match('text.*'))
        //  return;

        if (!$wnd.File || !$wnd.FileReader || !$wnd.FileList || !$wnd.Blob)
            $wnd
                    .alert("The File APIs are not fully supported by your browser.");

        var reader = new $wnd.FileReader();
        var that = this;

        reader.onload = (function(theFile) {
            return function(e) {
                that.@cz.iocb.chemweb.client.widgets.dialog.SearchQueryWizardDialog::handleLoadedFile(Ljava/lang/String;)(e.target.result);
            };
        })(file);

        reader.readAsText(file);
    }-*/;

    public void handleLoadedFile(String data)
    {
        queryTextArea.setValue(data);
    }


    @UiFactory
    DialogBox instance()
    {
        return this;
    }
}
