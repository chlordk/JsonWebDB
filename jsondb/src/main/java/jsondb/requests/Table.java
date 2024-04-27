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

package jsondb.requests;

import utils.Misc;
import jsondb.Config;
import jsondb.Session;
import database.Cursor;
import jsondb.Response;
import database.Column;
import database.SQLPart;
import utils.JSONOObject;
import messages.Messages;
import database.BindValue;
import org.json.JSONArray;
import sources.TableSource;
import java.util.ArrayList;
import org.json.JSONObject;


public class Table
{
   private final String sessid;
   private final String source;
   private final JSONObject definition;

   private static final String SOURCE = "source";
   private static final String SELECT = "select()";
   private static final String COLUMNS = "columns";
   private static final String SESSION = "session";
   private static final String PAGESIZE = "page-size";
   private static final String SAVEPOINT = "savepoint";


   public Table(JSONObject definition) throws Exception
   {
      this.definition = definition;
      source = definition.optString(SOURCE);
      sessid = definition.optString(SESSION);
   }


   public Response describe() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid);
      if (session == null) return(new Response(response));

      TableSource source = Utils.getSource(response,this.source);
      if (source == null) return(new Response(response));

      response.put("success",true);

      if (!source.described())
      {
         // We only need to describe it once
         synchronized(source)
         {
            if (!source.described())
            {
               ArrayList<BindValue> bindvalues = Utils.getBindValues(definition);

               String stmt = "select *";
               SQLPart select = new SQLPart(stmt);
               select.append(source.from(bindvalues));
               select.snippet(select.snippet()+" where 1 = 2");

               Cursor cursor = session.executeQuery(select.snippet(),select.bindValues(),false);
               source.setColumns(cursor.describe());
            }
         }
      }

      return(pack(response,source));
   }


   public Response select() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid);
      if (session == null) return(new Response(response));

      TableSource source = Utils.getSource(response,this.source);
      if (source == null) return(new Response(response));

      if (!source.described())
         this.describe();

      ArrayList<BindValue> bindvalues =
         Utils.getBindValues(definition);

      JSONObject args = definition.getJSONObject(SELECT);
      String[] columns = Misc.getJSONList(args,COLUMNS,String.class);
      String stmt = "select "+getColumnList(columns);

      SQLPart select = new SQLPart(stmt);
      select.append(source.from(bindvalues));

      boolean savepoint = Config.pool().savepoint(false);
      if (args.has(SAVEPOINT)) savepoint = args.getBoolean(SAVEPOINT);

      Cursor cursor = session.executeQuery(select.snippet(),select.bindValues(),savepoint);
      cursor.pagesize(Misc.get(args,PAGESIZE));

      JSONArray rows = new JSONArray();
      ArrayList<Object[]> table = cursor.fetch();

      response.put("success",true);
      response.put("cursor",cursor.name());

      response.put("rows",rows);
      for(Object[] row : table) rows.put(row);

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

   private Response pack(JSONObject response, TableSource source)
   {
      JSONArray rows = new JSONArray();
      ArrayList<Column> columns = source.getColumns();

      for (int i = 0; i < columns.size(); i++)
         rows.put(columns.get(i).toJSONObject());

      response.put("rows",rows);
      return(new Response(response));
   }
}
