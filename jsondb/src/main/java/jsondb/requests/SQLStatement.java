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
import jsondb.Response;
import database.Cursor;
import database.SQLPart;
import utils.JSONOObject;
import messages.Messages;
import java.util.HashMap;
import sources.SQLSource;
import database.BindValue;
import org.json.JSONArray;
import java.util.ArrayList;
import org.json.JSONObject;


public class SQLStatement
{
   private final String sessid;
   private final String source;
   private final JSONObject definition;

   private static final String SOURCE = "source";
   private static final String INSERT = "insert";
   private static final String UPDATE = "update";
   private static final String DELETE = "delete";
   private static final String SELECT = "select";
   private static final String EXECUTE = "execute";
   private static final String SESSION = "session";
   private static final String PAGESIZE = "page-size";
   private static final String SAVEPOINT = "savepoint";


   public SQLStatement(JSONObject definition) throws Exception
   {
      this.definition = definition;
      source = definition.getString(SOURCE);
      sessid = definition.optString(SESSION);
   }


   public Response insert() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid,INSERT);
      if (session == null) return(new Response(response));

      try
      {
         Forward fw = Forward.redirect(session,"Sql",definition);
         if (fw != null) return(new Response(fw.response()));
         return(insert(session));
      }
      finally
      {
         session.down();
      }
   }


   public Response insert(Session session) throws Exception
   {
      JSONObject response = new JSONOObject();

      SQLSource source = Utils.getSource(response,this.source);
      if (source == null) return(new Response(response));

      return(exeupdate(INSERT,session,source));
   }


   public Response update() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid,UPDATE);
      if (session == null) return(new Response(response));

      try
      {
         Forward fw = Forward.redirect(session,"Sql",definition);
         if (fw != null) return(new Response(fw.response()));
         return(update(session));
      }
      finally
         {
         session.down();
      }
   }


   public Response update(Session session) throws Exception
   {
      JSONObject response = new JSONOObject();

      SQLSource source = Utils.getSource(response,this.source);
      if (source == null) return(new Response(response));

      return(exeupdate(UPDATE,session,source));
   }


   public Response delete() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid,DELETE);
      if (session == null) return(new Response(response));

      try
      {
         Forward fw = Forward.redirect(session,"Sql",definition);
         if (fw != null) return(new Response(fw.response()));
         return(delete(session));
      }
      finally
      {
         session.down();
      }
   }


   public Response delete(Session session) throws Exception
   {
      JSONObject response = new JSONOObject();

      SQLSource source = Utils.getSource(response,this.source);
      if (source == null) return(new Response(response));

      return(exeupdate(DELETE,session,source));
   }


   public Response select() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid,SELECT);
      if (session == null) return(new Response(response));

      try
      {
         Forward fw = Forward.redirect(session,"Sql",definition);
         if (fw != null) return(new Response(fw.response()));
         return(select(session));
      }
      finally
      {
         session.down();
      }
   }


   public Response select(Session session) throws Exception
   {
      JSONObject response = new JSONOObject();

      SQLSource source = Utils.getSource(response,this.source);
      if (source == null) return(new Response(response));

      return(select(session,source));
   }


   public Response select(Session session, SQLSource source) throws Exception
   {
      BindValue used = null;
      JSONObject response = new JSONOObject();
      HashMap<String,BindValue> values = Utils.getBindValues(definition);

      JSONObject args = null;
      
      if (definition.has(SELECT+"()"))
         args = Utils.getMethod(definition,SELECT);

      SQLPart select = source.sql();
      boolean usecurs = source.cursor();

      if (select.bindValues().size() > 0 && values == null)
         throw new Exception(Messages.get("MISSING_BINDVALUES"));

      for(BindValue def : select.bindValues())
      {
         used = values.get(def.name().toLowerCase());
         if (used != null) def.value(used.value());
      }

      select.bindByValue();

      boolean savepoint = Config.dbconfig().savepoint(false);
      if (definition.has(SAVEPOINT)) savepoint = definition.getBoolean(SAVEPOINT);

      Integer pagesize = null;
      if (args != null) pagesize = Misc.get(args,PAGESIZE); if (pagesize == null) pagesize = 0;

      Cursor cursor = session.executeQuery(select.snippet(),select.bindValues(),savepoint,pagesize);

      JSONArray rows = new JSONArray();
      ArrayList<Object[]> table = cursor.fetch();

      if (!usecurs) cursor.close();

      else if (Config.conTimeout() <= 0 && !session.isStateful())
         cursor.remove();

      response.put("success",true);
      response.put("session",sessid);
      response.put("method","select()");

      if (cursor.next())
      {
         response.put("more",true);
         response.put("cursor",cursor.guid());
      }

      if (cursor.primary())
         response.put("primary",true);

      response.put("rows",rows);
      for(Object[] row : table) rows.put(row);

      return(new Response(response));
   }


   public Response exeupdate(String method, Session session, SQLSource source) throws Exception
   {
      BindValue used = null;
      JSONObject response = new JSONOObject();
      ArrayList<BindValue> bindvalues = new ArrayList<BindValue>();
      HashMap<String,BindValue> values = Utils.getBindValues(definition);

      SQLPart sql = source.sql();

      if (sql.bindValues().size() > 0 && values == null)
         throw new Exception(Messages.get("MISSING_BINDVALUES"));

      for(BindValue def : sql.bindValues())
      {
         // Only use the ones actually used
         used = values.get(def.name().toLowerCase());
         if (used != null) bindvalues.add(def.value(used.value()));
      }

      SQLPart update = new SQLPart(sql.snippet(),bindvalues).bindByValue();

      boolean savepoint = Config.dbconfig().savepoint(true);
      if (definition.has(SAVEPOINT)) savepoint = definition.getBoolean(SAVEPOINT);

      response = session.executeUpdate(update.snippet(),update.bindValues(),null,savepoint);

      response.put("success",true);
      response.put("session",sessid);
      response.put("method",method+"()");

      return(new Response(response));
   }


   public Response execute() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid,EXECUTE);
      if (session == null) return(new Response(response));

      try
      {
         Forward fw = Forward.redirect(session,"Sql",definition);
         if (fw != null) return(new Response(fw.response()));
         return(execute(session));
      }
      finally
      {
         session.down();
      }
   }


   public Response execute(Session session) throws Exception
   {
      JSONObject response = new JSONOObject();

      SQLSource source = Utils.getSource(response,this.source);
      if (source == null) return(new Response(response));

      return(execute(session,source));
   }


   public Response execute(Session session, SQLSource source) throws Exception
   {
      BindValue used = null;
      SQLPart sql = source.sql();
      JSONObject response = new JSONOObject();
      JSONObject args = Utils.getMethod(definition,EXECUTE);
      HashMap<String,BindValue> values = Utils.getBindValues(definition);

      if (sql.bindValues().size() > 0 && values == null)
         throw new Exception(Messages.get("MISSING_BINDVALUES"));

      for(BindValue def : sql.bindValues())
      {
         used = values.get(def.name().toLowerCase());
         if (used != null) def.value(used.value());
      }

      sql.bindByValue();

      Boolean update = source.update();
      if (update == null) update = false;

      boolean savepoint = Config.dbconfig().savepoint(true);
      if (args.has(SAVEPOINT)) savepoint = args.getBoolean(SAVEPOINT);

      boolean success = session.execute(sql.snippet(),update,savepoint);

      response.put("success",success);
      response.put("session",sessid);
      response.put("method","execute()");

      return(new Response(response));
   }
}