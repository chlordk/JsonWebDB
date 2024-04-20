package utils;

public class Misc
{
   public static String url(String ...parts)
   {
      String path = "";
      boolean url = parts[0].indexOf("://") > 0;

      for (int i = 0; i < parts.length; i++)
      {
         if (!url || i > 0)
         {
            if (parts[i].startsWith("/"))
               parts[i] = parts[i].substring(1);
         }

         if (parts[i].endsWith("/"))
            parts[i] = parts[i].substring(0,parts[i].length()-1);

         if (url && i == 0) path = parts[i];
         else path += "/" + parts[i];
      }
      return(path);
   }
}
