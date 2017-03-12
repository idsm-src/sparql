package cz.iocb.chemweb.client.widgets.codemirror;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;



public class RetinaCodeMirrorBundle extends CodeMirrorBundle
{
    public interface RetinaCodeMirrorClientBundle extends CodeMirrorClientBundle
    {
        @Override
        @Source("class@2x.png")
        ImageResource classSymbol();

        @Override
        @Source("property@2x.png")
        ImageResource propertySymbol();
    }


    @Override
    public CodeMirrorClientBundle create()
    {
        return GWT.create(RetinaCodeMirrorClientBundle.class);
    }
}
