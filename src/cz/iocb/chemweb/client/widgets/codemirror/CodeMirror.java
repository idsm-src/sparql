package cz.iocb.chemweb.client.widgets.codemirror;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.TextArea;
import cz.iocb.chemweb.shared.services.check.CheckService;
import cz.iocb.chemweb.shared.services.check.CheckServiceAsync;



public class CodeMirror
{
    static public abstract class Validator
    {
        public abstract void validate(String code, JavaScriptObject callbackFunction, JavaScriptObject options,
                JavaScriptObject cm);
    }


    private static CheckServiceAsync checkService = (CheckServiceAsync) GWT.create(CheckService.class);

    private JavaScriptObject codemirror = null;


    public CodeMirror(TextArea textArea, boolean readOnly)
    {
        codemirror = init(textArea.getElement(), readOnly);
    }


    public CodeMirror(TextArea textArea)
    {
        this(textArea, false);
    }


    public native void setValidator(Validator validator)
    /*-{
        var editor = this.@cz.iocb.chemweb.client.widgets.codemirror.CodeMirror::codemirror;
        var lint = editor.getOption("lint");

        if (lint != null)
            lint.javaValidator = validator;
    }-*/;


    public native Validator getValidator()
    /*-{
        var editor = this.@cz.iocb.chemweb.client.widgets.codemirror.CodeMirror::codemirror;
        return editor.javaValidator;
    }-*/;


    public static native Validator getValidator(JavaScriptObject cm)
    /*-{
        var lint = cm.getOption("lint");
        return lint == null ? null : lint.javaValidator;
    }-*/;


    public native static JavaScriptObject createWarning(int beginLine, int beginColumn, int endLine, int endColumn,
            String type, String warn)
    /*-{
        var error = {
            from : $wnd.CodeMirror.Pos(beginLine, beginColumn),
            to : $wnd.CodeMirror.Pos(endLine, endColumn),
            message : warn,
            severity : type
        };

        return error;
    }-*/;


    public native static void callback(JavaScriptObject callbackFunction, JavaScriptObject result, JavaScriptObject cm)
    /*-{
        callbackFunction(cm, result);
    }-*/;


    protected static void validateCode(final String code, final JavaScriptObject callbackFunction,
            final JavaScriptObject options, final JavaScriptObject cm)
    {
        Validator validator = getValidator(cm);

        if(validator != null)
            validator.validate(code, callbackFunction, options, cm);
    };


    public native static JavaScriptObject init(Element e, boolean readOnly)
    /*-{
        var getHints = function (cm, cb, options)
        {
            var cur = cm.getCursor();
            var token = cm.getTokenAt(cur);

            if(token.type == "string")
                return {};

            var result = [];
            for(var i in $wnd.hints)
            {
                var hint = $wnd.hints[i];

                if( token.string == hint.text.substring(0,token.string.length) )
                    result.push({text: hint.text, info: hint.info, type: hint.type,
                    render : function(element, self, data)
                    {
                        var div = $wnd.document.createElement("div");
                        var img = $wnd.document.createElement("img");
                        var span = $wnd.document.createElement("span");

                        span.style = "display: inline-block; height: 100%; vertical-align: middle;width:10px;";
                        img.style = "vertical-align: middle;";


                        if(data.type == "class")
                            img.src = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAYAAAAfSC3RAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAN1wAADdcBQiibeAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAHxSURBVCiRXdI/SNRxGMfx1+/up156F0LQJf4hEKIphHAJh4s0sIgIa6gxwkFXHaIxocXdzVnSIJeCLBBxaIlAQ4oQxIyywK40/5w/79twZ2EPPNP3+fDwfT/vKITgsKLrUQ73RXoEbSpPqw7MiD0K02Hz7+xhMLoSXZYx7ry8U2IRfmMDXyQ+WRfcDc/Ci7/BqDe65IQptzTKULdbp6uhS2Y/Y+7jnM2NTYpYVpS4GZ6HVxRk3bDmiWBeuLZwLXzY+hAOa+XnSuh73BcMC/oFvdYUZFNyHuiQV0tTucnYmTEpKcNvhw3OD2qoaTBycUR9TT1p1Mur9SCW0i0vtsPt07c11zV7uPzQ6MIo23zf+C7ZSSRJQhm1YindsbI2KiBa0i1g8dsiu9hj8t0k2yjhoIq0rC0lVOltsbW9BVprWivDu3Tlu7Tn2tlHUm2kBKt+VIJvVt8oh7JCviCX5GQPssaujpm5N6Mx3VjZWqrcNha89FWHvHj67bSJ9gl3zt2x1L8kHaU1HW8yNDmk+KPIHvYkeBkpyKr33mnN6oiiyMCFAZ2tnX5t/zK7NOvp66fKO2V2sO+zkrP/BEibclKjGKEK4kDlb3vVLilKVwQ4qlww7pi8GjFVEPsoSQTr0v8pd0TyXffRQ/VMrGJG5qjkfwBUAd94y1T5PQAAAABJRU5ErkJggg=="                        
                        else
                            img.src = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAYAAAAfSC3RAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAN1wAADdcBQiibeAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAHFSURBVCiRXdI/SNRhHMfx1+/uZ1gmKYIXFhJE0GARBo4aZEIN6pCbU7OLi1Bbg7U5OTm4ujjYUlAq1uJSBlIQVOYf+nMNaZSnd573NPicHH3hM32eN8/3zycJIajWQJI04n7CrUB7dDYPeZHy+EkIf6pvkyp4J0n66pm+Tu4saYJd/MJ3ylvkA/eehvD8GLydJDdbmB2iqR6FCP2t0Q4+s1Pm7rMQFpIbnG7mwzDncmgaGtI0OKiM4sGBYrHo7cyMd0tLfmOLr3tczl7hYSd958lAS3+/C6OjJIkTzc3aenpcHR62sbzs5/q6EqcOSTMZenOke7GlUhz+/cSE2a4uL8fG1DU0uNTXp4ITpBl60wrt4kxpDXims9PFSkVbdzfY3d52GL0K7WmI0AGyKEazY2REB0IIPi0uejU1pYxy9NPA5jatDRGs/vh6ctLHhQX5tTVfVlcVohf9zTQw/4NruaPej8GNlRVv5uaUsB87iSpjPlNg/Bv57epysllQymQU4k33ovaP5suXGD8OQJbZVppSBBxGHdT8VmInWw1AbeQC0yfJ1R0tWDmCJcqBfPb/yFVrIEka93mAXvFM2MR8PY9qQ/4PScbIYlDpMJQAAAAASUVORK5CYII="


                        div.style="height: 20px;"

                        div.appendChild(img);
                        div.appendChild(span);

                        div.appendChild($wnd.document.createTextNode(data.text));

                        if(data.info)
                        {
                            var info = $wnd.document.createElement("span");
                            info.className = "hint-info";
                            info.appendChild($wnd.document.createTextNode(" ("));
                            info.appendChild($wnd.document.createTextNode(data.info));
                            info.appendChild($wnd.document.createTextNode(")"));
                            div.appendChild(info);
                        }                       

                        element.appendChild(div);
                    }

                    });
            }


            return {

            list: result,
               from: $wnd.CodeMirror.Pos(cur.line, token.start),
               to: $wnd.CodeMirror.Pos(cur.line, token.end)
          }
        };


    	var editor = $wnd.CodeMirror.fromTextArea(e, {
    		mode : "sparql",
    		tabMode : "indent",
    		lineNumbers : true,
    		iframeClass : 'CodeMirror',
    		matchBrackets : true,
    		lineWrapping : true,
    		autoCloseBrackets : true,
    		readOnly: readOnly,
    		
    		gutters : [ "CodeMirror-lint-markers" ],
    		lint : readOnly ? null : {
    			getAnnotations : @cz.iocb.chemweb.client.widgets.codemirror.CodeMirror::validateCode(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;),
    			async : true,
    			javaValidator: null
    		},
    		
    		hintOptions : {
                hint : getHints,
            },
    		
            extraKeys: {"Ctrl-Space": "autocomplete",
                "':'": function(cm) {
                setTimeout(function(){cm.execCommand("autocomplete");}, 100);
                throw CodeMirror.Pass; // tell CodeMirror we didn't handle the key
                },
            },

            theme : "chemweb"
    	});

    	return editor;
    }-*/;


    public native String getValue()
    /*-{
        var editor = this.@cz.iocb.chemweb.client.widgets.codemirror.CodeMirror::codemirror;
        return editor.getValue();
    }-*/;


    public native String setValue(String val)
    /*-{
        var editor = this.@cz.iocb.chemweb.client.widgets.codemirror.CodeMirror::codemirror;
        return editor.setValue(val);
    }-*/;


    public native String refresh()
    /*-{
        var editor = this.@cz.iocb.chemweb.client.widgets.codemirror.CodeMirror::codemirror;
        return editor.refresh();
    }-*/;
}
