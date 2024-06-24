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

import database.Parser;
import database.SQLPart;
import java.util.HashMap;
import database.Variable;
import messages.Messages;
import database.BindValue;
import static utils.Misc.*;
import org.json.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Function implements Source
{
   private static final String ID = "id";
   private static final String UPD = "update";
   private static final String FUNC = "execute";
   private static final Pattern pattern = Pattern.compile("[:]?\\w*\\s*=");

   private final String id;
   private final SQLPart sql;
   private final boolean upd;
   private final boolean func;


   public Function(JSONObject definition) throws Exception
   {
      boolean func = false;
      Variable type = null;

      this.id = getString(definition,ID,true,true);

      String call = getString(definition,FUNC,false,false);
      HashMap<String,Variable> vars = Variable.getVariables(definition);

      call = call.trim();

      Matcher matcher = pattern.matcher(call);
      if (matcher.find() && matcher.start() == 0)
      {
         func = true;

         if (!call.startsWith(":"))
            call = ":" + call;
      }

      this.func = func;

      if (func)
      {
         int pos = call.indexOf("=");
         if (pos <= 0) throw new Exception(Messages.get("INVALID_SYNTAX",call));
         call = "{" +  call.substring(0,pos) + " = call " + call.substring(pos+1) + "}";
      }
      else call = "{ call " + call + "}";

      this.sql = Parser.parse(call);

      if (func && sql.bindValues().size() > 0)
         sql.bindValues().get(0).ampersand(false).out(true);

      Boolean upd = get(definition,UPD,false);
      if (upd == null) upd = false;
      this.upd = upd;

      for(BindValue bv : this.sql.bindValues())
      {
         type = vars.get(bv.name().toLowerCase());

         if (type != null)
         {
            bv.out(type.out);
            bv.type(type.sqlid);
         }
      }
   }

   public String id()
   {
      return(id);
   }

   public boolean returns()
   {
      return(func);
   }

   public SQLPart sql()
   {
      return(sql.clone());
   }

   public boolean update()
   {
      return(upd);
   }
}
