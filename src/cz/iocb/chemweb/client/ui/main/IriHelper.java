package cz.iocb.chemweb.client.ui.main;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.regexp.shared.RegExp;



public class IriHelper
{
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


    static private final RegExp URLreqexp = RegExp
            .compile("^((ftp|http|https)://[\\w@.\\-\\_]+(:\\d{1,5})?(/[\\w#!:.?+=&%@!\\_\\-/]+)*){1}$");


    static public boolean isValid(String value)
    {
        return URLreqexp.exec(value) != null;
    }


    static public String prefixedIRI(String url)
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


    static public String deprefixedIRI(String url)
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


    static public final native JsArray<JsPrefixDefinition> getPrefixes()
    /*-{
        return $wnd.prefixes;
    }-*/;
}
