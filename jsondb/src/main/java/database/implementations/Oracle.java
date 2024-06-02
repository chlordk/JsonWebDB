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

package database.implementations;

import java.sql.Savepoint;
import database.BindValue;
import java.sql.ResultSet;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Properties;
import database.JdbcInterface;
import database.definitions.AdvancedPool;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.driver.OracleConnection;


public class Oracle extends JdbcInterface
{
   public Oracle(AdvancedPool pool)
   {
      super(pool);
   }

   @Override
   public void setProxyUser(Connection conn, String username) throws Exception
   {
      Properties props = new Properties();
      OracleConnection oconn = getOracleConnection(conn);
      props.put(OracleConnection.PROXY_USER_NAME,username);
      oconn.openProxySession(OracleConnection.PROXYTYPE_USER_NAME,props);
   }

   @Override
   public void releaseProxyUser(Connection conn) throws Exception
   {
      OracleConnection oconn = getOracleConnection(conn);
      oconn.close(OracleConnection.PROXY_SESSION);
   }

   @Override
   public void releaseSavePoint(Savepoint savepoint, boolean rollback) throws Exception
   {
      if (savepoint == null) return;
      // Oracle only supports rollback. Savepoints are released when commit/rollback
      if (rollback) super.releaseSavePoint(savepoint,rollback);
   }


   @Override
   public ArrayList<Object[]> executeUpdateWithReturnValues(Connection conn, String sql, ArrayList<BindValue> bindvalues, String[] returning) throws Exception
   {
      sql += " returning ";

      for (int i = 0; i < returning.length; i++)
      {
         if (i == 0) sql += returning[i];
         else  sql += "," + returning[i];
      }

      sql += " into ";

      for (int i = 0; i < returning.length; i++)
         sql += i == 0 ? "?" : ",?";

      int stbv = bindvalues.size() - returning.length;
      OracleConnection oconn = getOracleConnection(conn);
      OraclePreparedStatement stmt = (OraclePreparedStatement) oconn.prepareStatement(sql);

      for (int i = 0; i < stbv; i++)
      {
         BindValue bv = bindvalues.get(i);
         if (bv.untyped()) stmt.setObject(i+1,bv.value());
         else stmt.setObject(i+1,bv.value(),bv.type());
      }

      for (int i = 0; i < returning.length; i++)
      {
         int idx = stbv + i;
         BindValue bv = bindvalues.get(idx);
         stmt.registerReturnParameter(idx+1,bv.type());
      }

      stmt.executeUpdate();
      ResultSet rset = stmt.getReturnResultSet();
      int cols = rset.getMetaData().getColumnCount();
      ArrayList<Object[]> data = new ArrayList<Object[]>();

      while(rset.next())
      {
         Object[] row = new Object[cols];

         for (int i = 0; i < row.length; i++)
            row[i] = rset.getObject(i+1);

         data.add(row);
      }

      return(data);
   }


   public OracleConnection getOracleConnection(Connection conn) throws Exception
   {
      OracleConnection oconn = null;

      if (!conn.isWrapperFor(OracleConnection.class))
      {
         oconn = (OracleConnection) conn;
      }
      else
      {
         oconn = conn.unwrap(OracleConnection.class);
      }

      return(oconn);
   }
}