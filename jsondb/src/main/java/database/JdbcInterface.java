/*
  MIT License

  Copyright © 2023 Alex Høffner

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the “Software”), to deal in the Software without
  restriction, including without limitation the rights to use, copy, modify, merge, publish,
  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
  Software is furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies or
  substantial portions of the Software.

  THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package database;

import utils.Dates;
import jsondb.Config;
import java.util.HashMap;
import java.sql.Statement;
import java.sql.Savepoint;
import java.util.ArrayList;
import utils.NameValuePair;
import java.sql.Connection;
import java.util.Properties;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import database.definitions.AdvancedPool;


public abstract class JdbcInterface
{
   private Connection conn = null;
   private final AdvancedPool pool;


   public static JdbcInterface getInstance(boolean write) throws Exception
   {
      return(Config.dbconfig().getInstance(write));
   }

   public JdbcInterface(AdvancedPool pool)
   {
      this.pool = pool;
   }

   public boolean isConnected()
   {
      return(conn != null);
   }

   public JdbcInterface connect(String username, boolean write, boolean stateful) throws Exception
   {
      this.conn = pool.getConnection(write);

      if (Config.dbconfig().useproxy())
         setProxyUser(conn,username);

      conn.setAutoCommit(!stateful);
      return(this);
   }

   public boolean disconnect() throws Exception
   {
      if (conn == null) return(false);
      if (!conn.getAutoCommit()) conn.rollback();

      if (Config.dbconfig().useproxy())
         releaseProxyUser(conn);

      pool.freeConnection(conn);

      conn = null;
      return(true);
   }

   public boolean authenticate(String username, String password) throws Exception
   {
      if (username == null) return(false);
      if (password == null) return(false);
      return(pool.authenticate(username,password));
   }

   public JdbcInterface commit() throws Exception
   {
      if (conn != null) conn.commit();
      return(this);
   }

   public JdbcInterface rollback() throws Exception
   {
      if (conn != null) conn.rollback();
      return(this);
   }

   public void releaseSavePoint(Savepoint savepoint, boolean rollback) throws Exception
   {
      if (savepoint == null) return;
      if (rollback) conn.rollback(savepoint);
      else conn.releaseSavepoint(savepoint);
   }

  public void setClientInfo(HashMap<String,BindValue> clientinfo) throws Exception
  {
    if (clientinfo != null)
    {
      Properties props = new Properties();

      for(BindValue bv : clientinfo.values())
        props.setProperty(bv.name(),bv.value()+"");

      conn.setClientInfo(props);
    }
  }

  public void clearClientInfo(HashMap<String,BindValue> clientinfo) throws Exception
  {
    if (clientinfo != null)
      conn.setClientInfo(new Properties());
  }


   public boolean execute(String sql, boolean savepoint) throws Exception
   {
      Savepoint sp = null;

      if (conn.getAutoCommit())
         savepoint = false;

      Statement stmt = conn.createStatement();
      Config.logger().severe(logentry(sql));

      try
      {
         if (savepoint)
            sp = conn.setSavepoint();

         boolean success = stmt.execute(sql);

         if (savepoint)
            releaseSavePoint(sp,false);

         return(success);
      }
      catch (Exception e)
      {
         if (savepoint)
            releaseSavePoint(sp,true);

         return(false);
      }
   }


   public ArrayList<NameValuePair<Object>> executeCall(String sql, ArrayList<BindValue> bindvalues, boolean savepoint) throws Exception
   {
      int returns = 0;
      Savepoint sp = null;

      if (conn.getAutoCommit())
         savepoint = false;

      ArrayList<NameValuePair<Object>> results =
         new ArrayList<NameValuePair<Object>>();

      Config.logger().severe(logentry(sql,bindvalues));

      try
      {
         if (savepoint)
            sp = conn.setSavepoint();

         CallableStatement stmt = conn.prepareCall(sql);

         for (int i = 0; i < bindvalues.size(); i++)
         {
            Integer type = 0;
            BindValue bv = bindvalues.get(i);

            if (!bv.untyped()) type = bv.type();
            else type = SQLTypes.guessType(bv.value());

            stmt.setObject(i+1,bv.value(),type);

            if (bv.out())
            {
               returns++;
               stmt.registerOutParameter(i+1,type);
            }
         }

         stmt.execute();

         for (int i = 0; returns > 0 && i < bindvalues.size(); i++)
         {
            Integer type = 0;
            BindValue bv = bindvalues.get(i);

            if (bv.out())
            {
               String name = bv.name();
               Object value = stmt.getObject(i+1);

               if (!bv.untyped()) type = bv.type();
               else type = SQLTypes.guessType(value);

               if (SQLTypes.isDateType(type))
                  value = Dates.toString(value);

               results.add(new NameValuePair<Object>(name,value));
            }
         }

         if (savepoint)
            releaseSavePoint(sp,false);

         return(results);
      }
      catch (Throwable t)
      {
         Config.logger().severe(logentry(sql,bindvalues,t));

         if (savepoint)
            releaseSavePoint(sp,true);

         throw new Exception(t);
      }
   }


   public UpdateResponse executeUpdate(String sql, ArrayList<BindValue> bindvalues, String[] returning, boolean savepoint) throws Exception
   {
      Savepoint sp = null;

      if (conn.getAutoCommit())
         savepoint = false;

      Config.logger().severe(logentry(sql,bindvalues));

      if (returning != null && returning.length > 0)
      {
         try
         {
            if (savepoint)
               sp = conn.setSavepoint();

            ArrayList<Object[]> data = executeUpdateWithReturnValues(conn,sql,bindvalues,returning);

            if (savepoint)
               releaseSavePoint(sp,false);

            return(new UpdateResponse(data));
         }
         catch (Throwable t)
         {
            Config.logger().severe(logentry(sql,bindvalues,t));

            if (savepoint)
               releaseSavePoint(sp,true);

            throw new Exception(t);
         }
      }

      PreparedStatement stmt = conn.prepareStatement(sql);

      for (int i = 0; i < bindvalues.size(); i++)
      {
         BindValue bv = bindvalues.get(i);
         if (bv.untyped()) stmt.setObject(i+1,bv.value());
         else stmt.setObject(i+1,bv.value(),bv.type());
      }

      try
      {
         synchronized(conn)
         {
            if (savepoint)
               sp = conn.setSavepoint();

            int affected = stmt.executeUpdate();

            if (savepoint)
               releaseSavePoint(sp,false);

            return(new UpdateResponse(affected));
         }
      }
      catch (Throwable t)
      {
         Config.logger().severe(logentry(sql,bindvalues,t));

         if (savepoint)
            releaseSavePoint(sp,true);

         throw new Exception(t);
      }
   }


   public void executeQuery(Cursor cursor, boolean savepoint)  throws Exception
   {
      Savepoint sp = null;

      if (conn.getAutoCommit())
         savepoint = false;

      Config.logger().info(logentry(cursor));
      PreparedStatement stmt = conn.prepareStatement(cursor.sql());

      for (int i = 0; i < cursor.bindvalues().size(); i++)
      {
         BindValue bv = cursor.bindvalues().get(i);
         if (bv.untyped()) stmt.setObject(i+1,bv.value());
         else stmt.setObject(i+1,bv.value(),bv.type());
      }

      try
      {
         synchronized(conn)
         {
            if (savepoint)
               sp = conn.setSavepoint();

            cursor.resultset(stmt.executeQuery());

            if (savepoint)
               releaseSavePoint(sp,false);
         }
      }
      catch (Throwable t)
      {
         Config.logger().severe(logentry(cursor,t));

         if (savepoint)
            releaseSavePoint(sp,true);

         throw new Exception(t);
      }
   }


   private String logentry(String sql)
   {
      return(logentry(sql,null));
   }


   private String logentry(Cursor cursor)
   {
      return(logentry(cursor,null));
   }


   private String logentry(Cursor cursor, Throwable t)
   {
      return(logentry(cursor.sql(),cursor.bindvalues(),t));
   }


   private String logentry(String sql, ArrayList<BindValue> bindvalues)
   {
      return(logentry(sql,bindvalues,null));
   }


   private String logentry(String sql, ArrayList<BindValue> bindvalues, Throwable t)
   {
      String del = "\n---------------------------------------------------------------------\n";

      String logentry = del;
      logentry += sql;

      if (bindvalues != null)
      {
         if (bindvalues.size() > 0)
            logentry += "\n\nbindings:";

         for (int i = 0; i < bindvalues.size(); i++)
            logentry += "\n"+bindvalues.get(i).desc();
      }

      if (t != null)
         logentry += "\n\n" + t.toString() + "\n";

      logentry += del;

      return(logentry);
   }


   public static class UpdateResponse
   {
      public final int affected;
      public final ArrayList<Object[]> returning;

      UpdateResponse(int affected)
      {
         this.returning = null;
         this.affected = affected;
      }

      UpdateResponse(ArrayList<Object[]> returning)
      {
         this.returning = returning;
         this.affected = returning.size();
      }
   }

   public abstract void releaseProxyUser(Connection conn) throws Exception;
   public abstract void setProxyUser(Connection conn, String username) throws Exception;
   public abstract ArrayList<Object[]> executeUpdateWithReturnValues(Connection conn, String sql, ArrayList<BindValue> bindvalues, String[] returning) throws Exception;
}