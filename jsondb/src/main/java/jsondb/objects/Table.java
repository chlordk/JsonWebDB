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

package jsondb.objects;

import jsondb.Session;
import jsondb.Response;
import database.Cursor;
import database.SQLPart;
import utils.JSONOObject;
import messages.Messages;
import database.BindValue;
import java.util.ArrayList;
import org.json.JSONObject;
import jsondb.sources.Sources;
import jsondb.sources.TableSource;


public class Table
{
   private final String sessid;
   private final String source;
   private final JSONObject definition;

   private static final String SOURCE = "source";
   private static final String SELECT = "select()";
   private static final String COLUMNS = "columns";
   private static final String SESSION = "session";


   public Table(JSONObject definition) throws Exception
   {
      this.definition = definition;
      source = definition.optString(SOURCE);
      sessid = definition.optString(SESSION);
   }


   public Response select() throws Exception
   {
      Session session = Session.get(sessid);
      JSONObject response = new JSONOObject();
      TableSource source = Sources.get(this.source);
      ArrayList<BindValue> bindvalues = Utils.getBindValues(definition);

      if (session == null)
      {
         response.put("success",false);
         response.put("message",Messages.get("NO_SUCH_SESSION",sessid));
         return(new Response(response));
      }

      if (source == null)
      {
         response.put("success",false);
         response.put("message",Messages.get("UNKNOWN_SOURCE",this.source));
         return(new Response(response));
      }

      JSONObject args = definition.getJSONObject(SELECT);
      String[] columns = Utils.getJSONList(args,COLUMNS,String.class);

      String stmt = "select "+getColumnList(columns);

      SQLPart select = new SQLPart(stmt);
      select.append(source.from(bindvalues));

      Cursor cursor = session.executeQuery(select.snippet(),select.bindValues());

      return(new Response(response));
   }


   private String getColumnList(String[] columns) throws Exception
   {

      if (columns == null)
         throw new Exception(Messages.get("MISSING_COLUNM_SPEC"));

      for (int i = 0; i < columns.length; i++)
      {
         columns[i] = columns[i].trim();
         if (columns[i].indexOf('(') >= 0)
            throw new Exception(Messages.get("BAD_COLUNM_SPEC"));
      }

      String list = "";

      for (int i = 0; i < columns.length; i++)
      {
         if (i > 0) list += ",";
         list += columns[i];
      }

      return(list);
   }
}
