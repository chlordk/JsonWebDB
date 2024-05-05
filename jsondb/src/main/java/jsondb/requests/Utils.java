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

import sources.Source;
import jsondb.Session;
import sources.Sources;
import database.Cursor;
import java.util.HashMap;
import messages.Messages;
import database.BindValue;
import org.json.JSONArray;
import org.json.JSONObject;


public class Utils
{
   private static final String NAME = "name";
   private static final String VALUE = "value";
   private static final String SECTION = "bindvalues";



   public static String getColumnList(String[] columns) throws Exception
   {
      if (columns == null)
         throw new Exception(Messages.get("MISSING_COLUNM_SPEC"));

      for (int i = 0; i < columns.length; i++)
      {
         columns[i] = columns[i].trim();

         if (columns[i].indexOf('(') >= 0)
            throw new Exception(Messages.get("BAD_COLUNM_SPEC"));

         if (columns[i].indexOf('&') >= 0)
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


   public static HashMap<String,BindValue> getBindValues(JSONObject def)
   {
      if (!def.has(SECTION))
         return(null);

      HashMap<String,BindValue> bindvalues =
         new HashMap<String,BindValue>();

      JSONArray arr = def.getJSONArray(SECTION);

      for (int i = 0; i < arr.length(); i++)
      {
         JSONObject bdef = arr.getJSONObject(i);

         String name = bdef.getString(NAME);
         Object value = bdef.getString(VALUE);

         bindvalues.put(name.toLowerCase(),new BindValue(name).value(value));
      }

      return(bindvalues);
   }


   public static Session getSession(JSONObject response, String sessid) throws Exception
   {
      Session session = Session.get(sessid);

      if (session == null)
      {
         response.put("success",false);
         response.put("message",Messages.get("NO_SUCH_SESSION",sessid));
      }

      return(session);
   }


   public static Cursor getCursor(JSONObject response, Session session, String cursid) throws Exception
   {
      Cursor cursor = session.getCursor(cursid);

      if (cursor == null)
      {
         response.put("success",false);
         response.put("message",Messages.get("NO_SUCH_CURSOR",cursid));
      }

      return(cursor);
   }


   @SuppressWarnings("unchecked")
   public static <T extends Source> T getSource(JSONObject response, String srcid) throws Exception
   {
      Source source = Sources.get(srcid);

      if (source == null)
      {
         response.put("success",false);
         response.put("message",Messages.get("UNKNOWN_SOURCE",srcid));
      }

      return((T) source);
   }
}