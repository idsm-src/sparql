package cz.iocb.sparql.engine.translator;

import static java.util.stream.Collectors.toCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.triple.AlternativePath;
import cz.iocb.sparql.engine.parser.model.triple.BracketedPath;
import cz.iocb.sparql.engine.parser.model.triple.InversePath;
import cz.iocb.sparql.engine.parser.model.triple.NegatedPath;
import cz.iocb.sparql.engine.parser.model.triple.Path;
import cz.iocb.sparql.engine.parser.model.triple.RepeatedPath;
import cz.iocb.sparql.engine.parser.model.triple.RepeatedPath.Kind;
import cz.iocb.sparql.engine.parser.model.triple.SequencePath;



public class PathRewriteVisitor extends ElementVisitor<Path>
{
    @Override
    public Path visit(AlternativePath path)
    {
        List<Path> alternatives = new LinkedList<Path>();

        for(Path child : path.getChildren())
        {
            Path rewrited = visitElement(child);

            if(rewrited instanceof AlternativePath alternativePath)
                alternatives.addAll(alternativePath.getChildren());
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

            if(rewrited instanceof AlternativePath alternativePath)
            {
                List<List<Path>> tmp = new LinkedList<List<Path>>();

                for(List<Path> s1 : sequences)
                {
                    for(Path s2 : alternativePath.getChildren())
                    {
                        List<Path> merged = new LinkedList<Path>(s1);
                        tmp.add(merged);

                        if(s2 instanceof SequencePath sequencePath)
                            merged.addAll(sequencePath.getChildren());
                        else
                            merged.add(s2);
                    }
                }

                sequences = tmp;
            }
            else if(rewrited instanceof SequencePath sequencePath)
            {
                sequences.stream().forEach(s -> s.addAll(sequencePath.getChildren()));
            }
            else
            {
                sequences.stream().forEach(s -> s.add(rewrited));
            }
        }

        if(sequences.size() == 1)
            return new SequencePath(sequences.get(0));

        return new AlternativePath(sequences.stream().map(p -> (Path) new SequencePath(p)).toList());
    }


    @Override
    public Path visit(InversePath path)
    {
        switch(path.getChild())
        {
            case AlternativePath child:
                return visitElement(
                        new AlternativePath(child.getChildren().stream().map(p -> (Path) new InversePath(p)).toList()));

            case SequencePath child:
                List<Path> rev = child.getChildren().stream().map(p -> (Path) new InversePath(p))
                        .collect(toCollection(ArrayList::new));
                Collections.reverse(rev);
                return visitElement(new SequencePath(rev));

            case InversePath child:
                return visitElement(child);

            case RepeatedPath child:
                return visitElement(new RepeatedPath(child.getKind(), new InversePath(child.getChild())));

            case NegatedPath child:
                return visitElement(new NegatedPath(new InversePath(child.getChild())));

            case BracketedPath child:
                return visitElement(new InversePath(child.getChild()));

            case IRI _:
                return path;

            default:
                return null;
        }
    }


    @Override
    public Path visit(RepeatedPath path)
    {
        Path child = visitElement(path.getChild());

        RepeatedPath rewrite = null;

        if(!(child instanceof RepeatedPath repeatedPath))
            rewrite = new RepeatedPath(path.getKind(), child);
        else if(repeatedPath.getKind() == path.getKind())
            rewrite = new RepeatedPath(path.getKind(), repeatedPath.getChild());
        else
            rewrite = new RepeatedPath(Kind.ZeroOrMore, repeatedPath.getChild());

        return rewrite;
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
