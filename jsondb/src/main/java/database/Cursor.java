/*
MIT License

Copyright (c) 2024 Alex Høffner

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package database;

import utils.Guid;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.sql.ResultSetMetaData;
import database.definitions.SQLTypes;


public class Cursor
{
   private long pos = 0;
   private int pagesize = 0;
   private boolean eof = false;
   private ResultSet rset = null;
   private Statement stmt = null;
   private ArrayList<Column> columns = null;

   private final String sql;
   private final String name;
   private final ArrayList<BindValue> bindvalues;


   public Cursor(String sql, ArrayList<BindValue> bindvalues)
   {
      this.pos = 0;
      this.sql = sql;
      this.name = Guid.generate();
      this.bindvalues = bindvalues;
   }

   public long pos()
   {
      return(pos);
   }

   public String sql()
   {
      return(sql);
   }

   public String name()
   {
      return(name);
   }

   public boolean next()
   {
      return(!eof);
   }

   public Cursor pagesize(Integer pagesize)
   {
      if (pagesize == null) pagesize = 0;
      this.pagesize = pagesize;
      return(this);
   }

   public ArrayList<BindValue> bindvalues()
   {
      return(bindvalues);
   }

   public Cursor statement(Statement stmt)
   {
      this.stmt = stmt;
      return(this);
   }

   public Cursor resultset(ResultSet rset)
   {
      this.eof = false;
      this.rset = rset;
      return(this);
   }

   public synchronized ArrayList<Object[]> fetch() throws Exception
   {
      ResultSetMetaData meta = rset.getMetaData();
      int cols = meta.getColumnCount();

      if (columns == null)
      {
         columns = new ArrayList<Column>();

         for (int i = 0; i < cols; i++)
         {
            int sqltype = meta.getColumnType(i+1);
            String name = meta.getColumnName(i+1);
            String type = meta.getColumnTypeName(i+1);

            columns.add(new Column(name,type,sqltype));
         }
      }

      ArrayList<Object[]> rows = new ArrayList<Object[]>();

      for (int i = 0; i < pagesize || pagesize <= 0; i++)
      {
         if (!rset.next())
          {close();break;}

         Object[] row = new Object[cols];

         for (int c = 0; c < cols; c++)
         {
            Object value = rset.getObject(c+1);

            if (columns.get(c).isDateType())
            {
               if (value instanceof Date)
                  value = ((Date) value).getTime();

               else

               if (value instanceof Timestamp)
                  value = ((Timestamp) value).getTime();
            }

            row[c] = value;
         }

         rows.add(row);
      }

      return(rows);
   }

   public void close() throws Exception
   {
      eof = true;
      if (rset != null) rset.close();
      if (stmt != null) stmt.close();
   }


   private static class Column
   {
      final int sqltype;
      final String type;
      final String name;

      Column(String name, String type, int sqltype)
      {
         this.type = type;
         this.name = name;
         this.sqltype = sqltype;
      }

      boolean isDateType()
      {
         return(SQLTypes.isDateType(sqltype));
      }

      public String toString()
      {
         return(name+" "+type+"["+sqltype+"]");
      }
   }
}
