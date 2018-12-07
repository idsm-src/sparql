// Generated from SparqlParser.g4 by ANTLR 4.5.1
 package cz.iocb.chemweb.server.sparql.grammar; 
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SparqlParser}.
 */
public interface SparqlParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link SparqlParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(SparqlParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(SparqlParser.QueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#prologue}.
	 * @param ctx the parse tree
	 */
	void enterPrologue(SparqlParser.PrologueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#prologue}.
	 * @param ctx the parse tree
	 */
	void exitPrologue(SparqlParser.PrologueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#baseDecl}.
	 * @param ctx the parse tree
	 */
	void enterBaseDecl(SparqlParser.BaseDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#baseDecl}.
	 * @param ctx the parse tree
	 */
	void exitBaseDecl(SparqlParser.BaseDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#prefixDecl}.
	 * @param ctx the parse tree
	 */
	void enterPrefixDecl(SparqlParser.PrefixDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#prefixDecl}.
	 * @param ctx the parse tree
	 */
	void exitPrefixDecl(SparqlParser.PrefixDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#selectQuery}.
	 * @param ctx the parse tree
	 */
	void enterSelectQuery(SparqlParser.SelectQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#selectQuery}.
	 * @param ctx the parse tree
	 */
	void exitSelectQuery(SparqlParser.SelectQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#subSelect}.
	 * @param ctx the parse tree
	 */
	void enterSubSelect(SparqlParser.SubSelectContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#subSelect}.
	 * @param ctx the parse tree
	 */
	void exitSubSelect(SparqlParser.SubSelectContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void enterSelectClause(SparqlParser.SelectClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void exitSelectClause(SparqlParser.SelectClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#selectVariable}.
	 * @param ctx the parse tree
	 */
	void enterSelectVariable(SparqlParser.SelectVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#selectVariable}.
	 * @param ctx the parse tree
	 */
	void exitSelectVariable(SparqlParser.SelectVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#constructQuery}.
	 * @param ctx the parse tree
	 */
	void enterConstructQuery(SparqlParser.ConstructQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#constructQuery}.
	 * @param ctx the parse tree
	 */
	void exitConstructQuery(SparqlParser.ConstructQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#describeQuery}.
	 * @param ctx the parse tree
	 */
	void enterDescribeQuery(SparqlParser.DescribeQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#describeQuery}.
	 * @param ctx the parse tree
	 */
	void exitDescribeQuery(SparqlParser.DescribeQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#askQuery}.
	 * @param ctx the parse tree
	 */
	void enterAskQuery(SparqlParser.AskQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#askQuery}.
	 * @param ctx the parse tree
	 */
	void exitAskQuery(SparqlParser.AskQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#datasetClause}.
	 * @param ctx the parse tree
	 */
	void enterDatasetClause(SparqlParser.DatasetClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#datasetClause}.
	 * @param ctx the parse tree
	 */
	void exitDatasetClause(SparqlParser.DatasetClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void enterWhereClause(SparqlParser.WhereClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void exitWhereClause(SparqlParser.WhereClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#solutionModifier}.
	 * @param ctx the parse tree
	 */
	void enterSolutionModifier(SparqlParser.SolutionModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#solutionModifier}.
	 * @param ctx the parse tree
	 */
	void exitSolutionModifier(SparqlParser.SolutionModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#groupClause}.
	 * @param ctx the parse tree
	 */
	void enterGroupClause(SparqlParser.GroupClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#groupClause}.
	 * @param ctx the parse tree
	 */
	void exitGroupClause(SparqlParser.GroupClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#groupCondition}.
	 * @param ctx the parse tree
	 */
	void enterGroupCondition(SparqlParser.GroupConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#groupCondition}.
	 * @param ctx the parse tree
	 */
	void exitGroupCondition(SparqlParser.GroupConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void enterHavingClause(SparqlParser.HavingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void exitHavingClause(SparqlParser.HavingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#havingCondition}.
	 * @param ctx the parse tree
	 */
	void enterHavingCondition(SparqlParser.HavingConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#havingCondition}.
	 * @param ctx the parse tree
	 */
	void exitHavingCondition(SparqlParser.HavingConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#orderClause}.
	 * @param ctx the parse tree
	 */
	void enterOrderClause(SparqlParser.OrderClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#orderClause}.
	 * @param ctx the parse tree
	 */
	void exitOrderClause(SparqlParser.OrderClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#orderCondition}.
	 * @param ctx the parse tree
	 */
	void enterOrderCondition(SparqlParser.OrderConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#orderCondition}.
	 * @param ctx the parse tree
	 */
	void exitOrderCondition(SparqlParser.OrderConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#limitOffsetClauses}.
	 * @param ctx the parse tree
	 */
	void enterLimitOffsetClauses(SparqlParser.LimitOffsetClausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#limitOffsetClauses}.
	 * @param ctx the parse tree
	 */
	void exitLimitOffsetClauses(SparqlParser.LimitOffsetClausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#limitClause}.
	 * @param ctx the parse tree
	 */
	void enterLimitClause(SparqlParser.LimitClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#limitClause}.
	 * @param ctx the parse tree
	 */
	void exitLimitClause(SparqlParser.LimitClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#offsetClause}.
	 * @param ctx the parse tree
	 */
	void enterOffsetClause(SparqlParser.OffsetClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#offsetClause}.
	 * @param ctx the parse tree
	 */
	void exitOffsetClause(SparqlParser.OffsetClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#valuesClause}.
	 * @param ctx the parse tree
	 */
	void enterValuesClause(SparqlParser.ValuesClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#valuesClause}.
	 * @param ctx the parse tree
	 */
	void exitValuesClause(SparqlParser.ValuesClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#updateCommand}.
	 * @param ctx the parse tree
	 */
	void enterUpdateCommand(SparqlParser.UpdateCommandContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#updateCommand}.
	 * @param ctx the parse tree
	 */
	void exitUpdateCommand(SparqlParser.UpdateCommandContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#update}.
	 * @param ctx the parse tree
	 */
	void enterUpdate(SparqlParser.UpdateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#update}.
	 * @param ctx the parse tree
	 */
	void exitUpdate(SparqlParser.UpdateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#load}.
	 * @param ctx the parse tree
	 */
	void enterLoad(SparqlParser.LoadContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#load}.
	 * @param ctx the parse tree
	 */
	void exitLoad(SparqlParser.LoadContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#clear}.
	 * @param ctx the parse tree
	 */
	void enterClear(SparqlParser.ClearContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#clear}.
	 * @param ctx the parse tree
	 */
	void exitClear(SparqlParser.ClearContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#drop}.
	 * @param ctx the parse tree
	 */
	void enterDrop(SparqlParser.DropContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#drop}.
	 * @param ctx the parse tree
	 */
	void exitDrop(SparqlParser.DropContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#create}.
	 * @param ctx the parse tree
	 */
	void enterCreate(SparqlParser.CreateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#create}.
	 * @param ctx the parse tree
	 */
	void exitCreate(SparqlParser.CreateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#add}.
	 * @param ctx the parse tree
	 */
	void enterAdd(SparqlParser.AddContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#add}.
	 * @param ctx the parse tree
	 */
	void exitAdd(SparqlParser.AddContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#move}.
	 * @param ctx the parse tree
	 */
	void enterMove(SparqlParser.MoveContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#move}.
	 * @param ctx the parse tree
	 */
	void exitMove(SparqlParser.MoveContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#copy}.
	 * @param ctx the parse tree
	 */
	void enterCopy(SparqlParser.CopyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#copy}.
	 * @param ctx the parse tree
	 */
	void exitCopy(SparqlParser.CopyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#insertData}.
	 * @param ctx the parse tree
	 */
	void enterInsertData(SparqlParser.InsertDataContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#insertData}.
	 * @param ctx the parse tree
	 */
	void exitInsertData(SparqlParser.InsertDataContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#deleteData}.
	 * @param ctx the parse tree
	 */
	void enterDeleteData(SparqlParser.DeleteDataContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#deleteData}.
	 * @param ctx the parse tree
	 */
	void exitDeleteData(SparqlParser.DeleteDataContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#deleteWhere}.
	 * @param ctx the parse tree
	 */
	void enterDeleteWhere(SparqlParser.DeleteWhereContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#deleteWhere}.
	 * @param ctx the parse tree
	 */
	void exitDeleteWhere(SparqlParser.DeleteWhereContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#modify}.
	 * @param ctx the parse tree
	 */
	void enterModify(SparqlParser.ModifyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#modify}.
	 * @param ctx the parse tree
	 */
	void exitModify(SparqlParser.ModifyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#deleteClause}.
	 * @param ctx the parse tree
	 */
	void enterDeleteClause(SparqlParser.DeleteClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#deleteClause}.
	 * @param ctx the parse tree
	 */
	void exitDeleteClause(SparqlParser.DeleteClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#insertClause}.
	 * @param ctx the parse tree
	 */
	void enterInsertClause(SparqlParser.InsertClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#insertClause}.
	 * @param ctx the parse tree
	 */
	void exitInsertClause(SparqlParser.InsertClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#usingClause}.
	 * @param ctx the parse tree
	 */
	void enterUsingClause(SparqlParser.UsingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#usingClause}.
	 * @param ctx the parse tree
	 */
	void exitUsingClause(SparqlParser.UsingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#graphOrDefault}.
	 * @param ctx the parse tree
	 */
	void enterGraphOrDefault(SparqlParser.GraphOrDefaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#graphOrDefault}.
	 * @param ctx the parse tree
	 */
	void exitGraphOrDefault(SparqlParser.GraphOrDefaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#graphRef}.
	 * @param ctx the parse tree
	 */
	void enterGraphRef(SparqlParser.GraphRefContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#graphRef}.
	 * @param ctx the parse tree
	 */
	void exitGraphRef(SparqlParser.GraphRefContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#graphRefAll}.
	 * @param ctx the parse tree
	 */
	void enterGraphRefAll(SparqlParser.GraphRefAllContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#graphRefAll}.
	 * @param ctx the parse tree
	 */
	void exitGraphRefAll(SparqlParser.GraphRefAllContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#quadPattern}.
	 * @param ctx the parse tree
	 */
	void enterQuadPattern(SparqlParser.QuadPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#quadPattern}.
	 * @param ctx the parse tree
	 */
	void exitQuadPattern(SparqlParser.QuadPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#quadData}.
	 * @param ctx the parse tree
	 */
	void enterQuadData(SparqlParser.QuadDataContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#quadData}.
	 * @param ctx the parse tree
	 */
	void exitQuadData(SparqlParser.QuadDataContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#quads}.
	 * @param ctx the parse tree
	 */
	void enterQuads(SparqlParser.QuadsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#quads}.
	 * @param ctx the parse tree
	 */
	void exitQuads(SparqlParser.QuadsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#quadsDetails}.
	 * @param ctx the parse tree
	 */
	void enterQuadsDetails(SparqlParser.QuadsDetailsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#quadsDetails}.
	 * @param ctx the parse tree
	 */
	void exitQuadsDetails(SparqlParser.QuadsDetailsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#quadsNotTriples}.
	 * @param ctx the parse tree
	 */
	void enterQuadsNotTriples(SparqlParser.QuadsNotTriplesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#quadsNotTriples}.
	 * @param ctx the parse tree
	 */
	void exitQuadsNotTriples(SparqlParser.QuadsNotTriplesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#triplesTemplate}.
	 * @param ctx the parse tree
	 */
	void enterTriplesTemplate(SparqlParser.TriplesTemplateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#triplesTemplate}.
	 * @param ctx the parse tree
	 */
	void exitTriplesTemplate(SparqlParser.TriplesTemplateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#groupGraphPattern}.
	 * @param ctx the parse tree
	 */
	void enterGroupGraphPattern(SparqlParser.GroupGraphPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#groupGraphPattern}.
	 * @param ctx the parse tree
	 */
	void exitGroupGraphPattern(SparqlParser.GroupGraphPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#groupGraphPatternSub}.
	 * @param ctx the parse tree
	 */
	void enterGroupGraphPatternSub(SparqlParser.GroupGraphPatternSubContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#groupGraphPatternSub}.
	 * @param ctx the parse tree
	 */
	void exitGroupGraphPatternSub(SparqlParser.GroupGraphPatternSubContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#groupGraphPatternSubList}.
	 * @param ctx the parse tree
	 */
	void enterGroupGraphPatternSubList(SparqlParser.GroupGraphPatternSubListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#groupGraphPatternSubList}.
	 * @param ctx the parse tree
	 */
	void exitGroupGraphPatternSubList(SparqlParser.GroupGraphPatternSubListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#triplesBlock}.
	 * @param ctx the parse tree
	 */
	void enterTriplesBlock(SparqlParser.TriplesBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#triplesBlock}.
	 * @param ctx the parse tree
	 */
	void exitTriplesBlock(SparqlParser.TriplesBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#graphPatternNotTriples}.
	 * @param ctx the parse tree
	 */
	void enterGraphPatternNotTriples(SparqlParser.GraphPatternNotTriplesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#graphPatternNotTriples}.
	 * @param ctx the parse tree
	 */
	void exitGraphPatternNotTriples(SparqlParser.GraphPatternNotTriplesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#optionalGraphPattern}.
	 * @param ctx the parse tree
	 */
	void enterOptionalGraphPattern(SparqlParser.OptionalGraphPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#optionalGraphPattern}.
	 * @param ctx the parse tree
	 */
	void exitOptionalGraphPattern(SparqlParser.OptionalGraphPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#graphGraphPattern}.
	 * @param ctx the parse tree
	 */
	void enterGraphGraphPattern(SparqlParser.GraphGraphPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#graphGraphPattern}.
	 * @param ctx the parse tree
	 */
	void exitGraphGraphPattern(SparqlParser.GraphGraphPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#serviceGraphPattern}.
	 * @param ctx the parse tree
	 */
	void enterServiceGraphPattern(SparqlParser.ServiceGraphPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#serviceGraphPattern}.
	 * @param ctx the parse tree
	 */
	void exitServiceGraphPattern(SparqlParser.ServiceGraphPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#bind}.
	 * @param ctx the parse tree
	 */
	void enterBind(SparqlParser.BindContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#bind}.
	 * @param ctx the parse tree
	 */
	void exitBind(SparqlParser.BindContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#inlineData}.
	 * @param ctx the parse tree
	 */
	void enterInlineData(SparqlParser.InlineDataContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#inlineData}.
	 * @param ctx the parse tree
	 */
	void exitInlineData(SparqlParser.InlineDataContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#dataBlock}.
	 * @param ctx the parse tree
	 */
	void enterDataBlock(SparqlParser.DataBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#dataBlock}.
	 * @param ctx the parse tree
	 */
	void exitDataBlock(SparqlParser.DataBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#inlineDataOneVar}.
	 * @param ctx the parse tree
	 */
	void enterInlineDataOneVar(SparqlParser.InlineDataOneVarContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#inlineDataOneVar}.
	 * @param ctx the parse tree
	 */
	void exitInlineDataOneVar(SparqlParser.InlineDataOneVarContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#inlineDataFull}.
	 * @param ctx the parse tree
	 */
	void enterInlineDataFull(SparqlParser.InlineDataFullContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#inlineDataFull}.
	 * @param ctx the parse tree
	 */
	void exitInlineDataFull(SparqlParser.InlineDataFullContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#dataBlockValues}.
	 * @param ctx the parse tree
	 */
	void enterDataBlockValues(SparqlParser.DataBlockValuesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#dataBlockValues}.
	 * @param ctx the parse tree
	 */
	void exitDataBlockValues(SparqlParser.DataBlockValuesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#dataBlockValue}.
	 * @param ctx the parse tree
	 */
	void enterDataBlockValue(SparqlParser.DataBlockValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#dataBlockValue}.
	 * @param ctx the parse tree
	 */
	void exitDataBlockValue(SparqlParser.DataBlockValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#minusGraphPattern}.
	 * @param ctx the parse tree
	 */
	void enterMinusGraphPattern(SparqlParser.MinusGraphPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#minusGraphPattern}.
	 * @param ctx the parse tree
	 */
	void exitMinusGraphPattern(SparqlParser.MinusGraphPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#groupOrUnionGraphPattern}.
	 * @param ctx the parse tree
	 */
	void enterGroupOrUnionGraphPattern(SparqlParser.GroupOrUnionGraphPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#groupOrUnionGraphPattern}.
	 * @param ctx the parse tree
	 */
	void exitGroupOrUnionGraphPattern(SparqlParser.GroupOrUnionGraphPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterFilter(SparqlParser.FilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitFilter(SparqlParser.FilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#constraint}.
	 * @param ctx the parse tree
	 */
	void enterConstraint(SparqlParser.ConstraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#constraint}.
	 * @param ctx the parse tree
	 */
	void exitConstraint(SparqlParser.ConstraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(SparqlParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(SparqlParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#argList}.
	 * @param ctx the parse tree
	 */
	void enterArgList(SparqlParser.ArgListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#argList}.
	 * @param ctx the parse tree
	 */
	void exitArgList(SparqlParser.ArgListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionList(SparqlParser.ExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionList(SparqlParser.ExpressionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#constructTemplate}.
	 * @param ctx the parse tree
	 */
	void enterConstructTemplate(SparqlParser.ConstructTemplateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#constructTemplate}.
	 * @param ctx the parse tree
	 */
	void exitConstructTemplate(SparqlParser.ConstructTemplateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#constructTriples}.
	 * @param ctx the parse tree
	 */
	void enterConstructTriples(SparqlParser.ConstructTriplesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#constructTriples}.
	 * @param ctx the parse tree
	 */
	void exitConstructTriples(SparqlParser.ConstructTriplesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#triplesSameSubject}.
	 * @param ctx the parse tree
	 */
	void enterTriplesSameSubject(SparqlParser.TriplesSameSubjectContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#triplesSameSubject}.
	 * @param ctx the parse tree
	 */
	void exitTriplesSameSubject(SparqlParser.TriplesSameSubjectContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#propertyList}.
	 * @param ctx the parse tree
	 */
	void enterPropertyList(SparqlParser.PropertyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#propertyList}.
	 * @param ctx the parse tree
	 */
	void exitPropertyList(SparqlParser.PropertyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#propertyListNotEmpty}.
	 * @param ctx the parse tree
	 */
	void enterPropertyListNotEmpty(SparqlParser.PropertyListNotEmptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#propertyListNotEmpty}.
	 * @param ctx the parse tree
	 */
	void exitPropertyListNotEmpty(SparqlParser.PropertyListNotEmptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#verb}.
	 * @param ctx the parse tree
	 */
	void enterVerb(SparqlParser.VerbContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#verb}.
	 * @param ctx the parse tree
	 */
	void exitVerb(SparqlParser.VerbContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#objectList}.
	 * @param ctx the parse tree
	 */
	void enterObjectList(SparqlParser.ObjectListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#objectList}.
	 * @param ctx the parse tree
	 */
	void exitObjectList(SparqlParser.ObjectListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#object}.
	 * @param ctx the parse tree
	 */
	void enterObject(SparqlParser.ObjectContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#object}.
	 * @param ctx the parse tree
	 */
	void exitObject(SparqlParser.ObjectContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#triplesSameSubjectPath}.
	 * @param ctx the parse tree
	 */
	void enterTriplesSameSubjectPath(SparqlParser.TriplesSameSubjectPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#triplesSameSubjectPath}.
	 * @param ctx the parse tree
	 */
	void exitTriplesSameSubjectPath(SparqlParser.TriplesSameSubjectPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#propertyListPath}.
	 * @param ctx the parse tree
	 */
	void enterPropertyListPath(SparqlParser.PropertyListPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#propertyListPath}.
	 * @param ctx the parse tree
	 */
	void exitPropertyListPath(SparqlParser.PropertyListPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#propertyListPathNotEmpty}.
	 * @param ctx the parse tree
	 */
	void enterPropertyListPathNotEmpty(SparqlParser.PropertyListPathNotEmptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#propertyListPathNotEmpty}.
	 * @param ctx the parse tree
	 */
	void exitPropertyListPathNotEmpty(SparqlParser.PropertyListPathNotEmptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#propertyListPathNotEmptyList}.
	 * @param ctx the parse tree
	 */
	void enterPropertyListPathNotEmptyList(SparqlParser.PropertyListPathNotEmptyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#propertyListPathNotEmptyList}.
	 * @param ctx the parse tree
	 */
	void exitPropertyListPathNotEmptyList(SparqlParser.PropertyListPathNotEmptyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#verbPath}.
	 * @param ctx the parse tree
	 */
	void enterVerbPath(SparqlParser.VerbPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#verbPath}.
	 * @param ctx the parse tree
	 */
	void exitVerbPath(SparqlParser.VerbPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#verbSimple}.
	 * @param ctx the parse tree
	 */
	void enterVerbSimple(SparqlParser.VerbSimpleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#verbSimple}.
	 * @param ctx the parse tree
	 */
	void exitVerbSimple(SparqlParser.VerbSimpleContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#objectListPath}.
	 * @param ctx the parse tree
	 */
	void enterObjectListPath(SparqlParser.ObjectListPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#objectListPath}.
	 * @param ctx the parse tree
	 */
	void exitObjectListPath(SparqlParser.ObjectListPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#objectPath}.
	 * @param ctx the parse tree
	 */
	void enterObjectPath(SparqlParser.ObjectPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#objectPath}.
	 * @param ctx the parse tree
	 */
	void exitObjectPath(SparqlParser.ObjectPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#path}.
	 * @param ctx the parse tree
	 */
	void enterPath(SparqlParser.PathContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#path}.
	 * @param ctx the parse tree
	 */
	void exitPath(SparqlParser.PathContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#pathAlternative}.
	 * @param ctx the parse tree
	 */
	void enterPathAlternative(SparqlParser.PathAlternativeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#pathAlternative}.
	 * @param ctx the parse tree
	 */
	void exitPathAlternative(SparqlParser.PathAlternativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#pathSequence}.
	 * @param ctx the parse tree
	 */
	void enterPathSequence(SparqlParser.PathSequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#pathSequence}.
	 * @param ctx the parse tree
	 */
	void exitPathSequence(SparqlParser.PathSequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#pathElt}.
	 * @param ctx the parse tree
	 */
	void enterPathElt(SparqlParser.PathEltContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#pathElt}.
	 * @param ctx the parse tree
	 */
	void exitPathElt(SparqlParser.PathEltContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#pathEltOrInverse}.
	 * @param ctx the parse tree
	 */
	void enterPathEltOrInverse(SparqlParser.PathEltOrInverseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#pathEltOrInverse}.
	 * @param ctx the parse tree
	 */
	void exitPathEltOrInverse(SparqlParser.PathEltOrInverseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#pathMod}.
	 * @param ctx the parse tree
	 */
	void enterPathMod(SparqlParser.PathModContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#pathMod}.
	 * @param ctx the parse tree
	 */
	void exitPathMod(SparqlParser.PathModContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#pathPrimary}.
	 * @param ctx the parse tree
	 */
	void enterPathPrimary(SparqlParser.PathPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#pathPrimary}.
	 * @param ctx the parse tree
	 */
	void exitPathPrimary(SparqlParser.PathPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#pathNegatedPropertySet}.
	 * @param ctx the parse tree
	 */
	void enterPathNegatedPropertySet(SparqlParser.PathNegatedPropertySetContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#pathNegatedPropertySet}.
	 * @param ctx the parse tree
	 */
	void exitPathNegatedPropertySet(SparqlParser.PathNegatedPropertySetContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#pathOneInPropertySet}.
	 * @param ctx the parse tree
	 */
	void enterPathOneInPropertySet(SparqlParser.PathOneInPropertySetContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#pathOneInPropertySet}.
	 * @param ctx the parse tree
	 */
	void exitPathOneInPropertySet(SparqlParser.PathOneInPropertySetContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#integer}.
	 * @param ctx the parse tree
	 */
	void enterInteger(SparqlParser.IntegerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#integer}.
	 * @param ctx the parse tree
	 */
	void exitInteger(SparqlParser.IntegerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#triplesNode}.
	 * @param ctx the parse tree
	 */
	void enterTriplesNode(SparqlParser.TriplesNodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#triplesNode}.
	 * @param ctx the parse tree
	 */
	void exitTriplesNode(SparqlParser.TriplesNodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#blankNodePropertyList}.
	 * @param ctx the parse tree
	 */
	void enterBlankNodePropertyList(SparqlParser.BlankNodePropertyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#blankNodePropertyList}.
	 * @param ctx the parse tree
	 */
	void exitBlankNodePropertyList(SparqlParser.BlankNodePropertyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#triplesNodePath}.
	 * @param ctx the parse tree
	 */
	void enterTriplesNodePath(SparqlParser.TriplesNodePathContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#triplesNodePath}.
	 * @param ctx the parse tree
	 */
	void exitTriplesNodePath(SparqlParser.TriplesNodePathContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#blankNodePropertyListPath}.
	 * @param ctx the parse tree
	 */
	void enterBlankNodePropertyListPath(SparqlParser.BlankNodePropertyListPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#blankNodePropertyListPath}.
	 * @param ctx the parse tree
	 */
	void exitBlankNodePropertyListPath(SparqlParser.BlankNodePropertyListPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#collection}.
	 * @param ctx the parse tree
	 */
	void enterCollection(SparqlParser.CollectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#collection}.
	 * @param ctx the parse tree
	 */
	void exitCollection(SparqlParser.CollectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#collectionPath}.
	 * @param ctx the parse tree
	 */
	void enterCollectionPath(SparqlParser.CollectionPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#collectionPath}.
	 * @param ctx the parse tree
	 */
	void exitCollectionPath(SparqlParser.CollectionPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#graphNode}.
	 * @param ctx the parse tree
	 */
	void enterGraphNode(SparqlParser.GraphNodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#graphNode}.
	 * @param ctx the parse tree
	 */
	void exitGraphNode(SparqlParser.GraphNodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#graphNodePath}.
	 * @param ctx the parse tree
	 */
	void enterGraphNodePath(SparqlParser.GraphNodePathContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#graphNodePath}.
	 * @param ctx the parse tree
	 */
	void exitGraphNodePath(SparqlParser.GraphNodePathContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#varOrTerm}.
	 * @param ctx the parse tree
	 */
	void enterVarOrTerm(SparqlParser.VarOrTermContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#varOrTerm}.
	 * @param ctx the parse tree
	 */
	void exitVarOrTerm(SparqlParser.VarOrTermContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#varOrIRI}.
	 * @param ctx the parse tree
	 */
	void enterVarOrIRI(SparqlParser.VarOrIRIContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#varOrIRI}.
	 * @param ctx the parse tree
	 */
	void exitVarOrIRI(SparqlParser.VarOrIRIContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#var}.
	 * @param ctx the parse tree
	 */
	void enterVar(SparqlParser.VarContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#var}.
	 * @param ctx the parse tree
	 */
	void exitVar(SparqlParser.VarContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#graphTerm}.
	 * @param ctx the parse tree
	 */
	void enterGraphTerm(SparqlParser.GraphTermContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#graphTerm}.
	 * @param ctx the parse tree
	 */
	void exitGraphTerm(SparqlParser.GraphTermContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#nil}.
	 * @param ctx the parse tree
	 */
	void enterNil(SparqlParser.NilContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#nil}.
	 * @param ctx the parse tree
	 */
	void exitNil(SparqlParser.NilContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unarySignedLiteralExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterUnarySignedLiteralExpression(SparqlParser.UnarySignedLiteralExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unarySignedLiteralExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitUnarySignedLiteralExpression(SparqlParser.UnarySignedLiteralExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code conditionalOrExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterConditionalOrExpression(SparqlParser.ConditionalOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code conditionalOrExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitConditionalOrExpression(SparqlParser.ConditionalOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code additiveExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpression(SparqlParser.AdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code additiveExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpression(SparqlParser.AdditiveExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unaryAdditiveExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryAdditiveExpression(SparqlParser.UnaryAdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unaryAdditiveExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryAdditiveExpression(SparqlParser.UnaryAdditiveExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code relationalExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpression(SparqlParser.RelationalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code relationalExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpression(SparqlParser.RelationalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code relationalSetExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterRelationalSetExpression(SparqlParser.RelationalSetExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code relationalSetExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitRelationalSetExpression(SparqlParser.RelationalSetExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code baseExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterBaseExpression(SparqlParser.BaseExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code baseExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitBaseExpression(SparqlParser.BaseExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code multiplicativeExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpression(SparqlParser.MultiplicativeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code multiplicativeExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpression(SparqlParser.MultiplicativeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code conditionalAndExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterConditionalAndExpression(SparqlParser.ConditionalAndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code conditionalAndExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitConditionalAndExpression(SparqlParser.ConditionalAndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unaryNegationExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryNegationExpression(SparqlParser.UnaryNegationExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unaryNegationExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryNegationExpression(SparqlParser.UnaryNegationExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#unaryLiteralExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryLiteralExpression(SparqlParser.UnaryLiteralExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#unaryLiteralExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryLiteralExpression(SparqlParser.UnaryLiteralExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpression(SparqlParser.UnaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpression(SparqlParser.UnaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryExpression(SparqlParser.PrimaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryExpression(SparqlParser.PrimaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#builtInCall}.
	 * @param ctx the parse tree
	 */
	void enterBuiltInCall(SparqlParser.BuiltInCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#builtInCall}.
	 * @param ctx the parse tree
	 */
	void exitBuiltInCall(SparqlParser.BuiltInCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#regexExpression}.
	 * @param ctx the parse tree
	 */
	void enterRegexExpression(SparqlParser.RegexExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#regexExpression}.
	 * @param ctx the parse tree
	 */
	void exitRegexExpression(SparqlParser.RegexExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#subStringExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubStringExpression(SparqlParser.SubStringExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#subStringExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubStringExpression(SparqlParser.SubStringExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#strReplaceExpression}.
	 * @param ctx the parse tree
	 */
	void enterStrReplaceExpression(SparqlParser.StrReplaceExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#strReplaceExpression}.
	 * @param ctx the parse tree
	 */
	void exitStrReplaceExpression(SparqlParser.StrReplaceExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#existsFunction}.
	 * @param ctx the parse tree
	 */
	void enterExistsFunction(SparqlParser.ExistsFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#existsFunction}.
	 * @param ctx the parse tree
	 */
	void exitExistsFunction(SparqlParser.ExistsFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#notExistsFunction}.
	 * @param ctx the parse tree
	 */
	void enterNotExistsFunction(SparqlParser.NotExistsFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#notExistsFunction}.
	 * @param ctx the parse tree
	 */
	void exitNotExistsFunction(SparqlParser.NotExistsFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#aggregate}.
	 * @param ctx the parse tree
	 */
	void enterAggregate(SparqlParser.AggregateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#aggregate}.
	 * @param ctx the parse tree
	 */
	void exitAggregate(SparqlParser.AggregateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#iriRefOrFunction}.
	 * @param ctx the parse tree
	 */
	void enterIriRefOrFunction(SparqlParser.IriRefOrFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#iriRefOrFunction}.
	 * @param ctx the parse tree
	 */
	void exitIriRefOrFunction(SparqlParser.IriRefOrFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#rdfLiteral}.
	 * @param ctx the parse tree
	 */
	void enterRdfLiteral(SparqlParser.RdfLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#rdfLiteral}.
	 * @param ctx the parse tree
	 */
	void exitRdfLiteral(SparqlParser.RdfLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#numericLiteral}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(SparqlParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#numericLiteral}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(SparqlParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#numericLiteralUnsigned}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteralUnsigned(SparqlParser.NumericLiteralUnsignedContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#numericLiteralUnsigned}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteralUnsigned(SparqlParser.NumericLiteralUnsignedContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#numericLiteralPositive}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteralPositive(SparqlParser.NumericLiteralPositiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#numericLiteralPositive}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteralPositive(SparqlParser.NumericLiteralPositiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#numericLiteralNegative}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteralNegative(SparqlParser.NumericLiteralNegativeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#numericLiteralNegative}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteralNegative(SparqlParser.NumericLiteralNegativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#booleanLiteral}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(SparqlParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#booleanLiteral}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(SparqlParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#string}.
	 * @param ctx the parse tree
	 */
	void enterString(SparqlParser.StringContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#string}.
	 * @param ctx the parse tree
	 */
	void exitString(SparqlParser.StringContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#iri}.
	 * @param ctx the parse tree
	 */
	void enterIri(SparqlParser.IriContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#iri}.
	 * @param ctx the parse tree
	 */
	void exitIri(SparqlParser.IriContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#prefixedName}.
	 * @param ctx the parse tree
	 */
	void enterPrefixedName(SparqlParser.PrefixedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#prefixedName}.
	 * @param ctx the parse tree
	 */
	void exitPrefixedName(SparqlParser.PrefixedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#blankNode}.
	 * @param ctx the parse tree
	 */
	void enterBlankNode(SparqlParser.BlankNodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#blankNode}.
	 * @param ctx the parse tree
	 */
	void exitBlankNode(SparqlParser.BlankNodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparqlParser#anon}.
	 * @param ctx the parse tree
	 */
	void enterAnon(SparqlParser.AnonContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparqlParser#anon}.
	 * @param ctx the parse tree
	 */
	void exitAnon(SparqlParser.AnonContext ctx);
}