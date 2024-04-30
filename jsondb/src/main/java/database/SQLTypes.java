/*
  MIT License

  Copyright © 2023 Alex Høffner

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the “Software”), to deal in the Software without
  restriction, including without limitation the rights to use, copy, modify, merge, publish,
  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
  Software is furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies or
  substantial portions of the Software.

  THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package database;

import jsondb.Config;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import messages.Messages;
import java.util.logging.Level;
import java.lang.reflect.Field;


public class SQLTypes
{
   private static HashSet<Integer> dates = null;
   private static HashMap<Integer,String> sqlids = null;
   private static HashMap<String,Integer> sqlnames = null;
   private static HashMap<String,String> synonyms = null;

   public static void main(String[] args)
   {
      getAllJdbcTypeNames();

      System.out.println("");
      System.out.println("-------------------------------------");
      System.out.println(String.format("  %-25s [%5s]","TYPE","SQLID"));
      System.out.println("-------------------------------------");

      for (Integer id : sqlids.keySet())
      {
         String name = sqlids.get(id);
         System.out.println(String.format("  %-25s [%5d]",name,id));
      }

      System.out.println("");
      System.out.println("");
      System.out.println("  +Synonyms");
      System.out.println("");

      for(String syn : synonyms.keySet())
      {
         String alt = synonyms.get(syn);
         System.out.println(String.format("  %-10s %-10s",syn.toUpperCase(),alt.toUpperCase()));
      }

      System.out.println("");
      System.out.println("-------------------------------------");
   }

   public static void initialize()
   {
      getAllJdbcTypeNames();
   }

   public static int getType(String name)
   {
      Integer id = sqlnames.get(name.toLowerCase());
      if (id == null) id = sqlnames.get("varchar");
      return(id);
   }

   public static String getType(int id)
   {
      String name = sqlids.get(id);
      if (name == null) name = "varchar";
      return(name);
   }

   public static boolean isDateType(int id)
   {
      return(dates.contains(id));
   }

   public static boolean isDateType(String name)
   {
      Integer id = getType(name);
      return(dates.contains(id));
   }

   private static void getAllJdbcTypeNames()
   {
      dates = new HashSet<Integer>();

      dates.add(Types.TIME);
      dates.add(Types.DATE);
      dates.add(Types.TIMESTAMP);
      dates.add(Types.TIMESTAMP_WITH_TIMEZONE);

      synonyms = new HashMap<String,String>();

      synonyms.put("string","varchar");
      synonyms.put("number","numeric");
      synonyms.put("varchar2","varchar");

      try
      {
         sqlids = new HashMap<Integer,String>();
         sqlnames = new HashMap<String,Integer>();

         for (Field field : Types.class.getFields())
         {
            String name = field.getName();
            int id = (Integer) field.get(null);

            sqlids.put(id,name);
            sqlnames.put(name.toLowerCase(),id);
         }

      }
      catch (Throwable t)
      {
         Config.logger().log(Level.SEVERE,t.toString(),t);
         Config.logger().severe(Messages.get("SERVER_STOPPED"));
         System.exit(-1);
      }
  }
}
