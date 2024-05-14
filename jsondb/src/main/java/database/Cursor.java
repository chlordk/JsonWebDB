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
import utils.Dates;
import state.State;
import java.sql.Date;
import jsondb.Config;
import jsondb.Session;
import messages.Messages;
import utils.JSONOObject;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import org.json.JSONArray;
import java.util.ArrayList;
import state.StatePersistency;
import java.util.logging.Level;
import java.sql.ResultSetMetaData;
import state.StatePersistency.CursorInfo;


public class Cursor
{
   private long pos = 0;
   private int excost = 0;
   private int ftccost = 0;
   private int pagesize = 0;
   private boolean eof = false;
   private boolean inuse = false;
   private ResultSet rset = null;
   private Statement stmt = null;
   private ArrayList<Column> columns = null;

   private final String sql;
   private final String guid;
   private final Session session;
   private final ArrayList<BindValue> bindvalues;


   public static Cursor create(Session session, String sql, ArrayList<BindValue> bindvalues, int pagesize) throws Exception
   {
      return(new Cursor(session,sql,bindvalues,pagesize));
   }

   public static Cursor load(Session session, String cursid) throws Exception
   {
      String guid = session.guid();

      CursorInfo info = StatePersistency.getCursor(guid,cursid);
      if (info == null) return(null);

      Config.logger().info(Messages.get("REINSTATE_CURSOR",cursid));

      String sql = info.json.getString("query");

      JSONArray bind = info.json.getJSONArray("bindvalues");
      ArrayList<BindValue> bindvalues = new ArrayList<BindValue>();

      for (int i = 0; i < bind.length(); i++)
         bindvalues.add(BindValue.from(bind.getJSONObject(i)));

      Cursor cursor = new Cursor(cursid,session,sql,bindvalues,info.pgz);
      cursor.pos = info.pos;

      return(cursor);
   }


   private Cursor(Session session, String sql, ArrayList<BindValue> bindvalues, int pagesize) throws Exception
   {
      this.pos = 0;
      this.sql = sql;
      this.excost = 0;
      this.session = session;
      this.pagesize = pagesize;
      this.bindvalues = bindvalues;
      this.guid = pagesize <= 0 ? Guid.generate() : this.save();
   }

   private Cursor(String guid, Session session, String sql, ArrayList<BindValue> bindvalues, int pagesize) throws Exception
   {
      this.pos = 0;
      this.sql = sql;
      this.guid = guid;
      this.excost = 0;
      this.session = session;
      this.pagesize = pagesize;
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

   public String guid()
   {
      return(guid);
   }

   public boolean next()
   {
      return(!eof);
   }

   public boolean inUse()
   {
      return(inuse);
   }

   public void inUse(boolean inuse)
   {
      this.inuse = inuse;
   }

   public Session session()
   {
      return(session);
   }

   public long excost()
   {
      return(excost);
   }

   public void excost(long nano)
   {
      excost += nano/1000000;
   }

   public long ftccost()
   {
      return(ftccost);
   }

   public void ftccost(long nano)
   {
      ftccost += nano/1000000;
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
      long nano = System.nanoTime();
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

      ftccost(System.nanoTime()-nano);
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


   public void close()
   {
      close(true,true);
   }


   public void offline()
   {
      close(false,false);
   }


   public void release()
   {
      close(true,false);
   }

   private void close(boolean delete, boolean remove)
   {
      eof = true;
      inuse = false;

      if (rset != null)
      {
         try {rset.close();} catch (Exception e)
         {Config.logger().log(Level.SEVERE,e.toString(),e);}
      }

      if (stmt != null)
      {
         try {stmt.close();} catch (Exception e)
         {Config.logger().log(Level.SEVERE,e.toString(),e);}
      }

      if (delete)
      {
         try {StatePersistency.removeCursor(session.guid(),guid);}
         catch (Exception e) {Config.logger().log(Level.SEVERE,e.toString(),e);}
      }

      if (remove)
      {
         try {State.removeCursor(this);} catch (Exception e)
         {Config.logger().log(Level.SEVERE,e.toString(),e);}
      }
   }


   private String save() throws Exception
   {
      JSONArray bind = new JSONArray();
      JSONOObject data = new JSONOObject();

      data.put("query",sql);
      data.put("bindvalues",bind);

      if (bindvalues != null)
      {
         for(BindValue bv : bindvalues)
            bind.put(bv.toJSON());
      }

      return(StatePersistency.createCursor(session.guid(),this.pos,this.pagesize,data));
   }


   private void saveState() throws Exception
   {
      StatePersistency.updateCursor(session.guid(),guid,this.pos,this.pagesize,this.excost,this.ftccost);
   }
}