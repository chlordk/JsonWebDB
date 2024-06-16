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
package sources;

import org.json.JSONObject;

import java.util.HashMap;
import database.DataType;
import static utils.Misc.*;


public class SQLSource implements Source
{
   private static final String ID = "id";
   private static final String SQL = "sql";
   private static final String UPD = "update";

   public final String sid;
   public final String sql;
   public final Boolean upd;
   public final HashMap<String,DataType> types;


   public SQLSource(JSONObject definition) throws Exception
   {
      upd = get(definition,UPD,false);
      sid = getString(definition,ID,true,true);
      sql = getString(definition,SQL,true,false);

      types = DataType.parse(definition);
   }

   public String id()
   {
      return(sid);
   }
}
