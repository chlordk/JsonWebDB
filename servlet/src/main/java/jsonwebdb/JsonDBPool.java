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
import java.sql.DriverManager;
import jsondb.database.DatabaseType;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;


public class JsonDBPool implements jsondb.database.JsonDBPool
{
   private static final String URL = "jdbc-url";
   private static final String MIN = "min";
   private static final String MAX = "max";
   private static final String TYPE = "type";
   private static final String WAIT = "max-wait";
   private static final String USER = "defaultuser";
   private static final String QUERY = "test";
   private static final String TOKEN = "password-token";
   private static final String PROXY = "proxyuser";
   private static final String CLASSES = "classes";
   private static final String PRIMARY = "primary";
   private static final String VALIDATE = "validate";
   private static final String USERNAME = "username";
   private static final String PASSWORD = "password";
   private static final String DATABASE = "database";
   private static final String SECONDARY = "secondary";

   private final String rurl;
   private final String user;
   private final String token;
   private final boolean proxy;
   private final DataSource read;
   private final DataSource write;
   private final DatabaseType type;


   public JsonDBPool(Config config) throws Exception
   {
      JSONObject def = config.get(DATABASE);
      this.type = DatabaseType.valueOf(config.get(def,TYPE));

      JSONArray cls = config.get(def,CLASSES);

      for (int i = 0; i < cls.length(); i++)
         Class.forName(cls.getString(i));

      PoolProperties prim = getProps(config,def,PRIMARY);
      PoolProperties secd = getProps(config,def,SECONDARY);

      this.read = new DataSource();
      this.read.setPoolProperties(prim);

      this.write = new DataSource();
      this.write.setPoolProperties(secd);

      this.user = def.getString(USER);
      this.token = def.getString(TOKEN);
      this.proxy = def.getBoolean(PROXY);

      this.rurl = config.get(def,SECONDARY+"-"+URL);
   }

   @Override
   public DatabaseType type()
   {
      return(type);
   }

   @Override
   public boolean proxyuser()
   {
      return(proxy);
   }

   @Override
   public String defaultuser()
   {
      return(user);
   }

   @Override
   public String passtoken()
   {
      return(token);
   }

   @Override
   public Connection reserve(boolean write) throws Exception
   {
      if (!write)
      return(this.read.getConnection());
      return(this.write.getConnection());
   }

   @Override
   public void release(Connection conn) throws Exception
   {
      conn.close();
   }

   @Override
   public boolean authenticate(String username, String password) throws Exception
   {
      String user = null;

      Connection conn = DriverManager.getConnection(rurl,username,password);
      ResultSet  rset = conn.createStatement().executeQuery("select user");

      if (rset.next()) user = rset.getString(1);
      rset.close(); conn.close();

      if (user == null)
         return(false);

      return(username.equalsIgnoreCase(user));
   }


   public PoolProperties getProps(Config config, JSONObject def, String type)
   {
      PoolProperties props = new PoolProperties();

      String sql = config.get(def,QUERY);
      String usr = config.get(def,USERNAME);
      String pwd = config.get(def,PASSWORD);
      String url = config.get(def,type+"-"+URL);

      JSONObject sub = def.getJSONObject(type);

      int min = config.get(sub,MIN);
      int max = config.get(sub,MAX);

      int wait = config.get(def,WAIT);
      int cval = config.get(def,VALIDATE);

      props.setUrl(url);

      props.setUsername(usr);
      props.setPassword(pwd);

      props.setValidationQuery(sql);
      props.setValidationInterval(cval);

      props.setMinIdle(min);
      props.setInitialSize(0);
      props.setMaxActive(max);
      props.setMaxWait(wait);

      return(props);
   }
}
