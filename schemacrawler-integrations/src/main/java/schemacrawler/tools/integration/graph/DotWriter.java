/* 
 *
 * SchemaCrawler
 * http://sourceforge.net/projects/schemacrawler
 * Copyright (c) 2000-2010, Sualeh Fatehi.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 */

package schemacrawler.tools.integration.graph;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import schemacrawler.schema.Column;
import schemacrawler.schema.ColumnMap;
import schemacrawler.schema.DatabaseInfo;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.ForeignKeyColumnMap;
import schemacrawler.schema.JdbcDriverInfo;
import schemacrawler.schema.NamedObject;
import schemacrawler.schema.Schema;
import schemacrawler.schema.SchemaCrawlerInfo;
import schemacrawler.schema.Table;
import schemacrawler.schema.View;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.utility.MetaDataUtility;
import schemacrawler.utility.MetaDataUtility.Connectivity;
import sf.util.Utility;

final class DotWriter
{

  private final PrintWriter out;
  private final Map<Schema, PastelColor> colorMap;

  DotWriter(final File dotFile)
    throws SchemaCrawlerException
  {
    if (dotFile == null)
    {
      throw new SchemaCrawlerException("No dot file provided");
    }
    try
    {
      out = new PrintWriter(dotFile);
    }
    catch (final IOException e)
    {
      throw new SchemaCrawlerException("Cannot open dot file for output", e);
    }

    colorMap = new HashMap<Schema, PastelColor>();
  }

  public void close()
  {
    out.println("}");
    out.flush();
    //
    out.close();
  }

  public void open()
  {
    final String text = Utility.readResourceFully("/dot.header.txt");
    out.println(text);
  }

  public void print(final ColumnMap[] weakAssociations)
  {
    if (weakAssociations != null)
    {
      out.write(Utility.NEWLINE);
      for (final ColumnMap columnMap: weakAssociations)
      {
        final Column primaryKeyColumn = columnMap.getPrimaryKeyColumn();
        final Column foreignKeyColumn = columnMap.getForeignKeyColumn();
        out
          .write(printColumnAssociation("", primaryKeyColumn, foreignKeyColumn));
      }
    }
    out.write(Utility.NEWLINE);
    out.write(Utility.NEWLINE);
  }

  public void print(final SchemaCrawlerInfo schemaCrawlerInfo,
                    final DatabaseInfo databaseInfo,
                    final JdbcDriverInfo jdbcDriverInfo)
  {
    final StringBuilder graphInfo = new StringBuilder();

    // SchemaCrawler info
    graphInfo
      .append("      <table border=\"1\" cellborder=\"0\" cellspacing=\"0\">")
      .append(Utility.NEWLINE);

    graphInfo.append("        <tr>").append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"right\">Generated by:</td>")
      .append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"left\">").append(schemaCrawlerInfo
      .getSchemaCrawlerProductName()).append(" ").append(schemaCrawlerInfo
      .getSchemaCrawlerVersion()).append("</td>").append(Utility.NEWLINE);
    graphInfo.append("        </tr>").append(Utility.NEWLINE);

    // Database info
    graphInfo.append("        <tr>").append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"right\">Database:</td>")
      .append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"left\">").append(databaseInfo
      .getProductName()).append("  ").append(databaseInfo.getProductVersion())
      .append("</td>").append(Utility.NEWLINE);
    graphInfo.append("        </tr>").append(Utility.NEWLINE);

    // JDBC driver info
    graphInfo.append("        <tr>").append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"right\">JDBC Connection:</td>")
      .append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"left\">").append(jdbcDriverInfo
      .getConnectionUrl()).append("</td>").append(Utility.NEWLINE);
    graphInfo.append("        </tr>").append(Utility.NEWLINE);

    graphInfo.append("        <tr>").append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"right\">JDBC Driver:</td>")
      .append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"left\">").append(jdbcDriverInfo
      .getDriverName()).append("  ").append(jdbcDriverInfo.getDriverVersion())
      .append("</td>").append(Utility.NEWLINE);
    graphInfo.append("        </tr>").append(Utility.NEWLINE);

    graphInfo.append("      </table>");

    final String graphLabel = String
      .format("  graph [%n    label=<%n%s    >%n    labeljust=r%n    labelloc=b%n  ];%n%n",
              graphInfo.toString());

    out.println(graphLabel);
  }

  public void print(final Table table)
  {
    final Schema schema = table.getSchema();
    if (!colorMap.containsKey(schema))
    {
      colorMap.put(schema, new PastelColor());
    }
    final PastelColor bgcolor = colorMap.get(schema);
    final PastelColor tableBgColor = bgcolor.shade();
    final StringBuilder buffer = new StringBuilder();
    buffer.append("  \"").append(nodeName(table)).append("\" [")
      .append(Utility.NEWLINE).append("    label=<").append(Utility.NEWLINE);
    buffer
      .append("      <table border=\"1\" cellborder=\"0\" cellspacing=\"0\">")
      .append(Utility.NEWLINE);
    buffer.append("        <tr>").append(Utility.NEWLINE);

    buffer.append("          <td colspan=\"2\" bgcolor=\"")
      .append(tableBgColor).append("\" align=\"left\">").append(table
        .getFullName()).append("</td>").append(Utility.NEWLINE);
    buffer.append("          <td bgcolor=\"").append(tableBgColor)
      .append("\" align=\"right\">").append((table instanceof View? "[view]"
                                                                  : "[table]"))
      .append("</td>").append(Utility.NEWLINE);
    buffer.append("        </tr>").append(Utility.NEWLINE);
    for (final Column column: table.getColumns())
    {
      final PastelColor columnBgcolor;
      if (column.isPartOfPrimaryKey())
      {
        columnBgcolor = bgcolor;
      }
      else
      {
        columnBgcolor = bgcolor.tint();
      }
      buffer.append("        <tr>").append(Utility.NEWLINE);
      buffer.append("          <td port=\"").append(nodeName(column))
        .append(".start\" bgcolor=\"").append(columnBgcolor)
        .append("\" align=\"left\">").append(column.getName()).append("</td>")
        .append(Utility.NEWLINE);
      buffer.append("          <td bgcolor=\"").append(columnBgcolor)
        .append("\"> </td>").append(Utility.NEWLINE);
      buffer.append("          <td port=\"").append(nodeName(column))
        .append(".end\" align=\"right\" bgcolor=\"").append(columnBgcolor)
        .append("\">").append(column.getType().getDatabaseSpecificTypeName())
        .append(column.getWidth()).append("</td>").append(Utility.NEWLINE);
      buffer.append("        </tr>").append(Utility.NEWLINE);
    }
    buffer.append("      </table>").append(Utility.NEWLINE);
    buffer.append("    >").append(Utility.NEWLINE).append("  ];")
      .append(Utility.NEWLINE);

    for (final ForeignKey foreignKey: table.getForeignKeys())
    {
      for (final ForeignKeyColumnMap foreignKeyColumnMap: foreignKey
        .getColumnPairs())
      {
        final Column primaryKeyColumn = foreignKeyColumnMap
          .getPrimaryKeyColumn();
        final Column foreignKeyColumn = foreignKeyColumnMap
          .getForeignKeyColumn();
        if (primaryKeyColumn.getParent().equals(table))
        {
          buffer.append(printColumnAssociation(foreignKey.getName(),
                                               primaryKeyColumn,
                                               foreignKeyColumn));
        }
      }
    }

    out.write(buffer.toString());
  }

  private String escape(final String text)
  {
    return text.replace("\"", "\"\"");
  }

  private String nodeName(final NamedObject namedOjbect)
  {
    if (namedOjbect == null)
    {
      return "";
    }
    else
    {
      return Utility.convertForComparison(namedOjbect.getName()) + "_"
             + Integer.toHexString(namedOjbect.getFullName().hashCode());
    }
  }

  private String printColumnAssociation(final String associationName,
                                        final Column primaryKeyColumn,
                                        final Column foreignKeyColumn)
  {
    final Connectivity connectivity = MetaDataUtility
      .getConnectivity(foreignKeyColumn);
    final String pkSymbol = "teetee";
    final String fkSymbol;
    if (connectivity != null)
    {
      switch (connectivity)
      {
        case OneToOne:
          fkSymbol = "teeodot";
          break;
        case OneToMany:
          fkSymbol = "crowodot";
          break;
        default:
          fkSymbol = "none";
          break;
      }
    }
    else
    {
      fkSymbol = "none";
    }
    final String style;
    if (Utility.isBlank(associationName))
    {
      style = "dashed";
    }
    else
    {
      style = "solid";
    }

    return String
      .format("  \"%s\":\"%s.start\":w -> \"%s\":\"%s.end\":e [label=\"%s\" style=\"%s\" arrowhead=\"%s\" arrowtail=\"%s\"];%n",
              nodeName(primaryKeyColumn.getParent()),
              nodeName(primaryKeyColumn),
              nodeName(foreignKeyColumn.getParent()),
              nodeName(foreignKeyColumn),
              escape(associationName),
              style,
              fkSymbol,
              pkSymbol);
  }

}
