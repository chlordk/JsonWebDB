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

import jsondb.Config;
import java.sql.ResultSet;
import org.json.JSONArray;
import org.json.JSONObject;
import java.sql.Connection;
import jsondb.database.Type;
import java.sql.DriverManager;
import jsonwebdb.JsonWebDB.AnyException;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;


public class Pool implements jsondb.database.Pool
{
   private static final String URL = "jdbc-url";
   private static final String MIN = "min";
   private static final String MAX = "max";
   private static final String TYPE = "type";
   private static final String WAIT = "max-wait";
   private static final String QUERY = "test";
   private static final String CLASSES = "classes";
   private static final String VALIDATE = "validate";
   private static final String USERNAME = "username";
   private static final String PASSWORD = "password";
   private static final String DATABASE = "database";

   private final Type type;
   private final String url;
   private final DataSource ds;

   public Pool(Config config) throws AnyException
   {
      JSONObject def = config.get(DATABASE);
      this.type = Type.valueOf(config.get(def,TYPE));

      String url = config.get(def,URL);
      String sql = config.get(def,QUERY);
      String usr = config.get(def,USERNAME);
      String pwd = config.get(def,PASSWORD);

      int min = config.get(def,MIN);
      int max = config.get(def,MAX);

      int wait = config.get(def,WAIT);
      int cval = config.get(def,VALIDATE);

      try
      {
         JSONArray cls = config.get(def,CLASSES);

         for (int i = 0; i < cls.length(); i++)
            Class.forName(cls.getString(i));
      }
      catch (Exception e)
      {
         throw new AnyException(e);
      }

      PoolProperties props = new PoolProperties();

      props.setUrl(url);

      props.setUsername(usr);
      props.setPassword(pwd);

      props.setValidationQuery(sql);
      props.setValidationInterval(cval);

      props.setMinIdle(min);
      props.setInitialSize(0);
      props.setMaxActive(max);
      props.setMaxWait(wait);

      this.url = url;
      this.ds = new DataSource();
      this.ds.setPoolProperties(props);
   }

   @Override
   public Type type()
   {
      return(type);
   }

   @Override
   public void release(Connection conn) throws Exception
   {
      conn.close();
   }

   @Override
   public Connection getConnection() throws Exception
   {
      return(ds.getConnection());
   }

   @Override
   public boolean authenticate(String username, String password) throws Exception
   {
      String user = null;

      Connection conn = DriverManager.getConnection(url,username,password);
      ResultSet  rset = conn.createStatement().executeQuery("select user");

      if (rset.next()) user = rset.getString(1);
      rset.close(); conn.close();

      if (user == null)
         return(false);

      return(username.equalsIgnoreCase(user));
   }
}
