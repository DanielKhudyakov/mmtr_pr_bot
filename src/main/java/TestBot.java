import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class TestBot extends TelegramLongPollingBot {

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                String host = "https://api.openweathermap.org/data/2.5/weather?q="+message.getText()+"&lang=ru&units=metric&appid=11c7735a92bc76c1ecaa6676b2111b18";
                URL url = new URL(host);
                URLConnection urlConnection = url.openConnection();
                InputStreamReader inputStreamReader = new InputStreamReader(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                /*JSONObject json = new JSONObject(bufferedReader.readLine());
                System.out.println(json);
                String first = json.getJSONObject("weather").getString("main");
                System.out.println(first);*/
                execute(SendMessage.builder().chatId(message.getChatId().toString()).text("JDM:"+ bufferedReader.readLine()).build());
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


    public static void main(String[] args) throws TelegramApiException {
        TestBot bot = new TestBot();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(bot);
    }
}
