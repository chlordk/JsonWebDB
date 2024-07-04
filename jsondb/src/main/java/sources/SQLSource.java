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

import utils.Misc;
import database.Parser;
import database.SQLPart;
import database.DataType;
import java.util.HashMap;
import database.BindValue;
import org.json.JSONObject;
import static utils.Misc.*;


public class SQLSource implements Source
{
   private static final String ID = "id";
   private static final String SQL = "sql";
   private static final String UPD = "update";
   private static final String CURSOR = "cursor";

   private final String sid;
   private final SQLPart sql;
   private final Boolean upd;
   private final Boolean cur;


   public SQLSource(JSONObject definition) throws Exception
   {
      Boolean usecurs = Misc.get(definition,CURSOR);
      if (usecurs == null) usecurs = true;

      this.cur = usecurs;
      this.upd = get(definition,UPD,false);
      this.sid = getString(definition,ID,true,true);
      this.sql = Parser.parse(getString(definition,SQL,true,false));

      HashMap<String,DataType> types = DataType.getDataTypes(definition);

      for(BindValue bv : this.sql.bindValues())
      {
         DataType dt = types.get(bv.name().toLowerCase());
         if (dt != null) bv.type(dt.sqlid);
      }
   }

   public String id()
   {
      return(sid);
   }

   public Boolean update()
   {
      return(upd);
   }

   public boolean cursor()
   {
      return(cur);
   }

   public SQLPart sql()
   {
      return(sql.clone());
   }
}