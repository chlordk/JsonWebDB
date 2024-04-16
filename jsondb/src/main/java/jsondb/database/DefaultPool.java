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
import java.sql.Connection;
import org.json.JSONObject;
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
   private static final String CLASSES = "classes";
   private static final String LATENCY = "replication-latency";
   private static final String PRIMARY = "primary";
   private static final String VALIDATE = "validate";
   private static final String USERNAME = "username";
   private static final String PASSWORD = "password";
   private static final String SECONDARY = "secondary";

   private final int latency;
   private final DatabaseType type;

   private final DataSource primary;
   private final DataSource secondary;


   public DefaultPool(JSONObject def)
   {
      if (!def.has(LATENCY)) latency = 0;
      else latency = def.getInt(LATENCY);

      JSONObject prmdef = def.getJSONObject(PRIMARY);
      JSONObject secdef = def.getJSONObject(SECONDARY);

      primary = new DataSource(getProps(prmdef));
      secondary = new DataSource(getProps(secdef));

      type = DatabaseType.valueOf(prmdef.getString(TYPE));
   }


   @Override
   public String passtoken()
   {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'passtoken'");
   }


   @Override
   public boolean proxyuser()
   {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'proxyuser'");
   }


   @Override
   public String defaultuser()
   {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'defaultuser'");
   }


   @Override
   public DatabaseType type() throws Exception
   {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'type'");
   }


   @Override
   public void release(Connection conn) throws Exception
   {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'release'");
   }


   @Override
   public Connection reserve(boolean write) throws Exception
   {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'reserve'");
   }


   @Override
   public boolean authenticate(String username, String password) throws Exception
   {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'authenticate'");
   }


   public PoolProperties getProps(JSONObject def)
   {
      PoolProperties props = new PoolProperties();

      String url = Config.get(def,URL);
      String sql = Config.get(def,QUERY);
      String usr = Config.get(def,USERNAME);
      String pwd = Config.get(def,PASSWORD);

      int min = Config.get(def,MIN);
      int max = Config.get(def,MAX);

      int wait = Config.get(def,WAIT);
      int cval = Config.get(def,VALIDATE);

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
