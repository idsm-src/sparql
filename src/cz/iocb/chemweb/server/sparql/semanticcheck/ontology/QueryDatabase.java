package cz.iocb.chemweb.server.sparql.semanticcheck.ontology;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import virtuoso.jdbc4.VirtuosoExtendedString;
import virtuoso.jdbc4.VirtuosoRdfBox;
import virtuoso.jdbc4.VirtuosoResultSet;



public class QueryDatabase
{
    /**
     * Get query result with header
     * 
     * @param query query
     * @param connection connection
     * @return result
     * @throws SQLException
     */
    public static ArrayList<ArrayList<String>> queryWithMeta(String query, Connection connection) throws SQLException
    {
        if(connection == null)
            return null;

        // exec query
        Statement stmt = connection.createStatement();
        boolean more = stmt.execute(query);
        ArrayList<ArrayList<String>> result = new ArrayList<>();

        // load result
        while(more)
        {
            ResultSet resultSet = stmt.getResultSet();

            // header
            ResultSetMetaData metadata = stmt.getResultSet().getMetaData();

            if(metadata.getColumnCount() > 0)
            {
                ArrayList<String> line = new ArrayList<>();
                for(int i = 0; i < metadata.getColumnCount(); i++)
                {
                    String name = metadata.getColumnName(i + 1);
                    line.add(name);
                }
                result.add(line);
            }
            // data
            while(resultSet.next())
            {
                ArrayList<String> line = new ArrayList<>();
                for(int i = 0; i < metadata.getColumnCount(); i++)
                {
                    String s = resultSet.getString(i + 1);
                    Object o = ((VirtuosoResultSet) resultSet).getObject(i + 1);

                    if(o instanceof VirtuosoExtendedString)
                    {
                        VirtuosoExtendedString vs = (VirtuosoExtendedString) o;

                        if(vs.iriType == VirtuosoExtendedString.IRI && (vs.strType & 0x01) == 0x01)
                        {
                            // IRI
                            line.add("<" + vs.str + ">");
                        }
                        else if(vs.iriType == VirtuosoExtendedString.BNODE)
                        {
                            // blank node
                            line.add("<" + vs.str + ">");
                        }
                    }
                    else if(o instanceof VirtuosoRdfBox)
                    {
                        // literal
                        VirtuosoRdfBox rb = (VirtuosoRdfBox) o;

                        if(rb.getType() != null)
                            line.add("\"" + rb.rb_box + "\"^^" + rb.getType());
                        else if(rb.getLang() != null)
                            line.add("\"" + rb.rb_box + "\"@" + rb.getLang());
                        else
                            line.add("\"" + rb.rb_box + "\"");

                    }
                    else if(stmt.getResultSet().wasNull())
                    {
                        line.add("null");
                    }
                    else
                    {
                        // literal
                        line.add("\"" + s + "\"");
                    }
                }
                result.add(line);
            }
            more = stmt.getMoreResults();
        }

        stmt.close();
        return result;
    }

    /**
     * Get query result without header
     * 
     * @param query query
     * @param connection connection
     * @return result
     * @throws SQLException
     */
    public static ArrayList<ArrayList<String>> query(String query, Connection connection) throws SQLException
    {
        if(connection == null)
            return null;

        // exec query
        try (Statement stmt = connection.createStatement())
        {
            try (ResultSet resultSet = stmt.executeQuery(query))
            {
                ArrayList<ArrayList<String>> result = new ArrayList<>();


                // header
                ResultSetMetaData metadata = resultSet.getMetaData();

                // data
                while(resultSet.next())
                {
                    ArrayList<String> line = new ArrayList<>();
                    for(int i = 0; i < metadata.getColumnCount(); i++)
                    {
                        String s = resultSet.getString(i + 1);
                        Object o = resultSet.getObject(i + 1);

                        if(o instanceof VirtuosoExtendedString)
                        {
                            VirtuosoExtendedString vs = (VirtuosoExtendedString) o;

                            if(vs.iriType == VirtuosoExtendedString.IRI && (vs.strType & 0x01) == 0x01)
                            {
                                // IRI
                                line.add("<" + vs.str + ">");
                            }
                            else if(vs.iriType == VirtuosoExtendedString.BNODE)
                            {
                                // blank node
                                line.add("<" + vs.str + ">");
                            }
                        }
                        else if(o instanceof VirtuosoRdfBox)
                        {
                            // literal
                            VirtuosoRdfBox rb = (VirtuosoRdfBox) o;

                            if(rb.getType() != null)
                                line.add("\"" + rb.rb_box + "\"^^" + rb.getType());
                            else if(rb.getLang() != null)
                                line.add("\"" + rb.rb_box + "\"@" + rb.getLang());
                            else
                                line.add("\"" + rb.rb_box + "\"");

                        }
                        else if(stmt.getResultSet().wasNull())
                        {
                            line.add("null");
                        }
                        else
                        {
                            // literal
                            line.add("\"" + s + "\"");
                        }
                    }
                    result.add(line);
                }


                return result;
            }
        }
    }

    /**
     * Transform list structure to map.
     * 
     * @param queryresult result of 2 columns
     * @return map
     */
    public static Map<String, Set<String>> extractMapFromCols(ArrayList<ArrayList<String>> queryresult)
    {
        if(queryresult != null)
        {
            Map<String, Set<String>> extracted = new HashMap<>();

            for(ArrayList<String> line : queryresult)
                if(line != null && line.size() == 2)
                {
                    extracted.putIfAbsent(line.get(0), new HashSet<>());
                    extracted.get(line.get(0)).add(line.get(1));
                }
            return extracted;
        }
        else
            return null;
    }

    /**
     * Transform list structure to set
     * 
     * @param queryresult query result
     * @param col which column should be transformed to set
     * @return set
     */
    public static Set<String> extractSetFromCol(ArrayList<ArrayList<String>> queryresult, Integer col)
    {
        if(queryresult != null)
        {
            Set<String> collect = queryresult.stream().map(list -> list.size() > col ? list.get(col) : null)
                    .collect(Collectors.toSet());
            return collect;
        }
        else
            return null;
    }
}
