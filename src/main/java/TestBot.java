import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.Console;
import java.sql.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

public class TestBot extends TelegramLongPollingBot {

    //флаг для подписки
    boolean flag = false;
    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                if (message.hasEntities()){
                    Optional<MessageEntity> commandEntity = message.getEntities()
                            .stream().filter(e -> "bot_command".equals(e.getType()))
                            .findFirst();
                    if (commandEntity.isPresent()) {
                        String command = message.getText().
                                substring(commandEntity.get().getOffset(), commandEntity.get().getLength());
                        switch (command) {
                            case "/subscribe":
                                flag = true;
                                execute(SendMessage.builder().chatId(message.getChatId().toString()).
                                        text("Укажите время оповещения, интервал и город в формате часы:минуты:часы:минуты:город").build());
                        }
                    }
                }
                else {
                    if (flag){
                        String[] time = message.getText().split(":");
                        String regex = "\\d{2}";
                        Pattern pattern = Pattern.compile(regex);
                        for (int i = 0; i < 4; i++) {
                            if(!pattern.matcher(time[i]).matches()){
                                execute(SendMessage.builder().chatId(message.getChatId().toString()).
                                        text("Неверный ввод").build());
                                return;
                            }
                        }
                        //запись в бд
                        Class.forName("org.sqlite.JDBC");
                        Connection connection = DriverManager.getConnection("jdbc:sqlite:C:\\Users\\DaniilH\\Downloads\\sqlite-dll-win64-x64-3390000\\users.db");
                        String q = "INSERT INTO users (chat, city, time, interval) " +
                                "VALUES ('" + message.getChatId().toString() + "', '" + time[4] + "' ,'" + time[0] + ":" + time[1]+ "', '" + time[2] + ":" + time[3]+ "') ";
                        Statement statement = connection.createStatement();
                        statement.executeUpdate(q);
                        execute(SendMessage.builder().chatId(message.getChatId().toString()).
                                text("Вы успешно подписались на рассылку.").build());
                        flag = false;
                    }
                    else {
                        String host = "https://api.openweathermap.org/data/2.5/weather?q="+message.getText()+"&lang=ru&units=metric&appid=11c7735a92bc76c1ecaa6676b2111b18";
                        URL url = new URL(host);
                        URLConnection urlConnection = url.openConnection();
                        InputStreamReader inputStreamReader = new InputStreamReader(urlConnection.getInputStream());
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        JSONObject json = new JSONObject(bufferedReader.readLine());
                        Double first = json.getJSONObject("main").getDouble("temp");
                        String sky = json.getJSONArray("weather").getJSONObject(0).getString("description");
                        execute(SendMessage.builder().chatId(message.getChatId().toString()).text("Температура: "+ first+"\n"+"\n"+sky).build());                    }
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "@MMTR_Weather_bot";
    }

    @Override
    public String getBotToken() {
        return "5483300634:AAEKXMCIK-z-d-d1m0hUpAlcZ5azuMauPMg";
    }



    public static void main(String[] args) {
        try{
            TestBot bot = new TestBot();
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(bot);
            Thread run = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        try {
                            Date date = new Date();
                            int h = date.getHours();
                            int m = date.getMinutes();
                            Class.forName("org.sqlite.JDBC");
                            Connection connection = DriverManager.getConnection("jdbc:sqlite:C:\\Users\\DaniilH\\Downloads\\sqlite-dll-win64-x64-3390000\\users.db");
                            String q = "SELECT * FROM users";
                            Statement statement = connection.createStatement();
                            ResultSet result= statement.executeQuery(q);
                            while (result.next()) {
                                String chat = result.getString("chat");
                                String[] time = result.getString("time").split(":");
                                String[] interval = result.getString("interval").split(":");
                                int timeSub = Integer.parseInt(time[0])*60 + Integer.parseInt(time[1]);
                                int interSub = Integer.parseInt(interval[0])*60 + Integer.parseInt(interval[1]);
                                if (h*60+m == timeSub || (h*60+m - timeSub)%interSub==0 ){
                                    String host = "https://api.openweathermap.org/data/2.5/weather?q="+ result.getString("city")+"&lang=ru&units=metric&appid=11c7735a92bc76c1ecaa6676b2111b18";
                                    URL url = new URL(host);
                                    URLConnection urlConnection = url.openConnection();
                                    InputStreamReader inputStreamReader = new InputStreamReader(urlConnection.getInputStream());
                                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                    JSONObject json = new JSONObject(bufferedReader.readLine());
                                    Double first = json.getJSONObject("main").getDouble("temp");
                                    String sky = json.getJSONArray("weather").getJSONObject(0).getString("description");
                                    bot.execute(SendMessage.builder().chatId(chat).text("Температура: "+ first+"\n"+"\n"+sky).build());
                                }

                            }
                            /////
                            Thread.sleep(60000); //1000 - 1 сек
                        } catch (InterruptedException | ClassNotFoundException | SQLException | IOException ex) {
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            run.start();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }

    }
}
