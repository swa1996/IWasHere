package ru.isu.swa.diplom.controller;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.isu.swa.diplom.model.Group;
import ru.isu.swa.diplom.model.User;
import ru.isu.swa.diplom.view.AuthActivity;

import static ru.isu.swa.diplom.controller.ServiceGenerator.BASE_URL;

/**
 * Created by swa on 18.02.2018.
 */
//класс статичных функций. Нужен, чтобы реализовать наиболее повторяющиеся методы
public class Function {

    private static IWHApi apiService = ServiceGenerator.createService(IWHApi.class, BASE_URL);

    //Массив байт в шестнадцатиричную строку
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        Formatter formatter = new Formatter(sb);
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return sb.toString();
    }
    //Проверка полей на заполнение
    public static Set<Integer> validate(TextView... params){
        Set<Integer> result = new HashSet();
        for(TextView param: params){
            if(param.getText().toString().isEmpty()) result.add(param.getId());
        }
        return result;
    }
    //Шестнадцатиричную строку в массив байт
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    //округление значений широты/долготы до 4-х символов после запятой
    public static Double round(Double num){
        return Math.floor(num*10000)/10000;
    }

    //Получение объекта класса User
    public static User getUser(String cookie, Integer id, Integer subId){
            Call<User> call = apiService.getUserInfo(cookie, id,subId);
        try {
            Response<User> res = call.execute();
           if(res.code()==200) return res.body();
        } catch (IOException e) {
            e.printStackTrace();
        }
    return null;
    }
    //получение объекта класса Group
    public static Group getGroup(String cookie, Integer id, Integer subId){
        Call<Group> call = apiService.getGroup(cookie, id, subId);
        //Обрабатываем ответ от сервера на запрос
        try {
            Response<Group> res = call.execute();
            if(res.code()==200) return res.body();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Выход из приложения
public static void exit(final Context context, final SharedPreferences sharedPreferences){
    AlertDialog.Builder ad = new AlertDialog.Builder(context);
    ad.setTitle("Выход");  // заголовок
    ad.setMessage("Ваша сессия будет завершена, а все приватные данные на данном устройстве удалены\nВы действительно хотите выйти?"); // сообщение
    ad.setPositiveButton("Выйти", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int arg1) {
            Call<Void> call = apiService.exit(sharedPreferences.getString("USER_COOKIE",""), sharedPreferences.getInt("USER_ID",0), sharedPreferences.getInt("USER_PUBLICKEY_ID",0));
            //Обрабатываем ответ от сервера на запрос
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    //код ответа сервера (200 - ОК), в данном случае далее не используется
                    int statusCode = response.code();
                    switch (statusCode) {
                        case 200:
                            Toast.makeText(context, "Выход успешно завершен", Toast.LENGTH_LONG).show();
                        case 401:
                            Intent intent1 = new Intent(context, AuthActivity.class);
                            intent1.putExtra("email", sharedPreferences.getString("USER_EMAIL",""));
                            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent1);
                        case 404:
                            Toast.makeText(context, "Пользователь не найден", Toast.LENGTH_LONG).show();
                            break;
                        case 400:
                            Toast.makeText(context, "Неверный запрос", Toast.LENGTH_LONG).show();
                            break;
                        case 500:
                            Toast.makeText(context, "Ошибка сервера", Toast.LENGTH_LONG).show();
                            break;
                        default:
                            Toast.makeText(context, "Неизвестная ошибка", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    String mes = t.getMessage();
                }
            });
        }
    });
    ad.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int arg1) {
        }
    });
    AlertDialog alertDialog = ad.create();
    alertDialog.show();
}

//удаление пользовательских данных при выходе из приложении/сбросе сессии
    public static void cleanCookie(SharedPreferences settings){
        SharedPreferences.Editor edit = settings.edit();
        edit.remove("USER");
        edit.apply();
    }

    //преобразование даты из time()-формата PHP в читаемый
    public static String getDate(String s){
        Date date = new Date();
        date.setTime(Long.parseLong(s)*1000);
        SimpleDateFormat df = new SimpleDateFormat();
        df.applyPattern("dd MMMM yyyy HH:mm");

        return df.format(date);
    }

    public static String[] getNames(List<? extends Object> objects){
        String[] result = new String[objects.size()];
        for(int i=0;i<result.length;i++){
            result[i] = objects.get(i).toString();
        }
        return result;
    }
}
