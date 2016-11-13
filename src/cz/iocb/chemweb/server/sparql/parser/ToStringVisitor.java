package cz.iocb.chemweb.server.sparql.parser;

import java.math.BigDecimal;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.Define;
import cz.iocb.chemweb.server.sparql.parser.model.GroupCondition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.OrderCondition;
import cz.iocb.chemweb.server.sparql.parser.model.PrefixDefinition;
import cz.iocb.chemweb.server.sparql.parser.model.PrefixedName;
import cz.iocb.chemweb.server.sparql.parser.model.Projection;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
import cz.iocb.chemweb.server.sparql.parser.model.Select;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BracketedExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BuiltInCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.ExistsExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.FunctionCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.InExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.expression.UnaryExpression;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Bind;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Filter;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Graph;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.GroupGraph;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Minus;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.MultiProcedureCall;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Optional;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCall;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCallBase;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Service;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Union;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Values;
import cz.iocb.chemweb.server.sparql.parser.model.triple.AlternativePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BracketedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.InversePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.NegatedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.RepeatedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.SequencePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Triple;



/**
 * Converts AST back to a SPARQL query string.
 */
public class ToStringVisitor extends ElementVisitor<Void>
{
    private boolean compact;
    private Prologue prologue;
    private final StringBuilder stringBuilder = new StringBuilder();
    private int indentation = 0;
    private final int indentationMultiplier = 4;
    boolean shouldIndent = false;
    boolean topLevel = true;
    protected boolean noIriSimplification = true;
    protected boolean ignorePredefinedPrefixes = true;

    public ToStringVisitor(Prologue prologue)
    {
        this.prologue = prologue;
        noIriSimplification = false;
        ignorePredefinedPrefixes = false;
    }

    public ToStringVisitor()
    {
    }

    protected static <T> void separatedForEach(Iterable<T> source, Consumer<T> itemConsumer, Runnable separatorAction)
    {
        boolean first = true;

        for(T item : source)
        {
            if(first)
                first = false;
            else
                separatorAction.run();

            itemConsumer.accept(item);
        }
    }

    /**
     * Returns the computed string.
     */
    public String getString()
    {
        return stringBuilder.toString();
    }

    /**
     * Resets the internal string, so that the same visitor can be used again.
     */
    public void resetString()
    {
        stringBuilder.setLength(0);
    }

    private void newLine()
    {
        int lastCharIndex = stringBuilder.length() - 1;
        if(stringBuilder.charAt(lastCharIndex) == ' ')
            stringBuilder.deleteCharAt(lastCharIndex);

        if(compact)
        {
            word();
        }
        else
        {
            checkIndentation();

            shouldIndent = true;
        }
    }

    private void checkIndentation()
    {
        if(shouldIndent)
        {
            stringBuilder.append("\n");

            for(int i = 0; i < indentation * indentationMultiplier; i++)
            {
                stringBuilder.append(' ');
            }

            shouldIndent = false;
        }
    }

    protected void write(String s)
    {
        checkIndentation();
        stringBuilder.append(s);
    }

    protected void word(String word)
    {
        checkIndentation();
        stringBuilder.append(word);
        word();
    }

    protected void word()
    {
        stringBuilder.append(' ');
    }

    @Override
    public Void visit(Prologue prologue)
    {
        this.prologue = prologue;

        visitElements(prologue.getDefines());

        if(!prologue.getBase().getUri().toString().equals(""))
        {
            word("BASE");
            word(prologue.getBase().toString(null, false, true));
            newLine();
        }

        visitElements(prologue.getPrefixes());

        if(stringBuilder.length() > 0)
            newLine();

        return null;
    }

    @Override
    public Void visit(Define define)
    {
        word("DEFINE");
        visit(define.getKey());
        word();
        boolean prev = noIriSimplification;
        visitElements(define.getValues());
        noIriSimplification = prev;
        newLine();

        return null;
    }

    @Override
    public Void visit(Define.StringValue stringValue)
    {
        write(stringValue.getValue());

        return null;
    }

    @Override
    public Void visit(Define.IntegerValue integerValue)
    {
        write(integerValue.getValue().toString());

        return null;
    }

    @Override
    public Void visit(PrefixDefinition prefix)
    {
        word("PREFIX");
        word(prefix.getName() + ':');
        word(prefix.getIri().toString(null, false, true));
        newLine();

        return null;
    }

    private void nested(Runnable action)
    {
        write("{");
        newLine();
        indentation++;

        topLevel = false;

        action.run();

        indentation--;
        write("}");
        newLine();
    }

    private void visitSelect(Select select)
    {
        word("SELECT");

        if(select.isDistinct())
            word("DISTINCT");
        else if(select.isReduced())
            word("REDUCED");

        if(select.getProjections().isEmpty())
        {
            word("*");
        }
        else
        {
            separatedForEach(select.getProjections(), this::visit, this::word);
        }

        newLine();

        visitElements(select.getDataSets());

        word("WHERE");
        newLine();

        visitElement(select.getPattern());

        if(!select.getGroupByConditions().isEmpty())
        {
            word("GROUP BY");
            separatedForEach(select.getGroupByConditions(), this::visit, this::word);
            newLine();
        }

        if(!select.getHavingConditions().isEmpty())
        {
            word("HAVING");
            separatedForEach(select.getHavingConditions(), this::visitElement, this::word);
            newLine();
        }

        if(!select.getOrderByConditions().isEmpty())
        {
            word("ORDER BY");
            separatedForEach(select.getOrderByConditions(), this::visit, this::word);
            newLine();
        }

        if(select.getLimit() != null)
        {
            word("LIMIT");
            write(select.getLimit().toString());
            newLine();
        }

        if(select.getOffset() != null)
        {
            word("OFFSET");
            write(select.getOffset().toString());
            newLine();
        }

        visitElement(select.getValues());
    }

    @Override
    public Void visit(Select select)
    {
        if(topLevel)
        {
            topLevel = false;
            visitSelect(select);
        }
        else
        {
            nested(() -> visitSelect(select));
        }

        return null;
    }

    @Override
    public Void visit(Projection projection)
    {
        if(projection.getExpression() == null)
        {
            visit(projection.getVariable());
            return null;
        }

        write("(");
        visitElement(projection.getExpression());
        word();
        word("AS");
        visit(projection.getVariable());
        write(")");

        return null;
    }

    @Override
    public Void visit(DataSet dataSet)
    {
        word("FROM");

        if(!dataSet.isDefault())
            word("NAMED");

        visitElement(dataSet.getSourceSelector());

        newLine();

        return null;
    }

    @Override
    public Void visit(GroupCondition groupCondition)
    {
        write("(");
        visitElement(groupCondition.getExpression());

        if(groupCondition.getVariable() != null)
        {
            word();
            word("AS");
            visit(groupCondition.getVariable());
        }
        write(")");

        return null;
    }

    @Override
    public Void visit(OrderCondition orderCondition)
    {
        if(orderCondition.getDirection() == OrderCondition.Direction.Unspecified
                && orderCondition.getExpression() instanceof Variable)
        {
            visitElement(orderCondition.getExpression());
        }
        else
        {
            if(orderCondition.getDirection() != OrderCondition.Direction.Unspecified)
                write(orderCondition.getDirection().getText());

            write("(");
            visitElement(orderCondition.getExpression());
            write(")");
        }

        return null;
    }

    @Override
    public Void visit(GroupGraph groupGraph)
    {
        nested(() -> visitElements(groupGraph.getPatterns()));

        return null;
    }

    @Override
    public Void visit(Union union)
    {
        separatedForEach(union.getPatterns(), this::visitElement, () -> {
            write("UNION");
            newLine();
        });

        return null;
    }

    @Override
    public Void visit(Optional optional)
    {
        write("OPTIONAL");
        newLine();

        visitElement(optional.getPattern());

        return null;
    }

    @Override
    public Void visit(Minus minus)
    {
        write("MINUS");
        newLine();

        topLevel = false;

        visitElement(minus.getPattern());

        return null;
    }

    @Override
    public Void visit(Filter filter)
    {
        word("FILTER");

        visitElement(filter.getConstraint());

        newLine();

        return null;
    }

    @Override
    public Void visit(Bind bind)
    {
        word("BIND");

        write("(");

        visitElement(bind.getExpression());
        word();

        word("AS");

        visit(bind.getVariable());

        write(")");
        newLine();

        return null;
    }

    @Override
    public Void visit(Graph graph)
    {
        word("GRAPH");

        visitElement(graph.getName());
        newLine();

        visitElement(graph.getPattern());

        return null;
    }

    @Override
    public Void visit(Service service)
    {
        word("SERVICE");

        if(service.isSilent())
            word("SILENT");

        visitElement(service.getName());
        newLine();

        visitElement(service.getPattern());

        return null;
    }

    @Override
    public Void visit(Values values)
    {
        word("VALUES");
        write("(");
        separatedForEach(values.getVariables(), this::visit, this::word);
        write(")");
        newLine();

        nested(() -> {
            values.getValuesLists().forEach(valuesList -> {
                visit(valuesList);
                newLine();
            });
        });

        return null;
    }

    @Override
    public Void visit(Values.ValuesList valuesList)
    {
        write("(");

        separatedForEach(valuesList.getValues(), literal -> {
            if(literal == null)
                write("UNDEF");
            else
                visitElement(literal);
        }, this::word);

        write(")");

        return null;
    }

    @Override
    public Void visit(BinaryExpression binaryExpression)
    {
        visitElement(binaryExpression.getLeft());
        write(" ");
        write(binaryExpression.getOperator().getText());
        write(" ");
        visitElement(binaryExpression.getRight());

        return null;
    }

    @Override
    public Void visit(InExpression inExpression)
    {
        visitElement(inExpression.getLeft());
        word();

        if(inExpression.isNegated())
            word("NOT");

        word("IN");

        write("(");
        separatedForEach(inExpression.getRight(), this::visitElement, () -> word(","));
        write(")");

        return null;
    }

    @Override
    public Void visit(UnaryExpression unaryExpression)
    {
        write(unaryExpression.getOperator().getText());
        visitElement(unaryExpression.getOperand());

        return null;
    }

    @Override
    public Void visit(BracketedExpression bracketedExpression)
    {
        write("(");
        visitElement(bracketedExpression.getChild());
        write(")");

        return null;
    }

    @Override
    public Void visit(BuiltInCallExpression builtInCallExpression)
    {
        String functionName = builtInCallExpression.getFunctionName().toUpperCase(Locale.US);
        List<Expression> arguments = builtInCallExpression.getArguments();

        write(functionName);

        write("(");

        if(builtInCallExpression.isDistinct())
            word("DISTINCT");

        if(functionName.equals("COUNT") && arguments.isEmpty())
        {
            write("*");
        }
        else if(functionName.equals("GROUP_CONCAT") && arguments.size() == 2)
        {
            visitElement(arguments.get(0));
            word(";");

            word("SEPARATOR =");

            visitElement(arguments.get(1));
        }
        else
        {
            separatedForEach(arguments, this::visitElement, () -> word(","));
        }

        write(")");

        return null;
    }

    @Override
    public Void visit(ExistsExpression existsExpression)
    {
        if(existsExpression.isNegated())
            word("NOT");

        word("EXISTS");

        visitElement(existsExpression.getPattern());

        shouldIndent = false;

        return null;
    }

    @Override
    public Void visit(FunctionCallExpression functionCallExpression)
    {
        visitElement(functionCallExpression.getFunction());
        write("(");
        if(functionCallExpression.isDistinct())
            word("DISTINCT");

        separatedForEach(functionCallExpression.getArguments(), this::visitElement, () -> word(","));

        write(")");

        return null;
    }

    @Override
    public Void visit(IRI iri)
    {
        write(iri.toString(prologue, noIriSimplification, ignorePredefinedPrefixes));

        return null;
    }

    @Override
    public Void visit(PrefixedName prefixedName)
    {
        write(prefixedName.getPrefix());
        write(":");
        write(prefixedName.getLocalName());

        return null;
    }

    @Override
    public Void visit(Literal literal)
    {
        // FIXME: use better print method

        IRI typeIri = literal.getTypeIri();
        URI typeUri = typeIri == null ? null : typeIri.getUri();

        if(literal.getValue() == null)
        {
            writeAsString(literal, typeUri);
            return null;
        }

        if(URI.create(Xsd.INTEGER).equals(typeUri) || URI.create(Xsd.BOOLEAN).equals(typeUri))
        {
            write(literal.getValue().toString());
        }
        else if(URI.create(Xsd.DECIMAL).equals(typeUri))
        {
            BigDecimal value = (BigDecimal) literal.getValue();

            if(value.scale() == 0)
                value = value.setScale(1);

            write(value.toPlainString());
        }
        else if(URI.create(Xsd.DOUBLE).equals(typeUri))
        {
            DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(Locale.US);
            // this many #s should be enough for everyone
            format.applyPattern("#.#################################E0");

            write(format.format(literal.getValue()).replace("Infinity", "INF"));
        }
        else
        {
            writeAsString(literal, typeUri);
        }

        return null;
    }

    private void writeAsString(Literal literal, URI typeUri)
    {
        writeQuoted(literal.getStringValue());

        if(literal.getLanguageTag() != null)
        {
            write("@");
            write(literal.getLanguageTag());
        }
        else if(typeUri != null)
        {
            write("^^");
            visit(literal.getTypeIri());
        }
    }

    private void writeQuoted(String stringValue)
    {
        write("\"");
        // escape \ " \r \n
        write(stringValue.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n"));
        write("\"");
    }

    @Override
    public Void visit(Variable variable)
    {
        write("?");
        write(variable.getName());

        return null;
    }

    @Override
    public Void visit(Triple triple)
    {
        visitElement(triple.getSubject());
        word();

        visitElement(triple.getPredicate());
        word();

        visitElement(triple.getObject());
        word();

        write(".");
        newLine();

        return null;
    }

    @Override
    public Void visit(BlankNode blankNode)
    {
        write("_:");
        write(blankNode.getName());

        return null;
    }

    @Override
    public Void visit(AlternativePath alternativePath)
    {
        separatedForEach(alternativePath.getChildren(), this::visitElement, () -> word(" |"));

        return null;
    }

    @Override
    public Void visit(SequencePath sequencePath)
    {
        separatedForEach(sequencePath.getChildren(), this::visitElement, () -> word(" /"));

        return null;
    }

    @Override
    public Void visit(InversePath inversePath)
    {
        write("^");
        visitElement(inversePath.getChild());

        return null;
    }

    @Override
    public Void visit(RepeatedPath repeatedPath)
    {
        visitElement(repeatedPath.getChild());
        write(repeatedPath.getKind().getText());

        return null;
    }

    @Override
    public Void visit(NegatedPath negatedPath)
    {
        write("!");
        visitElement(negatedPath.getChild());

        return null;
    }

    @Override
    public Void visit(BracketedPath bracketedPath)
    {
        write("(");
        visitElement(bracketedPath.getChild());
        write(")");

        return null;
    }

    @Override
    public Void visit(ProcedureCall procedureCall)
    {
        visitElement(procedureCall.getResult());
        word();

        visitProcedureBase(procedureCall);

        return null;
    }

    @Override
    public Void visit(MultiProcedureCall multiProcedureCall)
    {
        word("[");

        separatedForEach(multiProcedureCall.getResults(), this::visit, () -> word(";"));
        word();

        word("]");

        visitProcedureBase(multiProcedureCall);

        return null;
    }

    private void visitProcedureBase(ProcedureCallBase procedure)
    {
        visit(procedure.getProcedure());
        word();

        word("[");

        separatedForEach(procedure.getParameters(), this::visit, () -> word(";"));
        word();

        word("]");
        write(".");
        newLine();
    }

    @Override
    public Void visit(ProcedureCall.Parameter parameter)
    {
        visit(parameter.getName());
        word();
        visitElement(parameter.getValue());

        return null;
    }
}
