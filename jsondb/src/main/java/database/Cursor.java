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
import utils.Bytes;
import utils.Dates;
import java.sql.Date;
import jsondb.Session;
import utils.JSONOObject;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import org.json.JSONArray;
import state.StateHandler;
import org.json.JSONObject;
import java.util.ArrayList;
import java.sql.ResultSetMetaData;


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
   private final Session session;
   private final ArrayList<BindValue> bindvalues;


   public static Cursor reload(Session session, String cursid) throws Exception
   {
      String guid = session.getGuid();

      byte[] bytes = StateHandler.getCursor(guid,cursid);
      if (bytes == null) return(null);

      String str = new String(bytes,12,bytes.length-12);
      JSONObject json = new JSONObject(str);

      String sql = json.getString("query");

      JSONArray bind = json.getJSONArray("bindvalues");
      ArrayList<BindValue> bindvalues = new ArrayList<BindValue>();

      for (int i = 0; i < bind.length(); i++)
         bindvalues.add(BindValue.from(bind.getJSONObject(i)));

      int pgsz = Bytes.getInt(bytes,8);
      long pos = Bytes.getLong(bytes,0);

      Cursor cursor = new Cursor(cursid,session,sql,bindvalues);

      cursor.pos = pos;
      cursor.pagesize = pgsz;

      return(cursor);
   }


   public Cursor(Session session, String sql, ArrayList<BindValue> bindvalues) throws Exception
   {
      this(null,session,sql,bindvalues);
   }


   private Cursor(String name, Session session, String sql, ArrayList<BindValue> bindvalues) throws Exception
   {
      boolean save = false;

      if (name == null)
      {
         save = true;
         name = Guid.generate();
      }

      this.pos = 0;
      this.sql = sql;
      this.name = name;
      this.session = session;
      this.bindvalues = bindvalues;

      if (save) this.save();
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

   public ArrayList<Column> describe() throws Exception
   {
      if (columns != null) return(columns);
      ResultSetMetaData meta = rset.getMetaData();

      int cols = meta.getColumnCount();
      columns = new ArrayList<Column>();

      for (int i = 0; i < cols; i++)
      {
         int sqltype = meta.getColumnType(i+1);

         String name = meta.getColumnName(i+1);
         String type = meta.getColumnTypeName(i+1);

         int scale = meta.getScale(i+1);
         int precs = meta.getPrecision(i+1);

         columns.add(new Column(name,type,sqltype,precs,scale));
      }

      return(columns);
   }

   public synchronized ArrayList<Object[]> fetch() throws Exception
   {
      columns = describe();
      int cols = columns.size();
      ArrayList<Object[]> rows = new ArrayList<Object[]>();

      for (int i = 0; i < pagesize || pagesize <= 0; i++)
      {
         if (!rset.next())
          {close();break;}

         this.pos++;
         Object[] row = new Object[cols];

         for (int c = 0; c < cols; c++)
         {
            Object value = rset.getObject(c+1);

            if (columns.get(c).isDateType())
            {
               if (value instanceof Date)
                  value = Dates.convert((Date) value);

               else

               if (value instanceof Timestamp)
                  value = Dates.convert((Timestamp) value);
            }

            row[c] = value;
         }

         rows.add(row);
      }

      if (!eof) saveState();
      return(rows);
   }

   public synchronized void position() throws Exception
   {
      for (int i = 0; i < this.pos; i++)
      {
         if (!rset.next())
          {close();break;}
      }
   }

   public void close() throws Exception
   {
      eof = true;
      if (rset != null) rset.close();
      if (stmt != null) stmt.close();

      String guid = session.getGuid();

      session.removeCursor(this);
      StateHandler.removeCursor(guid,name);
   }

   public void loadState() throws Exception
   {
      String guid = session.getGuid();
      byte[] header = StateHandler.peekCursor(guid,name,12);
      this.pos = Bytes.getLong(header,0); this.pagesize = Bytes.getInt(header,8);
   }

   private void save() throws Exception
   {
      String guid = session.getGuid();

      JSONArray bind = new JSONArray();
      JSONOObject data = new JSONOObject();

      data.put("query",sql);
      data.put("bindvalues",bind);

      if (bindvalues != null)
      {
         for(BindValue bv : bindvalues)
            bind.put(bv.toJSON());
      }

      byte[] def = data.toString(0).getBytes();
      byte[] bytes = new byte[4 + 8 + def.length];

      byte[] pos = Bytes.getBytes(this.pos);
      byte[] psz = Bytes.getBytes(this.pagesize);

      System.arraycopy(pos,0,bytes,0,pos.length);
      System.arraycopy(psz,0,bytes,8,psz.length);
      System.arraycopy(def,0,bytes,12,def.length);

      StateHandler.createCursor(guid,name,bytes);
   }

   private void saveState() throws Exception
   {
      String guid = session.getGuid();
      byte[] pos = Bytes.getBytes(this.pos);
      byte[] psz = Bytes.getBytes(this.pagesize);
      StateHandler.updateCursor(guid,name,pos,psz);
   }
}