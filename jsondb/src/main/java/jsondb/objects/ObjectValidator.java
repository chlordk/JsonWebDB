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
package jsondb.objects;

import java.io.File;
import jsondb.Config;
import org.json.JSONObject;
import java.io.FileInputStream;
import com.networknt.schema.JsonSchema;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonFactory;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;


public class ObjectValidator
{
   private static final String SCHEMAS = "schemas";

   private static final ConcurrentHashMap<String,JsonSchema> schemas =
      new ConcurrentHashMap<String,JsonSchema>();

   public static void main(String[] args) throws Exception
   {
      Config.load(args[0],args[1]);
      ObjectValidator.load();

      FileInputStream in = new FileInputStream("/Users/alhof/Repository/JsonWebDB/schemas/examples/session1.json");
      byte[] content = in.readAllBytes();
      in.close();

      JSONObject ses = new JSONObject(new String(content));
      validate(ses);
   }


   private static void load() throws Exception
   {
      File loc = new File(Config.path(SCHEMAS));
      JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);

      for (File file : loc.listFiles())
      {
         if (file.isFile() && file.getName().endsWith(".json"))
         {
            String name = file.getName();
            FileInputStream in = new FileInputStream(file);
            schemas.put(name.substring(0,name.length()-5),factory.getSchema(in));
         }
      }
   }


   public static boolean validate(JSONObject object) throws Exception
   {
      String [] roots = JSONObject.getNames(object);
      JsonSchema schema = schemas.get(roots[0]);

      JsonFactory factory = new JsonFactory();
      factory.setCodec(new ObjectMapper(factory));
      JsonParser jp = factory.createParser(object.toString());
      JsonNode node = jp.readValueAsTree();

      Set<ValidationMessage> errors =  schema.validate(node);
      for(ValidationMessage msg : errors) System.out.println(msg);
      return(true);
   }
}
