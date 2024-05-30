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

import database.BindValue;
import java.sql.Connection;
import java.util.ArrayList;
import database.JdbcInterface;
import database.definitions.AdvancedPool;


public class Generic extends JdbcInterface
{
   public Generic(AdvancedPool pool)
   {
      super(pool);
   }

   @Override
   public void setProxyUser(Connection conn, String username) throws Exception
   {
      throw new Exception("Not implemented");
   }

   @Override
   public void releaseProxyUser(Connection conn) throws Exception
   {
      throw new Exception("Not implemented");
   }

   @Override
   public void executeUpdateWithReturnValues(Connection conn, String sql, ArrayList<BindValue> bindvalues, String[] returning) throws Exception
   {
      throw new UnsupportedOperationException("Unimplemented method 'executeUpdateWithReturnValues'");
   }
 }