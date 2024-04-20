package utils;

public class Path
{
   public static void main(String[] args) {
      System.out.println(get("https://localhost/","/admin/","/stop/"));
   }


   public static String get(String ...parts)
   {
      String path = "";

      for (int i = 0; i < parts.length; i++)
      {
         if (parts[i].startsWith("/") && i > 0)
            parts[i] = parts[i].substring(1);

         if (parts[i].endsWith("/"))
            parts[i] = parts[i].substring(0,parts[i].length()-2);

         path += parts[i];
      }
      return(path);
   }
}
