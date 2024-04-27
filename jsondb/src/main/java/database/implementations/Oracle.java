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

import java.sql.Connection;
import java.sql.Savepoint;
import java.util.Properties;
import database.definitions.AdvancedPool;
import database.definitions.JdbcInterface;
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
      OracleConnection oconn = (OracleConnection) conn;
      props.put(OracleConnection.PROXY_USER_NAME,username);
      oconn.openProxySession(OracleConnection.PROXYTYPE_USER_NAME,props);
   }

   @Override
   public void releaseProxyUser(Connection conn) throws Exception
   {
      OracleConnection oconn = (OracleConnection) conn;
      oconn.close(OracleConnection.PROXY_SESSION);
   }

   @Override
   public void releaseSavePoint(Savepoint savepoint, boolean rollback) throws Exception
   {
      if (savepoint == null) return;
      // Oracle only supports rollback. Savepoints are released when commit/rollback
      if (rollback) super.releaseSavePoint(savepoint,rollback);
   }
}