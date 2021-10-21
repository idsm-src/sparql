package cz.iocb.chemweb.server.sparql.translator;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import java.util.LinkedList;
import java.util.List;
import com.google.common.collect.Lists;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.triple.AlternativePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BracketedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.InversePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.NegatedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Path;
import cz.iocb.chemweb.server.sparql.parser.model.triple.RepeatedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.RepeatedPath.Kind;
import cz.iocb.chemweb.server.sparql.parser.model.triple.SequencePath;



public class PathRewriteVisitor extends ElementVisitor<Path>
{
    @Override
    public Path visit(AlternativePath path)
    {
        List<Path> alternatives = new LinkedList<Path>();

        for(Path child : path.getChildren())
        {
            Path rewrited = visitElement(child);

            if(rewrited instanceof AlternativePath)
                alternatives.addAll(((AlternativePath) rewrited).getChildren());
            else
                alternatives.add(rewrited);
        }

        return new AlternativePath(alternatives);
    }


    @Override
    public Path visit(SequencePath path)
    {
        List<List<Path>> sequences = new LinkedList<List<Path>>();
        sequences.add(new LinkedList<Path>());

        for(Path child : path.getChildren())
        {
            Path rewrited = visitElement(child);

            if(rewrited instanceof AlternativePath)
            {
                List<List<Path>> tmp = new LinkedList<List<Path>>();

                for(List<Path> s1 : sequences)
                {
                    for(Path s2 : ((AlternativePath) rewrited).getChildren())
                    {
                        List<Path> merged = new LinkedList<Path>(s1);
                        tmp.add(merged);

                        if(s2 instanceof SequencePath)
                            merged.addAll(((SequencePath) s2).getChildren());
                        else
                            merged.add(s2);
                    }
                }

                sequences = tmp;
            }
            else if(rewrited instanceof SequencePath)
            {
                sequences.stream().forEach(s -> s.addAll(((SequencePath) rewrited).getChildren()));
            }
            else
            {
                sequences.stream().forEach(s -> s.add(rewrited));
            }
        }

        if(sequences.size() == 1)
            return new SequencePath(sequences.get(0));

        return new AlternativePath(sequences.stream().map(p -> new SequencePath(p)).collect(toList()));
    }


    @Override
    public Path visit(InversePath path)
    {
        Path child = path.getChild();

        if(child instanceof AlternativePath)
            return visitElement(new AlternativePath(
                    ((AlternativePath) child).getChildren().stream().map(p -> new InversePath(p)).collect(toList())));
        else if(child instanceof SequencePath)
            return visitElement(new SequencePath(Lists.reverse(
                    ((SequencePath) child).getChildren().stream().map(p -> new InversePath(p)).collect(toList()))));
        else if(child instanceof InversePath)
            return visitElement(child);
        else if(child instanceof RepeatedPath)
            return visitElement(new RepeatedPath(((RepeatedPath) child).getKind(),
                    new InversePath(((RepeatedPath) child).getChild())));
        else if(child instanceof NegatedPath)
            return visitElement(new NegatedPath(new InversePath(((NegatedPath) child).getChild())));
        else if(child instanceof BracketedPath)
            return visitElement(new InversePath(((BracketedPath) child).getChild()));
        else if(child instanceof IRI)
            return path;

        return null;
    }


    @Override
    public Path visit(RepeatedPath path)
    {
        Path child = visitElement(path.getChild());

        RepeatedPath rewrite = null;

        if(!(child instanceof RepeatedPath))
            rewrite = new RepeatedPath(path.getKind(), child);
        else if(((RepeatedPath) child).getKind() == path.getKind())
            rewrite = new RepeatedPath(path.getKind(), ((RepeatedPath) child).getChild());
        else
            rewrite = new RepeatedPath(Kind.ZeroOrMore, ((RepeatedPath) child).getChild());

        if(rewrite.getKind() == Kind.OneOrMore)
            return rewrite;

        return new AlternativePath(asList(rewrite, new SequencePath(new LinkedList<Path>())));
    }


    @Override
    public Path visit(NegatedPath path)
    {
        return path;
    }


    @Override
    public Path visit(BracketedPath path)
    {
        return visitElement(path.getChild());
    }


    @Override
    public Path visit(IRI path)
    {
        return path;
    }
}
