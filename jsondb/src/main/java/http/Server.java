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

package http;

import java.net.URL;
import java.io.File;
import jsondb.JsonDB;
import java.io.FilenameFilter;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;


public class Server
{
   public static void main(String[] args) throws Exception
   {
      if (args.length != 1)
      {
         System.err.println("Program requires exactly one argument [instance name]");
         System.exit(-1);
      }

      String inst = args[0];
      String root = findAppHome();
      JsonDB.initialize(root,inst);

      HttpServer server = HttpServer.create(new InetSocketAddress("localhost",6001),16);
      server.setExecutor(Executors.newFixedThreadPool(16));
      server.createContext("/",new Handler());
      server.start();

      //HttpsServer secsrv = HttpsServer.create(null,0);
   }

   private static String findAppHome()
   {
      Object obj = new Object() { };

      String cname = obj.getClass().getEnclosingClass().getName();
      cname = "/" + cname.replace('.','/') + ".class";

      URL url = obj.getClass().getResource(cname);
      String path = url.getPath();

      if (url.getProtocol().equals("jar") || url.getProtocol().equals("code-source"))
      {
         path = path.substring(5); // get rid of "file:"
         path = path.substring(0,path.indexOf("!")); // get rid of "!class"
         path = path.substring(0,path.lastIndexOf("/")); // get rid of jarname
      }

      int pos = 0;
      File folder = null;
      HasConfigFolder check = new HasConfigFolder();

      while(pos >= 0)
      {
         folder = new File(path);

         if (folder.isDirectory() && folder.listFiles(check).length == 1)
            break;

         pos = path.lastIndexOf("/");
         path = path.substring(0,pos);
      }

      return(path);
   }


   private static class HasConfigFolder implements FilenameFilter
   {
      @Override
      public boolean accept(File dir, String name)
         {return(name.equals("config"));}
   }
}
