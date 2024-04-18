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

import java.sql.Connection;

import jsondb.Config;


public abstract class JdbcInterface
{
   protected final AdvancedPool pool;

   public static JdbcInterface getInstance(boolean write) throws Exception
   {
      return(Config.pool().type(write).getInstance());
   }

   public JdbcInterface(AdvancedPool pool)
   {
      this.pool = pool;
   }

   public int latency()
   {
      return(pool.latency());
   }

   public String defaultuser()
   {
      return(pool.defaultuser());
   }

   public boolean proxy()
   {
      return(pool.proxy());
   }

   public void release(Connection conn) throws Exception
   {
      pool.release(conn);
   }

   public Connection reserve(boolean write) throws Exception
   {
      return(pool.reserve(write));
   }

   public boolean authenticate(String username, String password) throws Exception
   {
      if (username == null) return(false);
      if (password == null) return(false);
      return(pool.authenticate(username,password));
   }
}
