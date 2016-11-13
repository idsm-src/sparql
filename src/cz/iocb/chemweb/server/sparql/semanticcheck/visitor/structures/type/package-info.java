/**
 * Type structures that represent unions or intersections of classes with necessary visitors. Types of variables in
 * different blocks of queries or property ranges and domains can be represented as unions and intersections of ontology
 * classes. This is combined in a tree structure that can be:
 * <ul>
 * <li>printed in infix order by {@link PrintInfix},</li>
 * <li>evaluated for 'satisfiability' of a type tree considering one specific class value in
 * {@link ConstraintEvaluation}</li>
 * <li>and evaluation of 'compatibility' between two more complicated type structures in
 * {@link OccurenceEvaluation}</li>
 * </ul>
 */
package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type;

