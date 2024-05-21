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
import database.SQLTypes;
import database.DataType;
import utils.JSONOObject;
import java.util.HashMap;
import messages.Messages;
import database.BindValue;
import org.json.JSONArray;
import sources.TableSource;
import java.util.ArrayList;
import org.json.JSONObject;
import filters.WhereClause;
import java.util.logging.Level;
import sources.TableSource.AccessType;


public class Table
{
   private final String sessid;
   private final String source;
   private final JSONObject definition;

   private static final String ORDER = "order";
   private static final String VALUE = "value";
   private static final String SOURCE = "source";
   private static final String COLUMN = "column";
   private static final String CURSOR = "cursor";
   private static final String SELECT = "select()";
   private static final String COLUMNS = "columns";
   private static final String FILTERS = "filters";
   private static final String SESSION = "session";
   private static final String PAGESIZE = "page-size";
   private static final String SAVEPOINT = "savepoint";
   private static final String FORUPDATE = "for-update";
   private static final String ASSERTIONS = "assertions";


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

      try {return(describe(session));}
      finally { session.down(); }
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
               getQRYColumns(session,source);

               if (source.queryBased() && source.hasBaseObject())
                  getBASEColumns(session,source);

               if (source.primarykey.size() == 0)
                  getPrimaryKey(session,source);
            }
         }
      }

      JSONArray rows = new JSONArray();
      ArrayList<DataType> columns = source.getColumns(true);

      if (source.order != null)
         response.put("order",source.order);

      if (source.primarykey.size() > 0)
         response.put("primary-key",source.primarykey);

      for (int i = 0; i < columns.size(); i++)
         rows.put(((Column) columns.get(i)).toJSONObject());

      response.put("rows",rows);
      return(new Response(response));
   }


   public Response select() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid,"select()");
      if (session == null) return(new Response(response));

      try
      {
         Forward fw = Forward.redirect(session,"Table",definition);
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

      TableSource source = Utils.getSource(response,this.source);
      if (source == null) return(new Response(response));

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

      Assertion assrt = Assertion.parse(source,args);
      WhereClause whcl = new WhereClause(source,source.qrycolumns,args);

      if (limit == AccessType.ifwhereclause && !whcl.exists())
         throw new Exception(Messages.get("NO_WHERE_CLAUSE"));

      if (limit == AccessType.byprimarykey && !whcl.usesPrimaryKey(source.primarykey))
         throw new Exception(Messages.get("WHERE_PRIMARY_KEY",Messages.flatten(source.primarykey)));

      SQLPart wh = whcl.asSQL();
      wh = whcl.append(assrt.whcl());

      select.append(wh);

      if (source.vpd != null && source.vpd.appliesTo("select"))
      {
         SQLPart vpdflt = source.vpd.bind(bindvalues);
         if (whcl.exists()) select.append("\nand",vpdflt);
         else select.append("\nwhere",vpdflt);
      }

      if (order != null) select.append("\norder by "+order);
      if (lock && session.isStateful()) select.append("\nfor update");

      boolean savepoint = Config.dbconfig().savepoint(false);
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


   private void getQRYColumns(Session session, TableSource source) throws Exception
   {
      HashMap<String,BindValue> bindvalues =
      Utils.getBindValues(definition);

      String stmt = "select *";
      SQLPart select = new SQLPart(stmt);
      select.append(source.from(bindvalues));
      select.snippet(select.snippet()+" where 1 = 2");

      Cursor cursor = session.executeQuery(select.snippet(),select.bindValues(),false,0);
      source.setColumns(true,cursor.describe());
      cursor.close();
   }


   private void getBASEColumns(Session session, TableSource source) throws Exception
   {
      String stmt = "select * from";
      SQLPart select = new SQLPart(stmt);
      select.append(source.object);
      select.snippet(select.snippet()+" where 1 = 2");

      Cursor cursor = session.executeQuery(select.snippet(),select.bindValues(),false,0);
      source.setColumns(false,cursor.describe());
      cursor.close();
   }


   private void getPrimaryKey(Session session, TableSource source) throws Exception
   {
      String pksrc = "PrimaryKey";
      JSONOObject response = new JSONOObject();
      TableSource pkeysrc = Utils.getSource(response,pksrc);

      if (pkeysrc != null)
      {
         Integer type = SQLTypes.guessType(source.object);

         HashMap<String,BindValue> tabbinding = new HashMap<String,BindValue>()
         {{put("table",new BindValue("table").value(source.object).type(type));}};

         String stmt = "select *";
         SQLPart select = new SQLPart(stmt);
         select.append(pkeysrc.from(tabbinding));

         Cursor cursor = session.executeQuery(select.snippet(),select.bindValues(),false,0);

         ArrayList<String> pkey = new ArrayList<String>();

         while (cursor.next())
         {
            ArrayList<Object[]> table = cursor.fetch();
            pkey.add((String) table.get(0)[0]);
         }

         cursor.close();
         source.setPrimaryKey(pkey);
      }
   }


   private static class Assertion
   {
      private final WhereClause whcl;


      static Assertion parse(TableSource source, JSONObject def) throws Exception
      {
         if (!def.has(ASSERTIONS))
            return(new Assertion(null));

         JSONObject ass = null;
         JSONArray asserts = null;

         JSONArray filters = new JSONArray();
         JSONObject filterdef = new JSONObject().put(FILTERS,filters);


         asserts = def.getJSONArray(ASSERTIONS);

         for (int i = 0; i < asserts.length(); i++)
         {
            ass = asserts.getJSONObject(i);

            Object value = ass.get(VALUE);
            String column = ass.getString(COLUMN);

            JSONObject filter = new JSONObject();

            filter.put("column",column);
            filter.put("filter","=");
            filter.put("value",value);

            filters.put(filter);
         }

         WhereClause whcl = new WhereClause(source,source.qrycolumns,filterdef);
         return(new Assertion(whcl));
      }

      Assertion(WhereClause whcl)
      {
         this.whcl = whcl;
      }

      WhereClause whcl()
      {
         return(this.whcl);
      }
   }
}