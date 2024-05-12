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
import java.util.HashMap;
import messages.Messages;
import database.BindValue;
import org.json.JSONArray;
import sources.TableSource;
import java.util.ArrayList;
import org.json.JSONObject;
import filters.WhereClause;
import sources.TableSource.AccessType;


public class Table
{
   private final String sessid;
   private final String source;
   private final JSONObject definition;

   private static final String ORDER = "order";
   private static final String SOURCE = "source";
   private static final String CURSOR = "cursor";
   private static final String SELECT = "select()";
   private static final String COLUMNS = "columns";
   private static final String SESSION = "session";
   private static final String PAGESIZE = "page-size";
   private static final String SAVEPOINT = "savepoint";
   private static final String FORUPDATE = "for-update";


   public Table(JSONObject definition) throws Exception
   {
      this.definition = definition;
      source = definition.getString(SOURCE);
      sessid = definition.getString(SESSION);
   }


   public Response describe() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid,"describe()");
      if (session == null) return(new Response(response));

      return(describe(session));
   }


   public Response describe(Session session) throws Exception
   {
      JSONObject response = new JSONOObject();

      TableSource source = Utils.getSource(response,this.source);
      if (source == null) return(new Response(response));

      return(describe(session,source));
   }


   public Response describe(Session session, TableSource source) throws Exception
   {
      JSONObject response = new JSONOObject();

      response.put("success",true);

      if (!source.described())
      {
         // We only need to describe it once
         synchronized(source)
         {
            if (!source.described())
            {
               HashMap<String,BindValue> bindvalues =
                  Utils.getBindValues(definition);

               String stmt = "select *";
               SQLPart select = new SQLPart(stmt);
               select.append(source.from(bindvalues));
               select.snippet(select.snippet()+" where 1 = 2");

               Cursor cursor = session.executeQuery(select.snippet(),select.bindValues(),false,0);
               source.setColumns(cursor.describe());
               cursor.close();
            }
         }
      }

      JSONArray rows = new JSONArray();
      ArrayList<Column> columns = source.getColumns();

      if (source.order != null)
         response.put("order",source.order);

      if (source.primarykey != null)
         response.put("primary-key",source.primarykey);

      for (int i = 0; i < columns.size(); i++)
         rows.put(columns.get(i).toJSONObject());

      response.put("rows",rows);
      return(new Response(response));
   }


   public Response select() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid,"select()");
      if (session == null) return(new Response(response));

      Forward fw = Forward.redirect(session,definition);
      if (fw != null) return(new Response(fw.response()));

      return(select(session));

   }


   public Response select(Session session) throws Exception
   {
      JSONObject response = new JSONOObject();

      TableSource source = Utils.getSource(response,this.source);
      if (source == null) return(new Response(response));

      Forward fw = Forward.redirect(session,definition);
      if (fw != null) return(new Response(fw.response()));

      return(select(session,source));
   }


   public Response select(Session session, TableSource source) throws Exception
   {
      JSONObject response = new JSONOObject();

      AccessType limit = source.getAccessLimit("select");
      if (limit == AccessType.denied) throw new Exception(Messages.get("ACCESS_DENIED"));

      if (!source.described())
         this.describe(session,source);

      HashMap<String,BindValue> bindvalues =
         Utils.getBindValues(definition);

      JSONObject args = definition.getJSONObject(SELECT);

      Boolean usecurs = Misc.get(args,CURSOR);
      if (usecurs == null) usecurs = true;

      Boolean lock = Misc.get(args,FORUPDATE);
      if (lock == null) lock = false;

      String order = Misc.getString(args,ORDER,false);

      String[] columns = Misc.getJSONList(args,COLUMNS,String.class);
      String stmt = "select "+Utils.getColumnList(columns);

      SQLPart select = new SQLPart(stmt);
      select.append(source.from(bindvalues));

      WhereClause whcl = new WhereClause(source,args);

      if (limit == AccessType.ifwhereclause && !whcl.exists())
         throw new Exception(Messages.get("NO_WHERE_CLAUSE"));

      if (limit == AccessType.byprimarykey && !whcl.usesPrimaryKey(source.primarykey))
         throw new Exception(Messages.get("WHERE_PRIMARY_KEY",Messages.flatten(source.primarykey)));

      select.append(whcl.asSQL());

      if (source.vpd != null && source.vpd.appliesTo("select"))
      {
         SQLPart vpdflt = source.vpd.bind(bindvalues);
         if (whcl.exists()) select.append("\nand",vpdflt);
         else select.append("\nwhere",vpdflt);
      }

      if (order != null) select.append("\norder by "+order);
      if (lock && session.isStateful()) select.append("\nfor update");

      boolean savepoint = Config.pool().savepoint(false);
      if (args.has(SAVEPOINT)) savepoint = args.getBoolean(SAVEPOINT);

      Integer pagesize = Misc.get(args,PAGESIZE); if (pagesize == null) pagesize = 0;
      Cursor cursor = session.executeQuery(select.snippet(),select.bindValues(),savepoint,pagesize);

      JSONArray rows = new JSONArray();
      ArrayList<Object[]> table = cursor.fetch();

      if (!usecurs) cursor.close();
      response.put("success",true);

      if (cursor.next())
      {
         response.put("more",true);
         response.put("cursor",cursor.guid());
      }

      response.put("rows",rows);
      for(Object[] row : table) rows.put(row);

      return(new Response(response));
   }
}