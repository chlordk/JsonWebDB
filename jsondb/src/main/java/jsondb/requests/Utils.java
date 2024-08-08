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
import jsondb.Session.TransactionLost;


public class Utils
{
   private static final String NAME = "name";
   private static final String VALUE = "value";
   private static final String SECTION = "bindvalues";


   public static JSONObject getMethod(JSONObject def, String method) throws Exception
   {
      method = method.trim().toLowerCase();

      if (def.has(method))
         return(def.getJSONObject(method));

      if (def.has(method+"()"))
         return(def.getJSONObject(method+"()"));

      throw new Exception(Messages.get("MANDATORY_SECTION_MISSING",method));
   }


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
         Object value = bdef.get(VALUE);

         if (value == JSONObject.NULL)
            value = null;

         bindvalues.put(name.toLowerCase(),new BindValue(name).value(value));
      }

      return(bindvalues);
   }


   public static Session getSession(JSONObject response, String sessid, String method) throws Exception
   {
      Session session = null;
      boolean losttrx = false;

      try
      {
         session = Session.get(sessid,false);
      }
      catch (Exception e)
      {
         if (e instanceof TransactionLost)
            losttrx = true;
      }

      if (session == null)
      {
         response.put("success",false);
         response.put("method",method);
         response.put("session",sessid);
         if (losttrx) response.put("TRXFatal",true);
         else response.put("message",Messages.get("NO_SUCH_SESSION",sessid));
      }

      return(session);
   }


   public static Cursor getCursor(JSONObject response, Session session, String cursid, String method) throws Exception
   {
      Cursor cursor = session.getCursor(cursid);

      if (cursor == null)
      {
         response.put("success",false);
         response.put("method",method);
         response.put("cursor",cursid);
         response.put("message",Messages.get("NO_SUCH_CURSOR",cursid));
      }
      else if (!cursor.session().guid().equals(session.guid()))
      {
         cursor = null;

         response.put("success",false);
         response.put("method",method);
         response.put("cursor",cursid);
         response.put("message",Messages.get("WRONG_CURSOR_SESSION",cursid,session.guid()));
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