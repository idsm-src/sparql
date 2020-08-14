package cz.iocb.chemweb.server.velocity;

import java.util.HashMap;
import org.apache.commons.lang3.StringEscapeUtils;
import cz.iocb.chemweb.server.sparql.engine.LiteralNode;
import cz.iocb.chemweb.server.sparql.engine.ReferenceNode;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.shared.utils.Encode;



public class NodeUtils
{
    private HashMap<String, String> prefixes;


    public NodeUtils(HashMap<String, String> prefixes)
    {
        this.prefixes = prefixes;
    }


    public String escapeHtml(LiteralNode node)
    {
        if(node == null)
            return null;

        return StringEscapeUtils.escapeHtml4(node.getValue()).replaceAll("\uFFFD",
                "<span style=\"color: #a0a0a0\">\uFFFD</span>");
    }


    public String prefixedIRI(ReferenceNode node)
    {
        String iri = IRI.toPrefixedIRI(node.getValue(), prefixes);
        return iri != null ? iri : node.getValue();
    }


    public String nodeId(ReferenceNode node)
    {
        return "NODE_" + Encode.base32m(node.getValue());
    }
}
