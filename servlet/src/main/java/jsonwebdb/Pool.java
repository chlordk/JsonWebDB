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

package jsonwebdb;

import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.DriverManager;
import jsonwebdb.JsonWebDB.AnyException;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

public class Pool
{
   private final String url;
   private final DataSource ds;

   public Pool() throws AnyException
   {
      try
      {
         Class.forName("org.postgresql.Driver");
         Class.forName("oracle.jdbc.driver.OracleDriver");
      }
      catch (Exception e)
      {
         throw new AnyException(e);
      }

      url = "jdbc:postgresql://host.docker.internal:5432/hr?ssl=false&ApplicationName=JsonWebDB";

      PoolProperties props = new PoolProperties();

      props.setUrl(url);

      props.setUsername("hr");
      props.setPassword("hr");

      props.setValidationInterval(30000);
      props.setValidationQuery("select user");

      props.setMinIdle(10);
      props.setInitialSize(0);
      props.setMaxActive(100);
      props.setMaxWait(10000);

      this.ds = new DataSource();
      this.ds.setPoolProperties(props);
   }

   public DataSource datasource()
   {
      return(ds);
   }

   public Connection getConnection() throws Exception
   {
      return(ds.getConnection());
   }

   public String authenticate(String username, String password) throws Exception
   {
      String user = null;

      Connection conn = DriverManager.getConnection(url,username,password);
      ResultSet  rset = conn.createStatement().executeQuery("select user");
      if (rset.next()) user = rset.getString(1);

      rset.close();
      conn.close();

      conn = getConnection();
      rset = conn.createStatement().executeQuery("select user");
      if (rset.next()) user += " "+rset.getString(1);

      return(user);
   }
}
