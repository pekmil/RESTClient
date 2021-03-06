package hu.pemik.dcs.restclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ContextResolver;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 *
 * @author pekmil
 */
public class ApplicationMain {
    
    private Client client;
    private WebTarget todosEndpoint;
    private WebTarget todoEndpoint;
    private WebTarget todoDeleteEndpoint;

    public static void main(String[] args){
        try{
            ApplicationMain app = new ApplicationMain();
            String mode = args[0];
            switch(mode){
                case "REST":
                    app.initRestClient(args[1]);
                    app.startRepl();
                    break;
                case "WS":
                    WebSocketNotificationClient ws = new WebSocketNotificationClient();
                    ws.startWsClient();
                    break;
                default:
                    throw new IllegalArgumentException("Not supported application mode: " + mode);
            }
        }
        catch(Throwable t){
            t.printStackTrace();
        }
    }
    
    private void initRestClient(String userName){
        this.client = ClientBuilder.newBuilder()
        .register(JsonObjectMapperProvider.class)
        .register(JacksonFeature.class)
        .build();
        AuthRequestFilter authFilter = new AuthRequestFilter(userName);
        this.todosEndpoint = client.target("http://localhost:33333/rest/todos/all").register(authFilter);
        this.todoEndpoint = client.target("http://localhost:33333/rest/todos/todo").register(authFilter);
        this.todoDeleteEndpoint = client.target("http://localhost:33333/rest/todos/todo/{id}").register(authFilter);
    }
    
    private void startRepl(){ //Read|Evaluate|Print|Loop
        Scanner scanner = new Scanner(System.in);
        System.out.print("Todo menu:\n\t1. List all\n\t2. Create\n\t3. Update\n\t4. Delete\n\t5. Exit\nCommand: ");
        String input = scanner.nextLine();
        while(!input.equals("5")){
            switch(input){
                case "1":
                    listTodos();
                    break;
                case "2":
                    createTodo(scanner);
                    break;
                case "3":
                    updateTodo(scanner);
                    break;
                case "4":
                    deleteTodo(scanner);
                    break;
                default:
                    System.out.println("Unsupported operation: " + input);
            }
            System.out.print("Command: ");
            input = scanner.nextLine();
        }
    }
    
    private void listTodos(){
        List<Todo> todos = this.todosEndpoint.request().get(new GenericType<List<Todo>>(){});
        todos.stream().forEach(t -> System.out.println(t.getId() + " - " + t.getName() + " - " + t.getDescription() + " - " + t.getUserName()));
    }
    
    private void createTodo(Scanner scanner){
        System.out.print("\tName: ");
        String name = scanner.nextLine();
        System.out.print("\tDescription: ");
        String desc = scanner.nextLine();
        Todo todo = new Todo(); todo.setName(name); todo.setDescription(desc);
        Response response = this.todoEndpoint.request(MediaType.APPLICATION_JSON).post(Entity.json(todo));
        if(response.getStatus() == Status.CREATED.getStatusCode()){
            System.out.println("Todo created!");
        }
        else{
            System.out.println("Something went wrong :(");
        }
    }
    
    private void updateTodo(Scanner scanner){
        System.out.print("\tId: ");
        String id = scanner.nextLine();
        System.out.print("\tName: ");
        String name = scanner.nextLine();
        System.out.print("\tDescription: ");
        String desc = scanner.nextLine();
        Todo todo = new Todo(); todo.setId(id); todo.setName(name); todo.setDescription(desc); todo.setUserName("pekmil");
        Response response = this.todoEndpoint.request(MediaType.APPLICATION_JSON).put(Entity.json(todo));
        if(response.getStatus() == Status.OK.getStatusCode()){
            System.out.println("Todo modified!");
        }
        else{
            System.out.println("Something went wrong :(");
        }
    }
    
    private void deleteTodo(Scanner scanner){
        System.out.print("\tId: ");
        String id = scanner.nextLine();
        Response response = this.todoDeleteEndpoint.resolveTemplate("id", id).request(MediaType.APPLICATION_JSON).delete();
        if(response.getStatus() == Status.OK.getStatusCode()){
            System.out.println("Todo deleted!");
        }
        else{
            System.out.println("Something went wrong :(");
        }
    }
    
    class AuthRequestFilter implements ClientRequestFilter {
    
        private final String userName;

        public AuthRequestFilter(String userName) {
            this.userName = userName;
        }

        @Override
        public void filter(ClientRequestContext crc) throws IOException {
            crc.getHeaders().put(HttpHeaders.AUTHORIZATION, Arrays.asList("token " + userName));
            crc.getHeaders().put(HttpHeaders.ACCEPT, Arrays.asList("application/json"));
        }
    }   
    
    static class JsonObjectMapperProvider implements ContextResolver<ObjectMapper> {
 
        final ObjectMapper defaultObjectMapper;

        public JsonObjectMapperProvider() {
            defaultObjectMapper = createDefaultMapper();
        }

        @Override
        public ObjectMapper getContext(Class<?> type) {
            return defaultObjectMapper;
        }    

        private ObjectMapper createDefaultMapper() {
            final ObjectMapper result = new ObjectMapper();
            result.enable(SerializationFeature.INDENT_OUTPUT);
            return result;
        }

    }   
    
}
