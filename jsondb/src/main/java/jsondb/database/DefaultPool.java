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

package jsondb.database;


import jsondb.Config;
import org.json.JSONArray;
import org.json.JSONObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Level;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;


public class DefaultPool implements JsonDBPool
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
   private static final String USESEC = "use-secondary";
   private static final String CLASSES = "classes";
   private static final String LATENCY = "replication-latency";
   private static final String PRIMARY = "primary";
   private static final String VALIDATE = "validate";
   private static final String USERNAME = "username";
   private static final String PASSWORD = "password";
   private static final String SECONDARY = "secondary";

   private final int latency;
   private final String token;
   private final String defusr;
   private final boolean proxy;
   private final DatabaseType type;

   private final DataSource primary;
   private final DataSource secondary;


   public DefaultPool(JSONObject def) throws Exception
   {
      PoolProperties prm = new PoolProperties();
      PoolProperties sec = new PoolProperties();

      JSONObject prmdef = def.getJSONObject(PRIMARY);
      JSONObject secdef = def.getJSONObject(SECONDARY);

      int latency = def.getInt(LATENCY);
      String type = def.getString(TYPE);
      String token = def.getString(TOKEN);
      String defusr = def.getString(USER);
      boolean proxy = def.getBoolean(PROXY);
      boolean secondary = def.getBoolean(USESEC);

      String sql = Config.get(def,QUERY);
      String usr = Config.get(def,USERNAME);
      String pwd = Config.get(def,PASSWORD);

      prm.setUsername(usr);
      prm.setPassword(pwd);
      prm.setValidationQuery(sql);

      sec.setUsername(usr);
      sec.setPassword(pwd);
      sec.setValidationQuery(sql);

      getCommonProps(prmdef,prm);
      getCommonProps(secdef,sec);

      JSONArray classes = def.getJSONArray(CLASSES);

      for (int i = 0; i < classes.length(); i++)
         Class.forName(classes.getString(i));

      this.proxy = proxy;
      this.token = token;
      this.defusr = defusr;
      this.latency = latency;
      this.primary = new DataSource(prm);
      this.type = DatabaseType.valueOf(type);

      if (!secondary) this.secondary = null;
      else this.secondary = new DataSource(sec);
   }


   @Override
   public int latency()
   {
      return(latency);
   }


   @Override
   public String passtoken()
   {
      return(token);
   }


   @Override
   public boolean proxyuser()
   {
      return(proxy);
   }


   @Override
   public String defaultuser()
   {
      return(defusr);
   }


   @Override
   public DatabaseType type(boolean write)
   {
      return(type);
   }


   @Override
   public void release(Connection conn) throws Exception
   {
      conn.close();
   }


   @Override
   public Connection reserve(boolean write) throws Exception
   {
      if (write || secondary == null) return(primary.getConnection());
      return(secondary.getConnection());
   }


   @Override
   public boolean authenticate(String username, String password) throws Exception
   {
      try
      {
         String url = secondary.getUrl();
         Connection conn = DriverManager.getConnection(url,username,password);
         conn.close();
      }
      catch (Exception e)
      {
         Config.logger().log(Level.INFO,username,e);
         return(false);
      }

      return(true);
   }


   public PoolProperties getCommonProps(JSONObject def, PoolProperties props)
   {
      String url = Config.get(def,URL);

      int min = Config.get(def,MIN);
      int max = Config.get(def,MAX);

      int wait = Config.get(def,WAIT);
      int cval = Config.get(def,VALIDATE);

      props.setUrl(url);

      props.setMinIdle(min);
      props.setInitialSize(0);
      props.setMaxActive(max);

      props.setMaxWait(wait);
      props.setValidationInterval(cval);

      return(props);
   }
}
