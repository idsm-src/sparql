package cz.iocb.sparql.engine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;



/**
 * Equality of variables: two variables of one name are the same variable only in the same scope, and the variables
 * without a scope, which the translator makes, are told apart by their names alone.
 */
public class VariableNodeTest
{
    @Test
    @DisplayName("variables of one name are equal only in one scope")
    void scopes()
    {
        assertEquals(new VariableNode("ctx0", "x"), new VariableNode("ctx0", "?x"));
        assertEquals(new VariableNode("ctx0", "x").hashCode(), new VariableNode("ctx0", "$x").hashCode());
        assertNotEquals(new VariableNode("ctx0", "x"), new VariableNode("ctx1", "x"));
        assertNotEquals(new VariableNode("ctx0", "x"), new VariableNode("ctx0", "y"));
    }


    @Test
    @DisplayName("variables without a scope are compared by their names")
    void withoutScope()
    {
        assertEquals(new VariableNode("x"), new VariableNode("?x"));
        assertEquals(new VariableNode("x").hashCode(), new VariableNode("$x").hashCode());
        assertNotEquals(new VariableNode("x"), new VariableNode("y"));
        assertNotEquals(new VariableNode("x"), new VariableNode("ctx0", "x"));
        assertNotEquals(new VariableNode("ctx0", "x"), new VariableNode("x"));
    }
}
