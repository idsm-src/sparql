package cz.iocb.chemweb.client;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import cz.iocb.chemweb.shared.services.query.DataGridNode;
import cz.iocb.chemweb.shared.utils.Encode;



public class LinkCell extends AbstractCell<DataGridNode>
{
    public LinkCell()
    {
        super("click", "keypress");
    }

    @Override
    public void render(Context context, DataGridNode value, SafeHtmlBuilder sb)
    {
        /*
         * Always do a null check on the value. Cell widgets can pass null to
         * cells if the underlying data contains a null, or if the data arrives
         * out of order.
         */
        if(value == null)
        {
            return;
        }

        // If the value comes from the user, we escape it to avoid XSS attacks.
        //SafeHtml safeValue = SafeHtmlUtils.fromString(value.name.substring(1, 4));

        // Use the template to create the Cell's html.
        //SafeStyles styles = SafeStylesUtils.fromTrustedString("color: red;");

        String htmlCode = value.html;

        if(value.ref != null)
            htmlCode = "<div class=\"BOX-selection BOX-NODE_" + Encode.base32m(value.ref) + "\">" + htmlCode + "</div>";

        sb.append(SafeHtmlUtils.fromTrustedString(htmlCode));
    }
}
