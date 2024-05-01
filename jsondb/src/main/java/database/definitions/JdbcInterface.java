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

package database.definitions;

import jsondb.Config;
import database.Cursor;
import messages.Messages;
import java.sql.Statement;
import database.BindValue;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;


public abstract class JdbcInterface
{
   private Connection conn = null;
   private final AdvancedPool pool;


   public static JdbcInterface getInstance(boolean write) throws Exception
   {
      return(Config.pool().type(write).getInstance());
   }

   public JdbcInterface(AdvancedPool pool)
   {
      this.pool = pool;
   }

   public boolean isConnected()
   {
      return(conn != null);
   }

   public JdbcInterface connect(String username, boolean write, boolean dedicated) throws Exception
   {
      this.conn = pool.getConnection(write);
      if (pool.proxy()) setProxyUser(conn,username);
      conn.setAutoCommit(!dedicated);
      return(this);
   }

   public boolean disconnect() throws Exception
   {
      if (conn == null) return(false);
      if (!conn.getAutoCommit()) conn.rollback();

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


   public boolean execute(String sql, boolean savepoint) throws Exception
   {
      Savepoint sp = null;

      if (conn.getAutoCommit())
         savepoint = false;

      Statement stmt = conn.createStatement();

      if (savepoint)
      {
         try
         {
            synchronized(conn)
            {
               sp = conn.setSavepoint();
               boolean success = stmt.execute(sql);
               releaseSavePoint(sp,false);
               return(success);
            }
         }
         catch (Exception e)
         {
            releaseSavePoint(sp,true);
            throw new Exception(e);
         }
      }

      return(stmt.execute(sql));
   }


   public int executeUpdate(String sql, boolean savepoint) throws Exception
   {
      Savepoint sp = null;

      if (conn.getAutoCommit())
         savepoint = false;

      Statement stmt = conn.createStatement();

      if (savepoint)
      {
         try
         {
            synchronized(conn)
            {
               sp = conn.setSavepoint();
               int affected = stmt.executeUpdate(sql);
               releaseSavePoint(sp,false);
               return(affected);
            }
         }
         catch (Exception e)
         {
            Config.logger().severe(sql);
            releaseSavePoint(sp,true);
            throw new Exception(e);
         }
      }
      else
      {
         try
         {
            int affected = stmt.executeUpdate(sql);
            return(affected);
         }
         catch (Exception e)
         {
            Config.logger().severe(sql);
            throw new Exception(e);
         }
      }
   }


   public void executeQuery(Cursor cursor, boolean savepoint)  throws Exception
   {
      Savepoint sp = null;

      if (conn.getAutoCommit())
         savepoint = false;

      if (conn == null)
         throw new Exception(Messages.get("NOT_CONNECTED"));

      String logentry = "\n\n"+cursor.sql();

      if (cursor.bindvalues().size() > 0)
         logentry += "\n\nbindings:";

      for (int i = 0; i < cursor.bindvalues().size(); i++)
         logentry += "\n"+cursor.bindvalues().get(i).toString()+"\n";

      Config.logger().fine(logentry);

      PreparedStatement stmt = conn.prepareStatement(cursor.sql());

      for (int i = 0; i < cursor.bindvalues().size(); i++)
      {
         BindValue bv = cursor.bindvalues().get(i);
         if (bv.untyped()) stmt.setObject(i+1,bv.value());
         else stmt.setObject(i+1,bv.value(),bv.type());
      }

      if (savepoint)
      {
         try
         {
            synchronized(conn)
            {
               sp = conn.setSavepoint();
               cursor.resultset(stmt.executeQuery());
               releaseSavePoint(sp,false);
            }
         }
         catch (Exception e)
         {
            Config.logger().severe(logentry);
            releaseSavePoint(sp,true);
            throw new Exception(e);
         }
      }
      else
      {
         try
         {
            cursor.resultset(stmt.executeQuery());
         }
         catch (Exception e)
         {
            Config.logger().severe(cursor.sql());
            throw new Exception(e);
         }
      }
   }

   public abstract void releaseProxyUser(Connection conn) throws Exception;
   public abstract void setProxyUser(Connection conn, String username) throws Exception;
}